package org.example;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

public class IDReduce {

    public static void run(String csvPath, String playerId, String outputPath) {
        if (csvPath == null || playerId == null || outputPath == null) {
            throw new IllegalArgumentException("CSV path, player ID, and output path are required.");
        }

        // Initialize SparkSession
        SparkSession spark = SparkSession.builder()
                .appName("IDReduce")
                .getOrCreate();

        try {
            // Read the CSV file directly into a Dataset<Row>
            Dataset<Row> df = spark.read()
                    .option("header", "true") // Assuming the CSV file has headers
                    .csv(csvPath);

            // Filter the Dataset<Row> based on the conditions
            Dataset<Row> filtered = df.filter(df.col("player1_id").equalTo(playerId)
                    .or(df.col("player2_id").equalTo(playerId))
                    .or(df.col("player3_id").equalTo(playerId)));

            // Write the filtered results to the specified output path
            filtered.write()
                    .option("header", "true") // Include headers in the output
                    .csv(outputPath);

            System.out.println("Filtered results successfully written to: " + outputPath);

        } catch (Exception e) {
            System.err.println("Error processing the Spark job: " + e.getMessage());
            e.printStackTrace();
        } finally {
            spark.stop(); // Ensure Spark session is closed
        }
    }

    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("Usage: IDReduce <csvPath> <playerId> <outputPath>");
            System.exit(1);
        }

        String csvPath = args[0];
        String playerId = args[1];
        String outputPath = args[2];

        // Run the Spark job
        run(csvPath, playerId, outputPath);
    }
}
