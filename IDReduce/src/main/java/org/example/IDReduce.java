package org.example;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.functions;

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

            // Add a new column "play_impact" based on descriptions
            Dataset<Row> withImpact = filtered.withColumn("play_impact",
                functions.when(filtered.col("homedescription").rlike("STEAL"), "Good")
                    .when(filtered.col("visitordescription").rlike("STEAL"), "Good")
                    .when(filtered.col("neutraldescription").rlike("STEAL"), "Good")
                    .when(filtered.col("homedescription").rlike("BLOCK"), "Good")
                    .when(filtered.col("visitordescription").rlike("BLOCK"), "Good")
                    .when(filtered.col("neutraldescription").rlike("BLOCK"), "Good")
                    .when(filtered.col("homedescription").rlike("SHOT.*PTS"), "Good")
                    .when(filtered.col("visitordescription").rlike("SHOT.*PTS"), "Good")
                    .when(filtered.col("neutraldescription").rlike("SHOT.*PTS"), "Good")
                    .when(filtered.col("homedescription").rlike("MISS"), "Bad")
                    .when(filtered.col("visitordescription").rlike("MISS"), "Bad")
                    .when(filtered.col("neutraldescription").rlike("MISS"), "Bad")
                    .when(filtered.col("homedescription").rlike("FOUL"), "Bad")
                    .when(filtered.col("visitordescription").rlike("FOUL"), "Bad")
                    .when(filtered.col("neutraldescription").rlike("FOUL"), "Bad")
                    .when(filtered.col("homedescription").rlike("TURNOVER"), "Bad")
                    .when(filtered.col("visitordescription").rlike("TURNOVER"), "Bad")
                    .when(filtered.col("neutraldescription").rlike("TURNOVER"), "Bad")
                    .when(filtered.col("homedescription").rlike("REBOUND"), "Neutral")
                    .when(filtered.col("visitordescription").rlike("REBOUND"), "Neutral")
                    .when(filtered.col("neutraldescription").rlike("REBOUND"), "Neutral")
                    .when(filtered.col("homedescription").rlike("TIMEOUT"), "Neutral")
                    .when(filtered.col("visitordescription").rlike("TIMEOUT"), "Neutral")
                    .when(filtered.col("neutraldescription").rlike("TIMEOUT"), "Neutral")
                    .when(filtered.col("homedescription").rlike("SUB"), "Neutral")
                    .when(filtered.col("visitordescription").rlike("SUB"), "Neutral")
                    .when(filtered.col("neutraldescription").rlike("SUB"), "Neutral")
                    .otherwise("Neutral"));

            // Select necessary columns for output
            Dataset<Row> result = withImpact.select(
                    "game_id", "period", "pctimestring", "homedescription",
                    "visitordescription", "neutraldescription", "play_impact"
            );

            // Write the filtered results with play impact to the specified output path
            result.write()
                    .option("header", "true") // Include headers in the output
                    .csv(outputPath);

            System.out.println("Filtered results with play impact successfully written to: " + outputPath);

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
