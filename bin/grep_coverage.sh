#!/bin/bash

# Check for the required arguments
if [ "$#" -lt 3 ]; then
    echo "Usage: $0 <start> <end> <server_prefix>"
    exit 1
fi

# Read arguments
start=$1       # First argument: starting index, inclusive
end=$2         # Second argument: ending index, inclusive
server_prefix=$3

for n in $(seq $start $end); do
    server="${server_prefix}${n}"
		ssh $server "mkdir -p ~/project/tmp && cd ~/project/tmp && grep -A 5 'run time :' ~/project/upfuzz/server.log > output"
		
done

echo "Done."
