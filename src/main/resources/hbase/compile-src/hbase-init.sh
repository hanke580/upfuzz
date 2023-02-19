#!/usr/bin/env bash
set -euo pipefail

if [[ -z $(grep -F "master" "/etc/hosts") ]];
then
        echo "252.11.1.10   master" >> /etc/hosts
        echo "252.11.1.2    hmaster" >> /etc/hosts
        echo "252.11.1.3    hregion1" >> /etc/hosts
        echo "252.11.1.4    hregion2" >> /etc/hosts
        echo "master written to host"
fi

# Connection to NN
while true; do
    /hadoop/hadoop-2.10.2/bin/hadoop fs -ls hdfs://master:8020/
    if [[ "$?" -eq 0 ]];
    then
        break
    fi
    sleep 5
done

/bin/bash -c "/hbase/hbase-2.4.15/bin/start-hbase.sh"
