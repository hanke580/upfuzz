#!/usr/bin/env bash

# echo /etc/hosts, node0 will always be the KDC server

# This only run on Node0


IP=$(hostname --ip-address | cut -f 1 -d ' ')
IP_MASK=$(echo $IP | cut -d "." -f -3)
KDC_SERVER=$IP_MASK.2
if [[ -z $(grep -F "kdc_server" "/etc/hosts") ]];
then
        echo "$KDC_SERVER    kdc_server" >> /etc/hosts
fi


IP_MASK=$(echo $IP | cut -d "." -f -3)
HDFS_NAMENODE=$IP_MASK.2
HDFS_SECONDARY_NAMENODE=$IP_MASK.3
HDFS_DATANODE1=$IP_MASK.4
HDFS_DATANODE2=$IP_MASK.5

if [[ "$IP" == "$KDC_SERVER" ]];
then
        { echo 'password'; echo 'password'; } | krb5_newrealm
        { echo 'password'; echo 'password'; } | kadmin.local addprinc ubuntu
        { echo 'password'; echo 'password'; } | kadmin.local addprinc ubuntu/admin

        # restart kdc
        service krb5-admin-server restart
        service krb5-kdc restart

        { echo 'password'; echo 'password'; } | kadmin.local addprinc ubuntu/admin 
        kadmin.local addprinc -randkey hdfs/master
        kadmin.local ktadd -k /master.keytab hdfs/master
        kinit hdfs/master -k -t /master.keytab 
elif [[ "$IP" == "$HDFS_SECONDARY_NAMENODE" ]];
then
        { echo 'password';} | kadmin -p ubuntu/admin addprinc -randkey hdfs/secondarynn
        { echo 'password';} | kadmin -p ubuntu/admin addprinc -randkey HTTP/secondarynn
        { echo 'password';} | kadmin -p ubuntu/admin ktadd -k /secondarynn.keytab hdfs/secondarynn HTTP/secondarynn
        kinit hdfs/secondarynn -k -t /secondarynn.keytab 
elif [[ "$IP" == "$HDFS_DATANODE1" ]];
then
        { echo 'password';} | kadmin -p ubuntu/admin addprinc -randkey hdfs/datanode1
        { echo 'password';} | kadmin -p ubuntu/admin ktadd -k /datanode1.keytab hdfs/datanode1
        kinit hdfs/datanode1 -k -t /datanode1.keytab
elif [[ "$IP" == "$HDFS_DATANODE2" ]];
then
        { echo 'password';} | kadmin -p ubuntu/admin addprinc -randkey hdfs/datanode2
        { echo 'password';} | kadmin -p ubuntu/admin ktadd -k /datanode2.keytab hdfs/datanode2
        kinit hdfs/datanode2 -k -t /datanode2.keytab 
fi

