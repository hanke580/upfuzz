#!/usr/bin/env bash
set -euo pipefail

# Get running container's IP
IP=$(hostname --ip-address | cut -f 1 -d ' ')
if [ $# == 1 ]; then
    NAMENODE="$1,$IP"
else NAMENODE="$IP"; fi

IP_MASK=$(echo $IP | cut -d "." -f -3)
HDFS_NAMENODE=$IP_MASK.2
HDFS_SECONDARY_NAMENODE=$IP_MASK.3
HDFS_DATANODE1=$IP_MASK.4
HDFS_DATANODE2=$IP_MASK.5

# Change it to the target systems
ORG_VERSION=hadoop-2.9.2
UPG_VERSION=hadoop-3.3.0_14509

# create necessary dirs (some version of cassandra cannot create these)
mkdir -p /var/log/hdfs
mkdir -p /var/lib/hdfs

if [[ ! -f "/var/log/.setup_conf" ]]; then
    echo "copy hadoop dir and format configurations"
    for VERSION in ${ORG_VERSION} ${UPG_VERSION}; do
        mkdir /etc/${VERSION}
        cp -r /hdfs/${VERSION}/etc /etc/${VERSION}/

        # disable config testing for debugging purpose
        CONFIG="/etc/${VERSION}/etc/hadoop"
        cp /hadoop-config/core-site.xml $CONFIG/

        if [[ "$IP" == "$HDFS_NAMENODE" ]];
        then
                cp /hadoop-config/hdfs-site-nn.xml $CONFIG/hdfs-site.xml
        elif [[ "$IP" == "$HDFS_SECONDARY_NAMENODE" ]];
        then
                cp /hadoop-config/hdfs-site-snn.xml $CONFIG/hdfs-site.xml
        elif [[ "$IP" == "$HDFS_DATANODE1" ]];
        then
                cp /hadoop-config/hdfs-site-dn1.xml $CONFIG/hdfs-site.xml
        elif [[ "$IP" == "$HDFS_DATANODE2" ]];
        then
                cp /hadoop-config/hdfs-site-dn2.xml $CONFIG/hdfs-site.xml
        fi

        # Only for 2.x version
        arrIN=(${VERSION//-/ })
        VERSION_PART=${arrIN[1]}
        arrIN=(${VERSION_PART//./ })
        MAJOR_VERSION=arrIN[0]
        if [[ "$MAJOR_VERSION" -le 3 ]];
        then
                echo "2.x version"
                echo "export HADOOP_SECURE_DN_USER=root" >> ${CONFIG}/hadoop-env.sh
                echo "export JSVC_HOME=/hdfs/$VERSION/libexec" >> ${CONFIG}/hadoop-env.sh
        fi

        echo "export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/" >> ${CONFIG}/hadoop-env.sh
        echo "export HDFS_NAMENODE_USER=\"root\"" >> ${CONFIG}/hadoop-env.sh
        echo "export HDFS_DATANODE_USER=\"root\"" >> ${CONFIG}/hadoop-env.sh
        echo "export HDFS_SECONDARYNAMENODE_USER=\"root\"" >> ${CONFIG}/hadoop-env.sh

        echo "export HADOOP_LOG_DIR=/var/log/hdfs" >> ${CONFIG}/hadoop-env.sh

        echo "export HADOOP_CLASSPATH=\${HADOOP_CLASSPATH}:/hdfs/${VERSION}" >> ${CONFIG}/hadoop-env.sh

        echo datanode1 > ${CONFIG}/slaves
        echo datanode2 >> ${CONFIG}/slaves

        echo datanode1 > ${CONFIG}/workers
        echo datanode2 >> ${CONFIG}/workers

        # always configure a 3 node HDFS cluster, .3/.4 is the data node
    done
    echo "setup done"
    touch "/var/log/.setup_conf"
fi

if [[ -z $(grep -F "master" "/etc/hosts") ]];
then
        echo "$HDFS_NAMENODE    master" >> /etc/hosts
        echo "$HDFS_SECONDARY_NAMENODE    secondarynn" >> /etc/hosts
        echo "$HDFS_DATANODE1    datanode1" >> /etc/hosts
        echo "$HDFS_DATANODE2    datanode2" >> /etc/hosts
fi

echo "Starting HDFS on $IP..."

if [[ "$IP" == "$HDFS_NAMENODE" ]];
then
        if [[ ! -f /var/log/hdfs/.formatted ]];
        then
                echo "formatting namenode"
                $HADOOP_HOME/bin/hdfs namenode -format
                touch /var/log/hdfs/.formatted
        fi

        splitArr=(${HADOOP_HOME//\// })
        CUR_VERSION=${splitArr[1]}
        echo "cur version = $CUR_VERSION"
        echo "org version = $ORG_VERSION"
        echo "up  version = $UPG_VERSION"
        if [[ $CUR_VERSION == $ORG_VERSION ]];
        then
                            echo "start up old version $HADOOP_HOME"
                $HADOOP_HOME/sbin/hadoop-daemon.sh start namenode
        else
                            echo "start up new version $HADOOP_HOME"
                $HADOOP_HOME/sbin/hadoop-daemon.sh start namenode -rollingUpgrade started
        fi
elif [[ "$IP" == "$HDFS_SECONDARY_NAMENODE" ]];
then
        $HADOOP_HOME/sbin/hadoop-daemon.sh start secondarynamenode
else
        $HADOOP_HOME/sbin/hadoop-daemon.sh start datanode
fi
