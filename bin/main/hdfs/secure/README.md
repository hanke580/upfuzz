# Reference

[IBM Secure Hadoop](https://www.ibm.com/docs/en/spectrum-symphony/7.3.0?topic=mapreduce-hadoop-security-configuration)

[Hadoop 2.x ERROR](https://www.cnblogs.com/bugzeroman/p/12858219.html)

[Kerberos for Ubuntu](https://ubuntu.com/server/docs/service-kerberos-principals)

[JSVC](https://commons.apache.org/proper/commons-daemon/download_daemon.cgi)


/etc/krb5.conf
```bash


kadmin.local addprinc -randkey hdfs/master
kadmin.local ktadd -k /master.keytab hdfs/master

kadmin.local addprinc -randkey hdfs/secondarynn
kadmin.local ktadd -k /secondarynn.keytab hdfs/secondarynn

kadmin.local addprinc -randkey hdfs/datanode1
kadmin.local ktadd -k /datanode1.keytab hdfs/datanode1

kadmin.local addprinc -randkey hdfs/datanode2
kadmin.local ktadd -k /datanode2.keytab hdfs/datanode2

service --status-all
kadmin -p ubuntu/admin


export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64

vi hadoop-env.sh
export HADOOP_SECURE_DN_USER=hdfs
export JSVC_HOME=/home/hdfs/hadoop-3.0.0-alpha2-SNAPSHOT/libexec
```

The other nodes need to wait for the keytab file.