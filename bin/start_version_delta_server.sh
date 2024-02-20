#!/bin/bash

if [[ ( $@ == "--help") ||  $@ == "-h" ]]
then
        echo "Usage: $0 CLIENT_NUM_G1"
        echo "Usage: $1 CLIENT_NUM_G2"
        echo "Usage: $2 config file"
        exit 0
fi


if [[ "$#" == 0 ]];
then
        CONFIG=config.json
else
        CONFIG=$2
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

java -Dlogfile="logs/upfuzz_server.log" -cp "build/classes/java/main/:dependencies/*:dependencies/:build/resources/main" org/zlab/upfuzz/fuzzingengine/Main -class server -config $CONFIG -group1 $CLIENT_NUM_G1 -group2 $CLIENT_NUM_G2
