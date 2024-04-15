# upfuzz

> A tool to detect upgrade bugs of distributed systems

## Feature
* Coverage-guided structural fuzz testing
  * upfuzz collects the code coverage of the cluster to guide the testing
  process.
  * upfuzz implements a type system for mutation. Users only need to
      implement their command via the given types, and upfuzz can
      generate/mutate valid command sequence.
* Fault Injection
* Inconsistency Detector
    * upfuzz use inconsistency detector to extract a list of system states and
    compare the value between (1) rolling upgrade and (2) full-stop upgrade to
    detect transient bugs.
* Configuration Generator
    * upfuzz utilizes static analysis to focus on testing a set of configurations.
* Data format likely invariants
* Nyx-snapshot to avoid the startup time

## Prerequisite
```bash
# jdk
sudo apt-get install openjdk-11-jdk openjdk-8-jdk

# docker
sudo apt-get update
sudo apt-get install \
    ca-certificates \
    curl \
    gnupg \
    lsb-release -y
sudo mkdir -p /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update
sudo apt-get install docker-ce docker-ce-cli containerd.io docker-compose-plugin -y
```

## Data Format Testing
> Check out dinv-monitor about how to create an instrumented tarball
> 
> Instrumented tarball is stored `khan@mufasa:/home/khan/format_inst_binary/`
> 

1. Use a format instrumented tarball.
2. Make sure the `configInfo/system-x.x.x` contain `serializedFields_alg1.json` and `topObjects.json` file. (They should be the same as the one under the instrumented system binary).
3. Enable `useFormatCoverage` in configuration file.

The other steps are the same as the normal testing.

## Version Delta Testing
### Cassandra
```bash
git clone git@github.com:zlab-purdue/upfuzz.git
cd upfuzz
git checkout feature/version_delta
export UPFUZZ_DIR=$PWD
export ORI_VERSION=3.11.15
export UP_VERSION=4.1.3

mkdir -p "$UPFUZZ_DIR"/prebuild/cassandra
cd prebuild/cassandra
wget https://archive.apache.org/dist/cassandra/"$ORI_VERSION"/apache-cassandra-"$ORI_VERSION"-bin.tar.gz ; tar -xzvf apache-cassandra-"$ORI_VERSION"-bin.tar.gz
wget https://archive.apache.org/dist/cassandra/"$UP_VERSION"/apache-cassandra-"$UP_VERSION"-bin.tar.gz ; tar -xzvf apache-cassandra-"$UP_VERSION"-bin.tar.gz
sed -i 's/num_tokens: 16/num_tokens: 256/' apache-cassandra-"$UP_VERSION"/conf/cassandra.yaml

cd ${UPFUZZ_DIR}
cp src/main/resources/cqlsh_daemon2.py prebuild/cassandra/apache-cassandra-"$ORI_VERSION"/bin/cqlsh_daemon.py
cp src/main/resources/cqlsh_daemon4.py  prebuild/cassandra/apache-cassandra-"$UP_VERSION"/bin/cqlsh_daemon.py

cd src/main/resources/cassandra/normal/compile-src/
sed -i "s/ORI_VERSION=apache-cassandra-.*$/ORI_VERSION=apache-cassandra-$ORI_VERSION/" cassandra-clusternode.sh
sed -i "s/UP_VERSION=apache-cassandra-.*$/UP_VERSION=apache-cassandra-$UP_VERSION/" cassandra-clusternode.sh
docker build . -t upfuzz_cassandra:apache-cassandra-"$ORI_VERSION"_apache-cassandra-"$UP_VERSION"
# modify the cassandra-clusternode.sh file: reverse ORI_VERSION and UP_VERSION
sed -i "s/ORI_VERSION=apache-cassandra-.*$/ORI_VERSION=apache-cassandra-$UP_VERSION/" cassandra-clusternode.sh
sed -i "s/UP_VERSION=apache-cassandra-.*$/UP_VERSION=apache-cassandra-$ORI_VERSION/" cassandra-clusternode.sh
docker build . -t upfuzz_cassandra:apache-cassandra-"$UP_VERSION"_apache-cassandra-"$ORI_VERSION"

cd ${UPFUZZ_DIR}
./gradlew copyDependencies
./gradlew :spotlessApply build

# open terminal1: start server
bin/start_server.sh config.json
# open terminal2: start two groups of agents: 3 agents in group 1, 2 agents in group 2
bin/start_version_delta_clients.sh 3 2 config.json

# stop testing:
bin/cass_cl.sh 3.11.15 4.1.3
bin/cass_cl.sh 4.1.3 3.11.15

# Check failures
# python3 proc_failure.py cassandra &> /dev/null | python3 proc_failure.py read
```


