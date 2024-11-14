#!/bin/bash
echo "Compiling all sub-projects..."

# Compile Spark Project
echo "Compiling Spark Id Project..."
cd GetID
mvn clean install
cd ..

# Compile Spark Project
echo "Compiling Spark Project..."
cd PlayerReduce
mvn clean install
cd ..

# Compile First MapReduce Project
# echo "Compiling First MapReduce Project..."
# cd mapreduce_project_1
# mvn clean install
# cd ..

# Compile Second MapReduce Project
# echo "Compiling Second MapReduce Project..."
# cd mapreduce_project_2
# mvn clean install
# cd ..

# Compile GUI Project
echo "Compiling GUI Project..."
cd GUI
mvn clean install
cd ..

echo "Compilation complete!"
