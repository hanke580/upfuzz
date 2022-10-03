#!/usr/bin/env bash
set -euo pipefail

# Get running container's IP
IP=$(hostname --ip-address | cut -f 1 -d ' ')
if [ $# == 1 ]; then
    NAMENODE="$1,$IP"
else NAMENODE="$IP"; fi

# Change it to the target systems
ORG_VERSION=hadoop-2.10.2
UPG_VERSION=hadoop-2.10.2-dup

# create necessary dirs (some version of cassandra cannot create these)
mkdir -p /var/log/hdfs
mkdir -p /var/lib/hdfs

if [[ ! -f "/tmp/.setup_conf" ]]; then
    echo "copy hadoop dir and format configurations"
    for VERSION in ${ORG_VERSION} ${UPG_VERSION}; do
#        # Modify the binary where all container use one copy
#        # Inside it should be general setting: JAVA version
#        # and points to another configuration file
#        if [[ -z $(grep -F "java-8-openjdk-amd64" "/hadoop/hadoop-2.10.2/etc/had/hadoop-env.sh") ]];
#        then
#                sed -i 's/export JAVA_HOME=${JAVA_HOME}/export JAVA_HOME=\/usr\/lib\/jvm\/java-8-openjdk-amd64\//' ${CONFIG}/hadoop-env.sh
#                sed -i
#                echo "export HADOOP_LOG_DIR=/var/hadoop/logs" >> ${CONFIG}\hadoop-env.sh
#        fi
#
#        cp -r "/hdfs/${VERSION}/etc/hadoop/*" "/var/hadoop_conf"

        cp /hadoop-config/* /hdfs/${VERSION}/etc/hadoop/
        CONFIG="/hdfs/${VERSION}/etc/hadoop/"

        # config on-disk data locations
        sed -i 's/export JAVA_HOME=${JAVA_HOME}/export JAVA_HOME=\/usr\/lib\/jvm\/java-8-openjdk-amd64\//' ${CONFIG}/hadoop-env.sh

        echo "export HADOOP_LOG_DIR=/var/hadoop/logs" >> ${CONFIG}\hadoop-env.sh

        echo node1 > ${CONFIG}/slaves
        echo node2 >> ${CONFIG}/slaves

        # always configure a 3 node HDFS cluster, .3/.4 is the data node
    done
    echo "setup done"
    touch "/tmp/.setup_conf"
fi


IP_MASK=$(echo $IP | cut -d "." -f -3)
HDFS_NAMENODE=$IP_MASK.2
HADOOP_HOME=/hdfs/$ORG_VERSION

if [[ -z $(grep -F "master" "/etc/hosts") ]];
then
        echo "$HDFS_NAMENODE    master" >> /etc/hosts
        echo "$IP_MASK.3    node1" >> /etc/hosts
        echo "$IP_MASK.4    node2" >> /etc/hosts

        echo "HADOOP_HOME=$HADOOP_HOME" >> ~/.bashrc
        echo "PATH=\${PATH}:\${HADOOP_HOME}/bin:\${HADOOP_HOME}/sbin" >> ~/.bashrc
        source ~/.bashrc
fi

#HADOOP_HOME=/etc/$ORG_VERSION
#PATH=${PATH}:${HADOOP_HOME}/bin:${HADOOP_HOME}/sbin

echo "Starting HDFS on $IP..."

if [[ "$IP" == "$HDFS_NAMENODE" ]];
then
        $HADOOP_HOME/bin/hdfs namenode -format
        $HADOOP_HOME/sbin/hadoop-daemon.sh start namenode
else
        $HADOOP_HOME/sbin/hadoop-daemon.sh start datanode
fi

#echo "ENV: HOME:${CASSANDRA_HOME}\nCONF:${CASSANDRA_CONF}"
#exec cassandra -f
#exec $CASSANDRA_HOME/bin/cassandra -fR
# use R so that Cassandra can be run as root