### HDFS

```bash
git clone git@github.com:zlab-purdue/upfuzz.git
cd upfuzz
export UPFUZZ_DIR=$PWD
export ORI_VERSION=2.10.2
export UP_VERSION=3.3.6

mkdir -p $UPFUZZ_DIR/prebuild/hdfs
cd $UPFUZZ_DIR/prebuild/hdfs
wget https://archive.apache.org/dist/hadoop/common/hadoop-"$ORI_VERSION"/hadoop-"$ORI_VERSION".tar.gz ; tar -xzvf hadoop-$ORI_VERSION.tar.gz
wget https://archive.apache.org/dist/hadoop/common/hadoop-"$UP_VERSION"/hadoop-"$UP_VERSION".tar.gz ; tar -xzvf hadoop-"$UP_VERSION".tar.gz

# old version hdfs daemon
cp $UPFUZZ_DIR/src/main/resources/FsShellDaemon2.java $UPFUZZ_DIR/prebuild/hdfs/hadoop-"$ORI_VERSION"/FsShellDaemon.java
cd $UPFUZZ_DIR/prebuild/hdfs/hadoop-"$ORI_VERSION"/
/usr/lib/jvm/java-8-openjdk-amd64/bin/javac -d . -cp "share/hadoop/hdfs/*:share/hadoop/common/*:share/hadoop/common/lib/*" FsShellDaemon.java
sed -i "s/elif \[ \"\$COMMAND\" = \"dfs\" \] ; then/elif [ \"\$COMMAND\" = \"dfsdaemon\" ] ; then\n  CLASS=org.apache.hadoop.fs.FsShellDaemon\n  HADOOP_OPTS=\"\$HADOOP_OPTS \$HADOOP_CLIENT_OPTS\"\n&/" bin/hdfs

# new version hdfs daemon
cp $UPFUZZ_DIR/src/main/resources/FsShellDaemon_trunk.java $UPFUZZ_DIR/prebuild/hdfs/hadoop-"$UP_VERSION"/FsShellDaemon.java
cd $UPFUZZ_DIR/prebuild/hdfs/hadoop-"$UP_VERSION"/
/usr/lib/jvm/java-8-openjdk-amd64/bin/javac -d . -cp "share/hadoop/hdfs/*:share/hadoop/common/*:share/hadoop/common/lib/*" FsShellDaemon.java
sed -i "s/  case \${subcmd} in/&\n    dfsdaemon)\n      HADOOP_CLASSNAME=\"org.apache.hadoop.fs.FsShellDaemon\"\n    ;;/" bin/hdfs

cd $UPFUZZ_DIR/src/main/resources/hdfs/compile-src/
sed -i "s/ORG_VERSION=hadoop-.*$/ORG_VERSION=hadoop-$ORI_VERSION/" hdfs-clusternode.sh
sed -i "s/UPG_VERSION=hadoop-.*$/UPG_VERSION=hadoop-$UP_VERSION/" hdfs-clusternode.sh
docker build . -t upfuzz_hdfs:hadoop-"$ORI_VERSION"_hadoop-"$UP_VERSION"
# replace up and down version
sed -i "s/ORG_VERSION=hadoop-.*$/ORG_VERSION=hadoop-$UP_VERSION/" hdfs-clusternode.sh
sed -i "s/UPG_VERSION=hadoop-.*$/UPG_VERSION=hadoop-$ORI_VERSION/" hdfs-clusternode.sh
docker build . -t upfuzz_hdfs:hadoop-"$UP_VERSION"_hadoop-"$ORI_VERSION"

cd $UPFUZZ_DIR
./gradlew copyDependencies
./gradlew :spotlessApply build

# open terminal1: start server
bin/start_server.sh hdfs_config.json
# open terminal2: start one client
bin/start_clients.sh 1 hdfs_config.json

# stop testing:
bin/hdfs_cl.sh
```

