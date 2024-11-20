#!/bin/bash

# check whether user had supplied -h or --help . If yes display usage
if [[ ( $@ == "--help") ||  $@ == "-h" ]]
then
        echo "Usage: $0 CLIENT_NUM"
        echo "Usage: $1 config file"
        exit 0
fi

CLIENT_NUM=$1
if [[ -z $2 ]];
then
        echo "using config.json"
        CONFIG="config.json"
else
        echo "using $2"
        CONFIG=$2
fi

if [ -z "${CLIENT_NUM}" ]; then
    echo "Please input the number of clients"
    read CLIENT_NUM
fi


echo "CLIENT_NUM: $CLIENT_NUM";

for i in $(seq $CLIENT_NUM)
do
  java -ea -Dlogfile="logs/upfuzz_client_${i}.log" -cp "build/classes/java/main/:dependencies/*:dependencies/:build/resources/main" org/zlab/upfuzz/fuzzingengine/Main -class client -config "$CONFIG" &
done
