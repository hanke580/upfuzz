 ## Configure a HBase cluster
 
Cluster with a HMaster, 2 HRegionServer and a Hadoop
1. Use ssh public key to make sure they can ssh to each other without password
2. Modify the configuration files

All nodes should share the same configuration file.

**Sample Configuration file**

hbase/conf/hbase-site.xml
```xml
<configuration>
  <property>
    <name>hbase.cluster.distributed</name>
    <value>true</value>
  </property>
  <property>
    <name>hbase.rootdir</name>
    <value>hdfs://master:8020/hbase</value>
  </property>
  <property>
  <name>hbase.zookeeper.quorum</name>
  <value>252.11.1.2,252.11.1.3,252.11.1.4</value>
  </property>
  <property>
    <name>hbase.zookeeper.property.dataDir</name>
    <value>/usr/local/zookeeper</value>
  </property>
</configuration>
```

hbase/conf/hbase-env.sh
```shell
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/
```

hbase/conf/regionservers
```shell
252.11.1.3
252.11.1.4
```

hadoop/etc/hadoop/hadoop-env.sh
```shell
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/
```

hadoop/etc/hadoop/core-site.xml
```shell
<configuration>
    <property>
        <name>fs.default.name</name>
        <value>hdfs://master:8020</value>
    </property>
</configuration>
```

hadoop/etc/hadoop/hdfs-site.xml
```shell
<configuration>
    <property>
        <name>dfs.namenode.name.dir</name>
        <value>/var/hadoop/data/nameNode</value>
    </property>

    <property>
        <name>dfs.datanode.data.dir</name>
        <value>/var/hadoop/data/dataNode</value>
    </property>
</configuration>
```

**Start cluster**
```shell
cd src/main/resources/hdfs/hbase-pure
docker build . -t upfuzz_hdfs:hadoop-2.10.2
```

```shell
cd src/main/resources/hbase/hbase-2.4.15/compile-src
docker build . -t upfuzz_hbase:hbase-2.4.15
docker compose up
```
