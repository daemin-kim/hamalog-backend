#!/bin/bash

echo "=== Comparing Source Files with Test Files ==="
echo ""

# Get all source files except DTOs, domain entities, enums, and application main
source_files=$(find src/main/java -name "*.java" | grep -v "/dto/" | grep -v "/domain/" | grep -v "Application.java" | sort)

echo "Checking for missing tests:"
echo ""

missing_tests=()

for source_file in $source_files; do
    # Extract class name and path
    relative_path=${source_file#src/main/java/}
    class_name=$(basename "$source_file" .java)
    package_path=$(dirname "$relative_path")
    
    # Expected test file path
    test_file="src/test/java/${package_path}/${class_name}Test.java"
    
    # Check if test file exists
    if [ ! -f "$test_file" ]; then
        echo "MISSING: $test_file (for $source_file)"
        missing_tests+=("$source_file")
    fi
done

echo ""
echo "=== SUMMARY ==="
echo "Total missing tests: ${#missing_tests[@]}"

if [ ${#missing_tests[@]} -eq 0 ]; then
    echo "All source files have corresponding tests!"
else
    echo ""
    echo "Files without tests:"
    for missing in "${missing_tests[@]}"; do
        echo "  - $missing"
    done
fi