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

if [ ${IS_HMASTER} = "false" ]
then
    exit 0
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

# /bin/bash -c "/hbase/hbase-2.4.15/bin/start-hbase.sh"

# . "$bin"/bin/hbase-config.sh --config ${HBASE_CONF}

# HBASE-6504 - only take the first line of the output in case verbose gc is on
# distMode=`$bin/bin/hbase --config "$HBASE_CONF" org.apache.hadoop.hbase.util.HBaseConfTool hbase.cluster.distributed | head -n 1`

# if [ "$distMode" == 'false' ]
# then
#   "$bin"/bin/hbase-daemon.sh --config "${HBASE_CONF_DIR}" $commandToRun master
# else
#   "$bin"/bin/hbase-daemons.sh --config "${HBASE_CONF_DIR}" $commandToRun zookeeper
#   "$bin"/bin/hbase-daemon.sh --config "${HBASE_CONF_DIR}" $commandToRun master
#   "$bin"/bin/hbase-daemons.sh --config "${HBASE_CONF_DIR}" \
#     --hosts "${HBASE_REGIONSERVERS}" $commandToRun regionserver
#   "$bin"/bin/hbase-daemons.sh --config "${HBASE_CONF_DIR}" \
#     --hosts "${HBASE_BACKUP_MASTERS}" $commandToRun master-backup
# fi

HBASE_REGIONSERVERS="${HBASE_REGIONSERVERS:-$HBASE_CONF/regionservers}"
"$bin"/bin/hbase-daemons.sh --config "${HBASE_CONF}" start zookeeper
"$bin"/bin/hbase-daemon.sh --config "${HBASE_CONF}" start master
"$bin"/bin/hbase-daemons.sh --config "${HBASE_CONF}" \
    --hosts "${HBASE_REGIONSERVERS}" start regionserver

python3 /hbase/hbase_daemon.py
