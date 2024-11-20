package org.example;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.PaintScale;
import org.jfree.chart.renderer.xy.XYBlockRenderer;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.chart.renderer.LookupPaintScale;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.*;

public class VisualizationWindow extends JFrame {

    public VisualizationWindow(String visualizationType, String hdfsOutputPath) {
        // Set window properties
        setTitle("Visualization: " + visualizationType);
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // Generate temporary CSV file with mock data
        File tempFile = createTempCSV();

        // Fetch and process data from the temporary CSV file
        Map<String, java.util.List<Pair<Integer, Integer>>> outputData = fetchAndParseLocalCSV(tempFile);

        // Uncomment the line below to fetch from HDFS instead of local CSV (for final integration)
        // Map<String, java.util.List<Pair<Integer, Integer>>> outputData = fetchAndParseHDFSData(hdfsOutputPath);

        if (outputData.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No data found for visualization.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Generate heat map chart
        if (outputData.containsKey(visualizationType)) {
            JFreeChart heatMap = createHeatMap(visualizationType, outputData.get(visualizationType));
            ChartPanel chartPanel = new ChartPanel(heatMap);
            add(chartPanel, BorderLayout.CENTER);
        } else {
            JOptionPane.showMessageDialog(this, "No data available for selected visualization type.", "Error", JOptionPane.ERROR_MESSAGE);
        }

        // Close button
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        add(closeButton, BorderLayout.SOUTH);
    }

    // Create temporary CSV file with mock data
    private File createTempCSV() {
        try {
            File tempFile = File.createTempFile("mock_data", ".csv");
            tempFile.deleteOnExit(); // Delete the file on JVM exit

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
                writer.write("Score Deficit,-15:25,-10:20,-5:15,0:10,5:5,10:0\n");
                writer.write("Period,1:15,2:10,3:20,4:5\n");
                writer.write("Team,1:30,2:15,3:20,4:25\n");
                writer.write("Home vs Away,1:35,2:10,3:20,4:15\n");
            }
            return tempFile;
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new RuntimeException("Failed to create temporary CSV file for testing.");
        }
    }

    // Fetch and parse local CSV file
    private Map<String, java.util.List<Pair<Integer, Integer>>> fetchAndParseLocalCSV(File csvFile) {
        Map<String, java.util.List<Pair<Integer, Integer>>> data = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", 2);
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    java.util.List<Pair<Integer, Integer>> values = parseKeyValuePairs(parts[1].trim());
                    data.put(key, values);
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return data;
    }

    // Parse key-value pairs from a line
    private java.util.List<Pair<Integer, Integer>> parseKeyValuePairs(String input) {
        java.util.List<Pair<Integer, Integer>> pairs = new ArrayList<>();
        String[] entries = input.split(",");
        for (String entry : entries) {
            entry = entry.trim().replace("(", "").replace(")", "");
            String[] parts = entry.split(":");
            if (parts.length == 2) {
                int key = Integer.parseInt(parts[0].trim());
                int value = Integer.parseInt(parts[1].trim());
                pairs.add(new Pair<>(key, value));
            }
        }
        return pairs;
    }

    // Fetch and parse data from HDFS
    private Map<String, java.util.List<Pair<Integer, Integer>>> fetchAndParseHDFSData(String hdfsOutputPath) {
        Map<String, java.util.List<Pair<Integer, Integer>>> data = new HashMap<>();

        try {
            Process fetchOutput = Runtime.getRuntime().exec(new String[]{"hdfs", "dfs", "-cat", hdfsOutputPath + "/part-*"});
            BufferedReader outputReader = new BufferedReader(new InputStreamReader(fetchOutput.getInputStream()));
            String line;
            while ((line = outputReader.readLine()) != null) {
                String[] parts = line.split(",", 2);
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    java.util.List<Pair<Integer, Integer>> values = parseKeyValuePairs(parts[1].trim());
                    data.put(key, values);
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return data;
    }

    // Create heat map visualization
    private JFreeChart createHeatMap(String title, java.util.List<Pair<Integer, Integer>> data) {
        // Prepare data for DefaultXYZDataset
        double[] xValues = new double[data.size()];
        double[] yValues = new double[data.size()];
        double[] zValues = new double[data.size()];

        for (int i = 0; i < data.size(); i++) {
            Pair<Integer, Integer> pair = data.get(i);
            xValues[i] = pair.getKey(); // X-axis (e.g., score range or period)
            yValues[i] = 0; // Single row
            zValues[i] = pair.getValue(); // Z-axis (performance score)
        }

        // Use DefaultXYZDataset to handle X, Y, Z values
        org.jfree.data.xy.DefaultXYZDataset dataset = new org.jfree.data.xy.DefaultXYZDataset();
        dataset.addSeries("Heat Map Data", new double[][]{xValues, yValues, zValues});

        // Create renderer and configure paint scale
        XYBlockRenderer renderer = new XYBlockRenderer();
        LookupPaintScale scale = new LookupPaintScale(0.0, 40.0, Color.BLACK);
        scale.add(10, Color.BLUE);
        scale.add(20, Color.GREEN);
        scale.add(30, Color.YELLOW);
        scale.add(40, Color.RED);
        renderer.setPaintScale(scale);
        renderer.setBlockHeight(1.0);
        renderer.setBlockWidth(5.0);

        // Configure axes
        NumberAxis xAxis = new NumberAxis("Game Context");
        NumberAxis yAxis = new NumberAxis("Performance (Z-Scores)");
        xAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        yAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        // Create plot and chart
        XYPlot plot = new XYPlot(dataset, xAxis, yAxis, renderer);
        plot.setBackgroundPaint(Color.WHITE);

        return new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, false);
    }




    // Pair class to hold key-value pairs
    public static class Pair<K, V> {
        private final K key;
        private final V value;

        public Pair(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }
    }
}
