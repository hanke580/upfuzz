#!/bin/bash

# check whether user had supplied -h or --help . If yes display usage
if [[ ( $@ == "--help") ||  $@ == "-h" ]]
then 
	echo "Usage: $0 CLIENT_NUM"
	exit 0
fi 

CLIENT_NUM=$1

if [ -z "${CLIENT_NUM}" ]; then
    echo "Please input the number of clients"
    read CLIENT_NUM
fi


echo "CLIENT_NUM: $CLIENT_NUM";

for i in $(seq $CLIENT_NUM)
do
  java -cp "build/classes/java/main/:dependencies/*:dependencies/:build/resources/main" org/zlab/upfuzz/fuzzingengine/Main -class client -config ./config.json &
done
