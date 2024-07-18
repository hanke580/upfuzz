#!/bin/bash

show_usage() {
    echo "Usage: $0 CLIENT_NUM CONFIG_FILE"
    exit 0
}

# Check whether user supplied -h or --help . If yes, display usage
if [[ ( "$1" == "--help" ) ||  "$1" == "-h" ]]
then
    show_usage
fi

# Check the number of arguments
if [[ $# -ne 2 ]]
then
    echo "Error: Invalid number of arguments"
    show_usage
fi

CLIENT_NUM=$1
CONFIG=$2

echo "CLIENT_NUM: $CLIENT_NUM";

DOWNGRADE_SUPPORTED="N"

for i in $(seq $CLIENT_NUM)
do
  java -Dlogfile="logs/upfuzz_client_g1.log" -cp "build/classes/java/main/:dependencies/*:dependencies/:build/resources/main" org/zlab/upfuzz/fuzzingengine/Main -class client -config "$CONFIG" -flag "group2" -downgrade "$DOWNGRADE_SUPPORTED" &
done
