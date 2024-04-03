#!/bin/bash

# check whether user had supplied -h or --help . If yes display usage
if [[ ( $@ == "--help") ||  $@ == "-h" ]]
then
        echo "Usage: $0 CLIENT_NUM_G1"
        echo "Usage: $1 CLIENT_NUM_G2"
        echo "Usage: $2 config file"
        exit 0
fi

CLIENT_NUM_G1=$1
CLIENT_NUM_G2=$2

if [[ -z $3 ]];
then
        echo "using config.json"
        CONFIG="config.json"
else
        echo "using $3"
        CONFIG=$3
fi

if [[ -z "${CLIENT_NUM_G1}" || -z "${CLIENT_NUM_G2}" ]]; then
    echo "Please input the number of clients correctly"
    read CLIENT_NUM_G1
    read CLIENT_NUM_G2
fi


echo "CLIENT_NUM_G1: $CLIENT_NUM_G1";
echo "CLIENT_NUM_G2: $CLIENT_NUM_G2";

DOWNGRADE_SUPPORTED="N"

for i in $(seq $CLIENT_NUM_G1)
do
  java -Dlogfile="logs/upfuzz_client_g1.log" -cp "build/classes/java/main/:dependencies/*:dependencies/:build/resources/main" org/zlab/upfuzz/fuzzingengine/Main -class client -config "$CONFIG" -flag "group1" -downgrade "$DOWNGRADE_SUPPORTED" &
done

for j in $(seq $CLIENT_NUM_G2)
do
  java -Dlogfile="logs/upfuzz_client_g2.log" -cp "build/classes/java/main/:dependencies/*:dependencies/:build/resources/main" org/zlab/upfuzz/fuzzingengine/Main -class client -config "$CONFIG" -flag "group2" -downgrade "$DOWNGRADE_SUPPORTED" &
done
