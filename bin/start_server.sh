#!/bin/bash

if [[ ( $@ == "--help") ||  $@ == "-h" ]]
then
        echo "Usage: $0 config file"
        exit 0
fi


if [[ "$#" == 0 ]];
then
        CONFIG=config.json
else
        CONFIG=$1
fi

java -ea -Dlogfile="logs/upfuzz_server.log" -cp "build/classes/java/main/:dependencies/*:dependencies/:build/resources/main" org/zlab/upfuzz/fuzzingengine/Main -class server -config $CONFIG
