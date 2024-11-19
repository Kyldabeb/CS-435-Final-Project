package org.example;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.functions;

public class PlayImpactAnalyzer {

    public static void analyze(String inputPath, String outputPath) {
        SparkSession spark = SparkSession.builder()
                .appName("Play Impact Analyzer")
                .getOrCreate();

        try {
            // Load data
            Dataset<Row> df = spark.read()
                    .option("header", "true")
                    .csv(inputPath);

            // Add "play_impact" column based on descriptions
            Dataset<Row> withImpact = df.withColumn("play_impact",
                functions.when(df.col("homedescription").rlike("STEAL"), "Good")
                    .when(df.col("visitordescription").rlike("STEAL"), "Good")
                    .when(df.col("neutraldescription").rlike("STEAL"), "Good")
                    .when(df.col("homedescription").rlike("BLOCK"), "Good")
                    .when(df.col("visitordescription").rlike("BLOCK"), "Good")
                    .when(df.col("neutraldescription").rlike("BLOCK"), "Good")
                    .when(df.col("homedescription").rlike("SHOT.*PTS"), "Good")
                    .when(df.col("visitordescription").rlike("SHOT.*PTS"), "Good")
                    .when(df.col("neutraldescription").rlike("SHOT.*PTS"), "Good")
                    .when(df.col("homedescription").rlike("MISS"), "Bad")
                    .when(df.col("visitordescription").rlike("MISS"), "Bad")
                    .when(df.col("neutraldescription").rlike("MISS"), "Bad")
                    .when(df.col("homedescription").rlike("FOUL"), "Bad")
                    .when(df.col("visitordescription").rlike("FOUL"), "Bad")
                    .when(df.col("neutraldescription").rlike("FOUL"), "Bad")
                    .when(df.col("homedescription").rlike("TURNOVER"), "Bad")
                    .when(df.col("visitordescription").rlike("TURNOVER"), "Bad")
                    .when(df.col("neutraldescription").rlike("TURNOVER"), "Bad")
                    .when(df.col("homedescription").rlike("REBOUND"), "Neutral")
                    .when(df.col("visitordescription").rlike("REBOUND"), "Neutral")
                    .when(df.col("neutraldescription").rlike("REBOUND"), "Neutral")
                    .when(df.col("homedescription").rlike("TIMEOUT"), "Neutral")
                    .when(df.col("visitordescription").rlike("TIMEOUT"), "Neutral")
                    .when(df.col("neutraldescription").rlike("TIMEOUT"), "Neutral")
                    .when(df.col("homedescription").rlike("SUB"), "Neutral")
                    .when(df.col("visitordescription").rlike("SUB"), "Neutral")
                    .when(df.col("neutraldescription").rlike("SUB"), "Neutral")
                    .otherwise("Error"));

            // Select necessary columns for output
            Dataset<Row> result = withImpact.select(
                    "game_id", "period", "pctimestring", "homedescription",
                    "visitordescription", "neutraldescription", "play_impact"
            );

            // Write results to output
            result.write()
                    .option("header", "true")
                    .csv(outputPath);

            System.out.println("Analysis completed. Results saved to: " + outputPath);

        } catch (Exception e) {
            System.err.println("Error in Play Impact Analyzer: " + e.getMessage());
        } finally {
            spark.stop();
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: PlayImpactAnalyzer <inputPath> <outputPath>");
            System.exit(1);
        }
        String inputPath = args[0];
        String outputPath = args[1];
        analyze(inputPath, outputPath);
    }
}
