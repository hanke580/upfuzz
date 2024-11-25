#!/bin/bash
source bin/compute_time.sh

# Usage: input: failure/failure
# Output

# Use the function with input from the command line
if [ $# -eq 0 ]; then
   echo "Usage: $0 <directory>"
   exit 1
fi
compute_triggering_time "$1"
