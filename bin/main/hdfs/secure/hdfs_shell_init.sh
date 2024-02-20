#!/usr/bin/env bash

HDFS="$HADOOP_HOME/bin/hdfs"

# TODO: Add similar jps check bug for Secur data node

while true; do
    $HDFS dfs -ls /
    if [[ "$?" -eq 0 ]];
    then
        break
    fi
    sleep 5
done

$HDFS dfsdaemon