### HBase (not finished yet)
```bash
git clone git@github.com:zlab-purdue/upfuzz.git
cd upfuzz
export UPFUZZ_DIR=$PWD
export ORI_VERSION=2.4.17
export UP_VERSION=2.5.8

mkdir -p $UPFUZZ_DIR/prebuild/hadoop
cd $UPFUZZ_DIR/prebuild/hadoop
wget https://archive.apache.org/dist/hadoop/common/hadoop-2.10.2/hadoop-2.10.2.tar.gz ; tar -xzvf hadoop-2.10.2.tar.gz
cp $UPFUZZ_DIR/src/main/resources/hdfs/hbase-pure/core-site.xml $UPFUZZ_DIR/prebuild/hadoop/hadoop-2.10.2/etc/hadoop/ -f
cp $UPFUZZ_DIR/src/main/resources/hdfs/hbase-pure/hdfs-site.xml $UPFUZZ_DIR/prebuild/hadoop/hadoop-2.10.2/etc/hadoop/ -f
cp $UPFUZZ_DIR/src/main/resources/hdfs/hbase-pure/hadoop-env.sh $UPFUZZ_DIR/prebuild/hadoop/hadoop-2.10.2/etc/hadoop/ -f

mkdir -p $UPFUZZ_DIR/prebuild/hbase
cd $UPFUZZ_DIR/prebuild/hbase
wget https://archive.apache.org/dist/hbase/"$ORI_VERSION"/hbase-"$ORI_VERSION"-bin.tar.gz -O hbase-"$ORI_VERSION".tar.gz ; tar -xzvf hbase-"$ORI_VERSION".tar.gz
wget https://archive.apache.org/dist/hbase/"$UP_VERSION"/hbase-"$UP_VERSION"-bin.tar.gz -O hbase-"$UP_VERSION".tar.gz ; tar -xzvf hbase-"$UP_VERSION".tar.gz
cp $UPFUZZ_DIR/src/main/resources/hbase/compile-src/hbase-env.sh $UPFUZZ_DIR/prebuild/hbase/hbase-$ORI_VERSION/conf/ -f
cp $UPFUZZ_DIR/src/main/resources/hbase/compile-src/hbase-env.sh $UPFUZZ_DIR/prebuild/hbase/hbase-$UP_VERSION/conf/ -f

cd $UPFUZZ_DIR/src/main/resources/hdfs/hbase-pure/
docker build . -t upfuzz_hdfs:hadoop-2.10.2

cd $UPFUZZ_DIR/src/main/resources/hbase/compile-src/
docker build . -t upfuzz_hbase:hbase-"$ORI_VERSION"_hbase-"$UP_VERSION"
docker build . -t upfuzz_hbase:hbase-"$UP_VERSION"_hbase-"$ORI_VERSION"

cd $UPFUZZ_DIR
./gradlew copyDependencies
./gradlew :spotlessApply build

# Enable verison delta testing mechanism in hbase_config.json
# open terminal1: start server
bin/start_server.sh hbase_config.json

# open terminal2: start one client
bin/start_clients.sh 1 hbase_config.json

# stop testing:
bin/hbase_cl.sh
```

## Minimal Set up for Cassandra (Try upfuzz quickly!)
Requirement: java11, docker (Docker version 26.0.0, build a5ee5b1)
> - Not test configurations.
> - single Cassandra node upgrade: 3.11.15 => 4.1.3
> - If using Nyx Mode, please clone the upfuzz repo at first and then follow the guide at `nyx_mode/README.md` before continuing.

### Test single version

```bash
ssh-keyscan github.com >> ~/.ssh/known_hosts
git clone git@github.com:zlab-purdue/upfuzz.git
cd upfuzz

export UPFUZZ_DIR=$PWD
export ORI_VERSION=3.11.15
mkdir -p ${UPFUZZ_DIR}/prebuild/cassandra
cd ${UPFUZZ_DIR}/prebuild/cassandra
wget https://archive.apache.org/dist/cassandra/"$ORI_VERSION"/apache-cassandra-"$ORI_VERSION"-bin.tar.gz ; tar -xzvf apache-cassandra-"$ORI_VERSION"-bin.tar.gz
cd ${UPFUZZ_DIR}
cp src/main/resources/cqlsh_daemon2.py prebuild/cassandra/apache-cassandra-"$ORI_VERSION"/bin/cqlsh_daemon.py
cd src/main/resources/cassandra/single-version-testing
sudo chmod 666 /var/run/docker.sock
docker build . -t upfuzz_cassandra:apache-cassandra-"$ORI_VERSION"
cd ${UPFUZZ_DIR}
./gradlew copyDependencies
./gradlew :spotlessApply build

sed -i 's/"testSingleVersion": false,/"testSingleVersion": true,/g' config.json

# open terminal1: start server
bin/start_server.sh config.json
# open terminal2: start one client
bin/start_clients.sh 1 config.json

# stop testing
bin/cass_cl.sh

# Check failures
# python3 proc_failure.py cassandra &> /dev/null | python3 proc_failure.py read
```

