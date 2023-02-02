 ## Configure a HBase cluster
 
Cluster with 3 nodes
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
    <value>hdfs://252.11.1.2:8020/hbase</value>
  </property>
  <property>
  <name>hbase.zookeeper.quorum</name>
  <value>252.11.1.2,252.11.1.3</value>
  </property>
  <property>
    <name>hbase.zookeeper.property.dataDir</name>
    <value>/usr/local/zookeeper</value>
  </property>
</configuration>
```

hbase/conf/hvase-env.sh
```shell
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/
```

hbase/conf/regionservers
```shell
252.11.1.3
```

**Start cluster**
```shell
docker build . -t upfuzz_hbase:hbase-2.4.15
docker compose up
```
