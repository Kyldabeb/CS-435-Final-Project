#!/bin/bash

echo "Starting the GUI application..."

# Dynamically locate the GUI JAR
GUI_JAR=$(realpath GUI/target/HeatMap-1.0-SNAPSHOT.jar)
SPARK_JOB_JAR=$(realpath IDReduce/target/IDReduce-1.0-SNAPSHOT.jar)
SPARK_HOME=$(realpath $(dirname $(which spark-submit))/..)
HDFS_OUTPUT_PATH="/output/spark-job-output"

# Verify GUI JAR exists
if [ ! -f "$GUI_JAR" ]; then
    echo "Error: GUI JAR not found at $GUI_JAR"
    exit 1
fi

# Verify Spark job JAR exists
if [ ! -f "$SPARK_JOB_JAR" ]; then
    echo "Error: Spark Job JAR not found at $SPARK_JOB_JAR"
    exit 1
fi

# Delete existing HDFS output if it exists
echo "Checking HDFS output path..."
hadoop fs -rm -r "$HDFS_OUTPUT_PATH" 2>/dev/null
hadoop fs -rm -r "/output/heatmap_data" > /dev/null 2>&1
echo "HDFS output path cleared."

# Run the GUI application and pass required arguments
java -jar "$GUI_JAR" "$SPARK_JOB_JAR" "$SPARK_HOME" "$HDFS_OUTPUT_PATH"