### Test upgrade process
```bash
git clone git@github.com:zlab-purdue/upfuzz.git
cd upfuzz
export UPFUZZ_DIR=$PWD
export ORI_VERSION=3.11.15
export UP_VERSION=4.1.3

mkdir -p "$UPFUZZ_DIR"/prebuild/cassandra
cd prebuild/cassandra
wget https://archive.apache.org/dist/cassandra/"$ORI_VERSION"/apache-cassandra-"$ORI_VERSION"-bin.tar.gz ; tar -xzvf apache-cassandra-"$ORI_VERSION"-bin.tar.gz
wget https://archive.apache.org/dist/cassandra/"$UP_VERSION"/apache-cassandra-"$UP_VERSION"-bin.tar.gz ; tar -xzvf apache-cassandra-"$UP_VERSION"-bin.tar.gz
sed -i 's/num_tokens: 16/num_tokens: 256/' apache-cassandra-"$UP_VERSION"/conf/cassandra.yaml

cd ${UPFUZZ_DIR}
cp src/main/resources/cqlsh_daemon2.py prebuild/cassandra/apache-cassandra-"$ORI_VERSION"/bin/cqlsh_daemon.py
cp src/main/resources/cqlsh_daemon4.py  prebuild/cassandra/apache-cassandra-"$UP_VERSION"/bin/cqlsh_daemon.py

cd src/main/resources/cassandra/normal/compile-src/
sed -i "s/ORI_VERSION=apache-cassandra-.*$/ORI_VERSION=apache-cassandra-$ORI_VERSION/" cassandra-clusternode.sh
sed -i "s/UP_VERSION=apache-cassandra-.*$/UP_VERSION=apache-cassandra-$UP_VERSION/" cassandra-clusternode.sh
docker build . -t upfuzz_cassandra:apache-cassandra-"$ORI_VERSION"_apache-cassandra-"$UP_VERSION"

cd ${UPFUZZ_DIR}
./gradlew copyDependencies
./gradlew :spotlessApply build

# open terminal1: start server
bin/start_server.sh config.json
# open terminal2: start one client
bin/start_clients.sh 1 config.json

# stop testing:
bin/cass_cl.sh

# Check failures
# python3 proc_failure.py cassandra &> /dev/null | python3 proc_failure.py read
```

## Minimal Set up for HDFS (Try upfuzz quickly!)
Requirement: jdk8, jdk11, docker (Docker version 26.0.0)
> - Not test configurations.
> - 4 Nodes upgrade (NN, SNN, 2DN) ORI_VERSION => UP_VERSION

```bash
git clone git@github.com:zlab-purdue/upfuzz.git
cd upfuzz
export UPFUZZ_DIR=$PWD
export ORI_VERSION=2.10.2
export UP_VERSION=3.3.6

mkdir -p $UPFUZZ_DIR/prebuild/hdfs
cd $UPFUZZ_DIR/prebuild/hdfs
wget https://archive.apache.org/dist/hadoop/common/hadoop-"$ORI_VERSION"/hadoop-"$ORI_VERSION".tar.gz ; tar -xzvf hadoop-$ORI_VERSION.tar.gz
wget https://archive.apache.org/dist/hadoop/common/hadoop-"$UP_VERSION"/hadoop-"$UP_VERSION".tar.gz ; tar -xzvf hadoop-"$UP_VERSION".tar.gz

# old version hdfs daemon
cp $UPFUZZ_DIR/src/main/resources/FsShellDaemon2.java $UPFUZZ_DIR/prebuild/hdfs/hadoop-"$ORI_VERSION"/FsShellDaemon.java
cd $UPFUZZ_DIR/prebuild/hdfs/hadoop-"$ORI_VERSION"/
/usr/lib/jvm/java-8-openjdk-amd64/bin/javac -d . -cp "share/hadoop/hdfs/*:share/hadoop/common/*:share/hadoop/common/lib/*" FsShellDaemon.java
sed -i "s/elif \[ \"\$COMMAND\" = \"dfs\" \] ; then/elif [ \"\$COMMAND\" = \"dfsdaemon\" ] ; then\n  CLASS=org.apache.hadoop.fs.FsShellDaemon\n  HADOOP_OPTS=\"\$HADOOP_OPTS \$HADOOP_CLIENT_OPTS\"\n&/" bin/hdfs

# new version hdfs daemon
cp $UPFUZZ_DIR/src/main/resources/FsShellDaemon_trunk.java $UPFUZZ_DIR/prebuild/hdfs/hadoop-"$UP_VERSION"/FsShellDaemon.java
cd $UPFUZZ_DIR/prebuild/hdfs/hadoop-"$UP_VERSION"/
/usr/lib/jvm/java-8-openjdk-amd64/bin/javac -d . -cp "share/hadoop/hdfs/*:share/hadoop/common/*:share/hadoop/common/lib/*" FsShellDaemon.java
sed -i "s/  case \${subcmd} in/&\n    dfsdaemon)\n      HADOOP_CLASSNAME=\"org.apache.hadoop.fs.FsShellDaemon\"\n    ;;/" bin/hdfs

cd $UPFUZZ_DIR/src/main/resources/hdfs/compile-src/
docker build . -t upfuzz_hdfs:hadoop-"$ORI_VERSION"_hadoop-"$UP_VERSION"

cd $UPFUZZ_DIR
./gradlew copyDependencies
./gradlew :spotlessApply build

# open terminal1: start server
bin/start_server.sh hdfs_config.json
# open terminal2: start one client
bin/start_clients.sh 1 hdfs_config.json

# stop testing:
bin/hdfs_cl.sh

# Check failures
# python3 proc_failure.py hdfs &> /dev/null | python3 proc_failure.py read
```

