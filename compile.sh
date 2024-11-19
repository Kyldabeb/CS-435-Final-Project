#!/bin/bash
echo "Compiling all projects..."


# Compile GUI Project
echo "Compiling IDReduce Project..."
cd IDReduce

# Build GUI Project
echo "Building IDReduce Project..."
mvn install || { echo "Failed to install GUI dependencies"; exit 1; }
mvn package || { echo "Failed to package GUI"; exit 1; }

cd ..

# Compile GUI Project
echo "Compiling GUI Project..."
cd GUI

# Build GUI Project
echo "Building GUI Project..."
mvn install || { echo "Failed to install GUI dependencies"; exit 1; }
mvn package || { echo "Failed to package GUI"; exit 1; }

cd ..

echo "Compilation complete!"
