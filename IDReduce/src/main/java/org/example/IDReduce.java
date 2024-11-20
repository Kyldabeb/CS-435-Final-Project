package org.example;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import java.util.Arrays;
import java.util.List;

import static org.apache.spark.sql.functions.*;

public class IDReduce {

    // Function to create a regex pattern for keywords
    public static String createPattern(List<String> keywords) {
        return ".*\\b(" + String.join("|", keywords) + ")\\b.*";
    }

    public static void run(String csvPath, String playerId, String outputPath) {
        if (csvPath == null || playerId == null || outputPath == null) {
            throw new IllegalArgumentException("CSV path, player ID, and output path are required.");
        }

        // Define keyword lists
        List<String> goodKeywords = Arrays.asList(
                "shot", "layup", "dunk", "3pt", "free throw", "hook shot", "fadeaway",
                "finger roll", "putback", "alley oop", "tip layup", "bank shot", "assist", "ast", "pts", "points", "steal", "block"
        );

        List<String> badKeywords = Arrays.asList(
                "miss", "missed", "blocked", "turnover", "lost ball", "bad pass",
                "foul", "offensive foul", "charge foul", "traveling", "out of bounds", "double technical"
        );

        List<String> neutralKeywords = Arrays.asList(
                "rebound", "timeout", "sub", "jump ball", "halftime", "start"
        );

        // Patterns for regex matching
        String goodPattern = createPattern(goodKeywords);
        String badPattern = createPattern(badKeywords);
        String neutralPattern = createPattern(neutralKeywords);

        // Initialize SparkSession
        SparkSession spark = SparkSession.builder()
                .appName("IDReduce")
                .getOrCreate();

        try {
            // Read the CSV file into a Dataset<Row>
            Dataset<Row> df = spark.read()
                    .option("header", "true") // Assuming the CSV file has headers
                    .csv(csvPath);

            // Filter Dataset<Row> based on player ID
            Dataset<Row> filtered = df.filter(
                    col("player1_id").equalTo(playerId)
                            .or(col("player2_id").equalTo(playerId))
                            .or(col("player3_id").equalTo(playerId))
            );

            // Update play_impact column using regex patterns
            filtered = filtered.withColumn(
                    "play_impact",
                    when(
                            lower(col("homedescription")).rlike(goodPattern)
                                    .or(lower(col("visitordescription")).rlike(goodPattern))
                                    .or(lower(col("neutraldescription")).rlike(goodPattern)),
                            "Good"
                    ).when(
                            lower(col("homedescription")).rlike(badPattern)
                                    .or(lower(col("visitordescription")).rlike(badPattern))
                                    .or(lower(col("neutraldescription")).rlike(badPattern)),
                            "Bad"
                    ).when(
                            lower(col("homedescription")).rlike(neutralPattern)
                                    .or(lower(col("visitordescription")).rlike(neutralPattern))
                                    .or(lower(col("neutraldescription")).rlike(neutralPattern)),
                            "Neutral"
                    ).otherwise("Error")
            );

            // Remove rows where all descriptions are blank

            // Select only the required columns
            Dataset<Row> finalOutput = filtered.select(
                    "game_id", "period", "pctimestring", "homedescription", "visitordescription", "neutraldescription", "play_impact"
            );

            finalOutput = finalOutput.filter(
                    col("homedescription").isNotNull().and(col("homedescription").notEqual(""))
                            .or(col("visitordescription").isNotNull().and(col("visitordescription").notEqual("")))
                            .or(col("neutraldescription").isNotNull().and(col("neutraldescription").notEqual("")))
            );

            // Write the filtered results to the specified output path
            finalOutput.coalesce(1)
                    .write()
                    .option("header", "true")
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
