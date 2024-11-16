import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;

public class GUI {
    public static void main(String[] args) {
        // Create a JFrame (the main window)
        JFrame frame = new JFrame("Player and Team Input");
        frame.setSize(700, 400); // Increased height for the additional input
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

        // SQL testing input
        JLabel sqlLabel = new JLabel("Testing SQL Input:");
        sqlLabel.setBounds(50, 220, 200, 20);
        frame.add(sqlLabel);

        JTextField sqlField = new JTextField();
        sqlField.setBounds(250, 220, 300, 30);
        frame.add(sqlField);

        // SQL testing button
        JButton sqlButton = new JButton("Run SQL");
        sqlButton.setBounds(250, 270, 100, 30);
        frame.add(sqlButton);

        JLabel sqlResultLabel = new JLabel();
        sqlResultLabel.setBounds(50, 320, 500, 30);
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
                            int playerId = rs.getInt("player_id");
                            resultLabel.setText("Player ID: " + playerId);
                        } else {
                            // If no result found, try searching by name only
                            String fallbackSql = "SELECT id AS player_id FROM player WHERE full_name COLLATE NOCASE = ?";
                            try (PreparedStatement fallbackPstmt = conn.prepareStatement(fallbackSql)) {
                                fallbackPstmt.setString(1, playerName);

                                try (ResultSet fallbackRs = fallbackPstmt.executeQuery()) {
                                    if (fallbackRs.next()) {
                                        int playerId = fallbackRs.getInt("player_id");
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

        // SQL testing button action
        sqlButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String sql = sqlField.getText();

                try (Connection conn = DriverManager.getConnection(url);
                     Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(sql)) {

                    StringBuilder result = new StringBuilder();
                    int columnCount = rs.getMetaData().getColumnCount();

                    // Fetch rows
                    while (rs.next()) {
                        for (int i = 1; i <= columnCount; i++) {
                            result.append(rs.getString(i)).append(" ");
                        }
                        result.append("| ");
                    }

                    sqlResultLabel.setText(result.toString().isEmpty() ? "No results." : result.toString());

                } catch (SQLException ex) {
                    ex.printStackTrace();
                    sqlResultLabel.setText("Error: " + ex.getMessage());
                }
            }
        });

        frame.setVisible(true);
    }
}
