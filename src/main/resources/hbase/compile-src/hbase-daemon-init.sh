#!/usr/bin/env bash

#if [ ${IS_HMASTER} = "false" ]
#then
#    exit 0
#fi

if [ ${IS_HMASTER} = "true" ]
then
    JPS_PROCESS="HMaster"
else
    JPS_PROCESS="HRegionServer"
fi

# Wait for 40 seconds for HBase to start
# jps: target process should exist for at least 40 seconds

time=0
while true; do
    proc=`jps`
    echo "processes = $proc"
    if [[ $proc == *${JPS_PROCESS}* ]]; then
      echo "It's there!"
      time=$((time+1))
      echo "time = $time"
    fi
    if [[ $time -eq 8 ]]; then
      break
    fi
    sleep 5
done

echo "Starting HBase Daemon"
# python3 /hbase/hbase_daemon.py
python3 ${HBASE_HOME}/bin/hbase_daemon.py