**Critical configuration** setting which affects the hdfs upgrade process. Normally `prepareImageFirst` should be false. But to test log replay, we need to set it to true.
```
# hdfs_config.json
prepareImageFirst
```

## Minimal Set up for HBase (Try upfuzz quickly!)

```bash
git clone git@github.com:zlab-purdue/upfuzz.git
cd upfuzz
export UPFUZZ_DIR=$PWD
export ORI_VERSION=2.4.17
export UP_VERSION=2.5.5

mkdir -p $UPFUZZ_DIR/prebuild/hadoop
cd $UPFUZZ_DIR/prebuild/hadoop
wget https://archive.apache.org/dist/hadoop/common/hadoop-2.10.2/hadoop-2.10.2.tar.gz ; tar -xzvf hadoop-2.10.2.tar.gz
cp $UPFUZZ_DIR/src/main/resources/hdfs/hbase-pure/core-site.xml $UPFUZZ_DIR/prebuild/hadoop/hadoop-2.10.2/etc/hadoop/ -f
cp $UPFUZZ_DIR/src/main/resources/hdfs/hbase-pure/hdfs-site.xml $UPFUZZ_DIR/prebuild/hadoop/hadoop-2.10.2/etc/hadoop/ -f
cp $UPFUZZ_DIR/src/main/resources/hdfs/hbase-pure/hadoop-env.sh $UPFUZZ_DIR/prebuild/hadoop/hadoop-2.10.2/etc/hadoop/ -f

mkdir -p $UPFUZZ_DIR/prebuild/hbase
cd $UPFUZZ_DIR/prebuild/hbase
wget https://archive.apache.org/dist/hbase/"$ORI_VERSION"/hbase-"$ORI_VERSION"-bin.tar.gz -O hbase-"$ORI_VERSION".tar.gz ; tar -xzvf hbase-"$ORI_VERSION".tar.gz
wget https://archive.apache.org/dist/hbase/"$UP_VERSION"/hbase-"$UP_VERSION"-bin.tar.gz -O hbase-"$UP_VERSION".tar.gz ; tar -xzvf hbase-"$UP_VERSION".tar.gz
cp $UPFUZZ_DIR/src/main/resources/hbase/compile-src/hbase-env.sh $UPFUZZ_DIR/prebuild/hbase/hbase-$ORI_VERSION/conf/ -f
cp $UPFUZZ_DIR/src/main/resources/hbase/compile-src/hbase-env.sh $UPFUZZ_DIR/prebuild/hbase/hbase-$UP_VERSION/conf/ -f

cd $UPFUZZ_DIR/src/main/resources/hdfs/hbase-pure/
docker build . -t upfuzz_hdfs:hadoop-2.10.2

cd $UPFUZZ_DIR/src/main/resources/hbase/compile-src/
docker build . -t upfuzz_hbase:hbase-"$ORI_VERSION"_hbase-"$UP_VERSION"

cd $UPFUZZ_DIR
./gradlew copyDependencies
./gradlew :spotlessApply build

# open terminal1: start server
bin/start_server.sh hbase_config.json

# open terminal2: start one client
bin/start_clients.sh 1 hbase_config.json

# stop testing:
bin/hbase_cl.sh
```

