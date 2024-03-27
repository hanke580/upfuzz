#!/bin/bash

# Define the path to your JSON file
config_file="../$1"

# Check if the config file exists
if [ ! -f "$config_file" ]; then
    echo "Config file '$config_file' not found."
    exit 1
fi

# Use sed to extract the version values
original_version=$(sed -n 's/.*"originalVersion" *: *"\([^"]*\)".*/\1/p' "$config_file")
upgraded_version=$(sed -n 's/.*"upgradedVersion" *: *"\([^"]*\)".*/\1/p' "$config_file")

# Extract only the version numbers
original_version_number=$(echo "$original_version" | sed 's/apache-cassandra-\(.*\)/\1/')
upgraded_version_number=$(echo "$upgraded_version" | sed 's/apache-cassandra-\(.*\)/\1/')

res=0
curDir=$PWD
cd cass_downgrade_checker/run/
./run.sh "$upgraded_version_number" "$original_version_number"
res="$?"
cd $curDir
exit $res