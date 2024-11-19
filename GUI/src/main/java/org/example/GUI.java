package org.example;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.spark.launcher.SparkLauncher;

public class GUI {
    public static int playerId = -1;

    public static void main(String[] args) {
        // Ensure the Spark job JAR path is passed as an argument
        if (args.length < 2) {
            System.err.println("Error: Spark Job JAR path or Spark home not provided.");
            System.exit(1);
        }

        String jarPath = args[0]; // Path to the Spark Job JAR
        String sparkHome = args[1]; // Path to Spark home
        String masterUrl = "spark://baghdad.cs.colostate.edu:30276";

        // Create a JFrame (the main window)
        JFrame frame = new JFrame("Player and Team Input");
        frame.setSize(700, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(null);

        // Player name input
        JLabel playerLabel = new JLabel("Enter a Player Name:");
        playerLabel.setBounds(50, 20, 200, 20);
        frame.add(playerLabel);

        JTextField playerField = new JTextField();
        playerField.setBounds(250, 20, 300, 30);
        frame.add(playerField);

        // Team name input
        JLabel teamLabel = new JLabel("Enter a Team Name:");
        teamLabel.setBounds(50, 70, 200, 20);
        frame.add(teamLabel);

        JTextField teamField = new JTextField();
        teamField.setBounds(250, 70, 300, 30);
        frame.add(teamField);

        // Submit button
        JButton submitButton = new JButton("Submit");
        submitButton.setBounds(250, 120, 100, 30);
        frame.add(submitButton);

        JLabel resultLabel = new JLabel();
        resultLabel.setBounds(50, 170, 500, 30);
        frame.add(resultLabel);

        // SQL testing button
        JButton sqlButton = new JButton("Run SQL");
        sqlButton.setBounds(250, 200, 100, 30);
        frame.add(sqlButton);

        JLabel sqlResultLabel = new JLabel();
        sqlResultLabel.setBounds(50, 250, 500, 30);
        frame.add(sqlResultLabel);

        // Database connection string
        String url = "jdbc:sqlite:nba/nba.sqlite";

        // Player and team submission button action
        submitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String playerName = playerField.getText();
                String teamName = teamField.getText();

                String sql = "SELECT p.id AS player_id " +
                        "FROM player p " +
                        "JOIN common_player_info cpi ON p.id = cpi.person_id " +
                        "JOIN team t ON cpi.team_id = t.id " +
                        "WHERE p.full_name COLLATE NOCASE = ? " +
                        "AND (t.nickname COLLATE NOCASE = ? OR t.full_name COLLATE NOCASE = ?)";

                try (Connection conn = DriverManager.getConnection(url);
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {

                    pstmt.setString(1, playerName);
                    pstmt.setString(2, teamName);
                    pstmt.setString(3, teamName);

                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            playerId = rs.getInt("player_id");
                            resultLabel.setText("Player ID: " + playerId);
                        } else {
                            String fallbackSql = "SELECT id AS player_id FROM player WHERE full_name COLLATE NOCASE = ?";
                            try (PreparedStatement fallbackPstmt = conn.prepareStatement(fallbackSql)) {
                                fallbackPstmt.setString(1, playerName);

                                try (ResultSet fallbackRs = fallbackPstmt.executeQuery()) {
                                    if (fallbackRs.next()) {
                                        playerId = fallbackRs.getInt("player_id");
                                        resultLabel.setText("Player ID (Name-Only): " + playerId);
                                    } else {
                                        resultLabel.setText("No results found for the given name and team.");
                                    }
                                }
                            }
                        }
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    resultLabel.setText("Error: " + ex.getMessage());
                }
            }
        });

        sqlButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (playerId == -1) {
                    sqlResultLabel.setText("No Player ID available. Submit valid inputs first.");
                    return;
                }

                String csvPath = "/csv/play_by_play.csv";
                String hdfsOutputPath = "/output/spark-job-results";

                try {
                    // Delete HDFS output path if it exists
                    Process deleteProcess = Runtime.getRuntime().exec(
                            new String[]{"hdfs", "dfs", "-rm", "-r", hdfsOutputPath});
                    deleteProcess.waitFor();

                    // Run Spark job
                    Process sparkJob = new SparkLauncher()
                            .setAppResource(jarPath)
                            .setMainClass("org.example.IDReduce")
                            .setMaster(masterUrl)
                            .addAppArgs(csvPath, String.valueOf(playerId), hdfsOutputPath)
                            .setSparkHome(sparkHome)
                            .launch();

                    // Monitor Spark job progress (Standard Output)
                    Thread stdoutThread = new Thread(() -> {
                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(sparkJob.getInputStream()))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                sqlResultLabel.setText(line); // Update the GUI with progress
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    });

                    // Monitor Spark job errors (Standard Error)
                    Thread stderrThread = new Thread(() -> {
                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(sparkJob.getErrorStream()))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                System.err.println(line); // Log errors to console
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    });

                    stdoutThread.start();
                    stderrThread.start();

                    // Wait for job completion
                    int exitCode = sparkJob.waitFor();
                    stdoutThread.join();
                    stderrThread.join();

                    if (exitCode == 0) {
                        // Fetch first 5 lines from the output file
                        Process fetchOutput = Runtime.getRuntime().exec(
                                new String[]{"hdfs", "dfs", "-cat", hdfsOutputPath + "/part-*"});
                        BufferedReader outputReader = new BufferedReader(
                                new InputStreamReader(fetchOutput.getInputStream()));
                        StringBuilder result = new StringBuilder("<html>");
                        for (int i = 0; i < 5; i++) {
                            String line = outputReader.readLine();
                            if (line != null) {
                                result.append(line).append("<br>");
                            } else {
                                break;
                            }
                        }
                        result.append("</html>");
                        sqlResultLabel.setText(result.toString());
                    } else {
                        sqlResultLabel.setText("Spark job failed with exit code: " + exitCode);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    sqlResultLabel.setText("Error running Spark job: " + ex.getMessage());
                }
            }
        });

        frame.setVisible(true);
    }
}