## Usage

Important configurations
- **testingMode**
  - 0: Execute stacked test packets.
  - 4: Execute stacked test packets and test plan (with fault injection) recursively.

> Config test is disabled by default.
>
> If you want to test configuration, checkout **docs/TEST_CONFIG.md**.

### Testing through UpFuzz image with all dependencies enabled
* You can avoid setting up all the prerequisites by connecting to our image hosted in chameleon cloud

```bash
ssh upfuzz@192.5.87.94
# password: 123
```

* After logging in, you can proceed with the next steps
* Instead of cloning git repository and switching branch again, you can pull
```bash
cd $UPFUZZ_DIR
git pull
```
* You can avoid setting up UPFUZZ_DIR again if you test through this image
* Start from setting the ORI_VERSION and the UP_VERSION


### Debug

#### Check container
If the tool runs into problems, you can enter the container to check the log.

```bash
➜  upfuzz git:(main) ✗ docker ps   # get container id
➜  upfuzz git:(main) ✗ bin/en.sh CONTAINERID
root@1c8e314a12a9:/# supervisorctl
sshd                             RUNNING   pid 8, uptime 0:01:03
upfuzz_cassandra:cassandra       RUNNING   pid 9, uptime 0:01:03
upfuzz_cassandra:cqlsh           RUNNING   pid 10, uptime 0:01:03
```

There could be several reasons (1) System starts up but the daemon in container cannot start up (2) The target system cannot start up due to configuration problem or jacoco agent instrumentation problem.

#### OOM
Check memory usage of fuzzing server
```bash
cat /proc/$(pgrep -f "upfuzz_server")/status | grep Vm
```

### JACOCO
- If jacoco jar is modified, make sure put the runtime jar into the compile/ so that it can be put into the container.
- Make sure the old jacoco jars are removed in `dependencies` folder.
- If we want to test with functions with weight, use diffchecker to generate a diff_func.txt and put it in the cassandra folder, like `Cassandra-2.2.8/diff_func.txt`.


The two jacoco jars are
* org.jacoco.core-1c01d8328d.jar
* org.jacoco.agent-1c01d8328d-runtime.jar

Use `dependencies/org.jacoco.agent-1c01d8328d-runtime.jar` to replace all `org.jacoco.agent.rt.jar`


### Speed up Cassandra start up

Cassandra by default performs ring delay and gossip wait, but we can skip them if we
only test single node
```bash
# Modify bin/cassandra, add the following code
cassandra_parms="$cassandra_parms -Dcassandra.ring_delay_ms=1 -Dcassandra.skip_wait_for_gossip_to_settle=0"
```

### Add cassandra log config for 3.0.x/2.0.x
Old version cassandra cannot use env var to adjust log dir, so we add a few scripts to handle this.
```bash
if [ -z "$CASSANDRA_LOG_DIR" ]; then
  CASSANDRA_LOG_DIR=$CASSANDRA_HOME/logs
fi

launch_service()
{
    pidpath="$1"
    foreground="$2"
    props="$3"
    class="$4"
    cassandra_parms="-Dlogback.configurationFile=logback.xml"
    cassandra_parms="$cassandra_parms -Dcassandra.logdir=$CASSANDRA_LOG_DIR"
```

### cqlsh daemon to the compatible version
* [cqlsh_daemon2_1.py](src/main/resources/cqlsh_daemon2_1.py): cassandra 2.1
* [cqlsh_daemon2.py](src/main/resources/cqlsh_daemon2.py): cassandra-2.2.8, cassandra-3.0.15/16/17, **cassandra-3.11.16**
* [cqlsh_daemon3.py](src/main/resources/cqlsh_daemon3.py): N/A
* [cqlsh_daemon4.py](src/main/resources/cqlsh_daemon4.py): 4.0.5, 4.0.12, 4.1.0, 4.1.4
* [cqlsh_daemon5.py](src/main/resources/cqlsh_daemon5.py): 5.0-beta

### hdfs daemon to version
* FsShellDaemon2.java: hadoop-2.10.2
* FsShellDaemon3.java: (> 3)
* FsShellDaemon_trunk.java: (>=3.3.4) hadoop-3.3.6
