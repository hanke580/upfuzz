#!/usr/bin/env bash
set -euo pipefail

if [[ -z $(grep -F "master" "/etc/hosts") ]];
then
    echo ${HADOOP_IP}"   master" >> /etc/hosts
    echo "master written to host"
fi

mkdir -p ${HBASE_CONF}

bin=${HBASE_HOME}

cp -f ${bin}/conf/* ${HBASE_CONF}/
if [ ${CUR_STATUS} = "ORI" ]
then
    cp -f /test_config/oriconfig/* ${HBASE_CONF}/
else
    cp -f /test_config/upconfig/* ${HBASE_CONF}/
fi

export HBASE_ENV_INIT=
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/
export HBASE_CONF_DIR=${HBASE_CONF}

if [ ${CUR_STATUS} = "UP" ]
then
    HBASE_REGIONSERVERS="${HBASE_REGIONSERVERS:-$HBASE_CONF/regionservers}"
    "$bin"/bin/rolling-restart.sh
fi

if [ ${IS_HMASTER} = "false" ]
then
    supervisorctl restart upfuzz_hbase:hbase_daemon
fi