#!/usr/bin/env bash
set -euo pipefail

# This will only be executed for once in the old version
# not using the jacoco since we don't want the coverage of it

# Make sure this is nn

IP=$(hostname --ip-address | cut -f 1 -d ' ')
IP_MASK=$(echo $IP | cut -d "." -f -3)
HDFS_NAMENODE=$IP_MASK.2

if [[ "$IP" == "$HDFS_NAMENODE" && ! -f /var/log/hdfs/.formatted ]];
then
        echo "formatting namenode"
        unset JAVA_TOOL_OPTIONS
        $HADOOP_HOME/bin/hdfs namenode -format
        touch /var/log/hdfs/.formatted
fi

