#!/bin/bash

# Check if an argument is provided
if [ "$#" -ne 1 ]; then
    echo "Usage: $0 <peer ID>"
    exit 1
fi

# Compile Java code
javac peerProcess.java

# Run the Java program with the provided argument
java peerProcess "$1"

# Remove files after execution and hide errors
rm *.class 2>/dev/null
rm ../*.lck 2>/dev/null