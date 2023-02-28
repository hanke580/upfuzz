#!/usr/bin/env bash
set -euo pipefail

if [[ -z $(grep -F "master" "/etc/hosts") ]];
then
        echo ${HADOOP_IP}"   master" >> /etc/hosts
        echo "master written to host"
fi

mkdir -p ${HADOOP_CONF_DIR}

bin=${HADOOP_HOME}

cp ${bin}/etc/hadoop/* ${HADOOP_CONF_DIR}/
cp /test_config/oriconfig/* ${HADOOP_CONF_DIR}/ -f

DEFAULT_LIBEXEC_DIR="$bin"/libexec
HADOOP_LIBEXEC_DIR=${HADOOP_LIBEXEC_DIR:-$DEFAULT_LIBEXEC_DIR}
. $HADOOP_LIBEXEC_DIR/hadoop-config.sh --config ${HADOOP_CONF_DIR}

if [[ ! -f /var/hadoop/data/.formatted ]];
then
        echo "formatting namenode"
        ${bin}/bin/hdfs namenode -format
        touch /var/hadoop/data/.formatted
fi

# start hdfs daemons if hdfs is present
if [ -f "${HADOOP_HDFS_HOME}"/sbin/start-dfs.sh ]; then
  "${HADOOP_HDFS_HOME}"/sbin/start-dfs.sh --config $HADOOP_CONF_DIR
fi

# start yarn daemons if yarn is present
if [ -f "${HADOOP_YARN_HOME}"/sbin/start-yarn.sh ]; then
  "${HADOOP_YARN_HOME}"/sbin/start-yarn.sh --config $HADOOP_CONF_DIR
fi

