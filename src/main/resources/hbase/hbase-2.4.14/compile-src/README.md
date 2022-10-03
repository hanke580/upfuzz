 ## Configure a HDFS cluster (A basis for HBase cluster)
 
Cluster with 3 nodes
1. Use ssh public key to make sure they can ssh to each other without password
2. Modify the configuration files

All nodes should share the same configuration file.

**Sample Configuration file**

etc/hadoop/hdfs-site.xml
```xml
<configuration>
    <property>
            <name>dfs.namenode.name.dir</name>
            <value>/var/hadoop/hadoop-2.10.2/data/nameNode</value>
    </property>

    <property>
            <name>dfs.datanode.data.dir</name>
            <value>/var/hadoop/hadoop-2.10.2/data/dataNode</value>
    </property>

    <property>
            <name>dfs.replication</name>
            <value>1</value>
    </property>
    <property>
            <name>dfs.namenode.datanode.registration.ip-hostname-check</name>
            <value>false</value>
    </property>
</configuration>
```

etc/hadoop/hadoop-env.sh
```shell
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/
```

etc/hadoop/slaves
* Mark the slave nodes
* In the hadoop-3, they might use `workers`
```shell
172.17.0.3
172.17.0.4
```

We can also assign hostname as a replacement for the ipaddress
```shell
vim /etc/hosts

172.17.0.2      node-master
172.17.0.3      node1
172.17.0.4      node2
```

If we do use IP for all, we need to add `dfs.namenode.datanode.registration.ip-hostname-check` in hdfs-site.xml.

Set up the bashrc
```shell
export HADOOP_HOME=/etc/hadoop-2.10.2
export PATH=${PATH}:${HADOOP_HOME}/bin:${HADOOP_HOME}/sbin

echo "export HADOOP_HOME=/etc/hadoop-2.10.2" >> ~/.bashrc
echo "export PATH=\${PATH}:\${HADOOP_HOME}/bin:\${HADOOP_HOME}/sbin" >> ~/.bashrc
source ~/.bashrc
```

**Start cluster**
```shell
hdfs namenode -format
start-dfs.sh
```

**Stop cluster**
```shell
stop-dfs.sh
rm -rf data/* logs/*
```