#!/usr/bin/env bash

if [ ${IS_HMASTER} = "false" ]
then
    exit 0
fi

time=0
while true; do
    proc=`jps`
    echo "processes = $proc"
    if [[ $proc == *"HMaster"* || $proc == *"HQuorumPeer"* ]]; then
      echo "It's there!"
      time=$((time+1))
      echo "time = $time"
    fi
    if [[ $time -eq 3 ]]; then
      break
    fi
    sleep 5
done

echo "Starting HBase Daemon"
python3 /hbase/hbase_daemon.py
