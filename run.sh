#!/bin/bash

echo "Starting the GUI application..."

# Get the absolute path to the correct JAR file
val=$(realpath GUI/target/gui_project-1.0-SNAPSHOT.jar)

# Print the JAR path for confirmation
echo "Using JAR file: $val"

# Run the GUI application using the absolute path
java -jar "$val"

echo "GUI application started."
