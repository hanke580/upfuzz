# upfuzz

> A tool to detect upgrade bugs of distributed systems

## Feature
* Coverage-guided structural fuzz testing
    * upfuzz collect the code coverage of the cluster to guide the testing
    process
    * upfuzz implements a type system for mutation. Users only need to
      implement their command via the given types, and upfuzz can
      generate/mutate valid command sequence.

* Fault Injection
    * upfuzz also inject faults during the upgrade process.

* Inconsistency Detector
    * upfuzz use inconsistency detector to extract a list of system states and
    compare the value between (1) rolling upgrade and (2) full-stop upgrade to
      detect transient bugs

* Configuration Generator
    * upfuzz utilizes static analysis to focus on testing a set of configurations

## Architecture

The testing framework is a  **one server, multiple clients** structure. Each
client retrieves a test packet from the server and execute it.
The client process starts up a cluster which contains multiple docker containers,
and then sends command sequences to the system instance inside the docker via
network packet.

To test the upgrade process of a specific system, you need to deploy it into the
testing framework. There are some system-specific parts like (1) System related
shell commands and (2) How does the system start up, upgrade and shutdown.

There is an interface `Executor`. To deploy a distributed system in the testing
framework, you need to implement this interface.

The testing framework also provides a frontend for users to implement the
user/admin level commands with constraints. It tries to make the fuzzer generate
the syntax valid commands so that we can test the deep logic of the stateful
systems.

### Example: Cassandra

The client process runs in the physical machine. Inside the docker, there is a
Cassandra cqlsh daemon running. The goal of the cqlsh daemon is to avoid the
initialization cost for cqlsh.
* Without the cqlsh daemon, each time we issue a single command, it needs to
  initialize a process. This means that if our test case contains 20 commands,
  there will be 20 times the cost for this initialization.
* With the cqlsh daemon, we only have one initialization process for a single
  Cassandra instance.

The client in the physical machine communicates with the cqlsh daemon via socket.

Multiple processes are started and controlled by `supervisord`. When the
Cassandra instance in the container needs to do an upgrade, `FuzzingClient`
will use `docker exec` to issue a supervisorctl restart command to the docker
for the upgrade process.


## Prerequsite
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


## Minimal Set up for Cassandra (Try upfuzz quickly!)
Requirement: java11, docker (Docker version 23.0.1, build a5ee5b1)
> - Not test configurations.
> - single Cassandra node upgrade: 3.11.15 => 4.1.2

Clone the repo
```bash
git clone --recursive git@github.com:zlab-purdue/upfuzz.git
cd upfuzz
export UPFUZZ_DIR=$PWD
export ORI_VERSION=3.11.15
export UP_VERSION=4.1.2

mkdir -p "$UPFUZZ_DIR"/prebuild/cassandra
cd prebuild/cassandra
wget https://archive.apache.org/dist/cassandra/"$ORI_VERSION"/apache-cassandra-"$ORI_VERSION"-bin.tar.gz ; tar -xzvf apache-cassandra-"$ORI_VERSION"-bin.tar.gz
wget https://archive.apache.org/dist/cassandra/"$UP_VERSION"/apache-cassandra-"$UP_VERSION"-bin.tar.gz ; tar -xzvf apache-cassandra-"$UP_VERSION"-bin.tar.gz

sed -i 's/num_tokens: 16/num_tokens: 256/' apache-cassandra-"$UP_VERSION"/conf/cassandra.yaml

cd ${UPFUZZ_DIR}
cp src/main/resources/cqlsh_daemon2.py prebuild/cassandra/apache-cassandra-"$ORI_VERSION"/bin/cqlsh_daemon.py
cp src/main/resources/cqlsh_daemon3_4.0.5_4.1.0.py  prebuild/cassandra/apache-cassandra-"$UP_VERSION"/bin/cqlsh_daemon.py

cd src/main/resources/cassandra/normal/compile-src/
docker build . -t upfuzz_cassandra:apache-cassandra-"$ORI_VERSION"_apache-cassandra-"$UP_VERSION"

cd ${UPFUZZ_DIR}
./gradlew copyDependencies
./gradlew :spotlessApply build

# open terminal1: start server
./start_server.sh config.json

# open terminal2: start one client
./start_clients.sh 1 config.json

# stop testing:
./cass_cl.sh
```

## Usage

Important configurations
- **testingMode**
  - 0: Full-Stop testing
  - 1: Rolling upgrade testing
  - 4: Mixed full-stop&Rolling upgrade testing

> Config test is disabled by default.
>
> If you want to test configuration, checkout **TEST_CONFIG.md**.

### Deploy Cassandra

1. Clone upfuzz repo.

Make sure `docker` is correctly installed. (the latest docker has `docker compose` installed already)
```bash
git clone --recursive git@github.com:zlab-purdue/upfuzz.git
cd upfuzz

➜  upfuzz git:(main) docker -v
Docker version 23.0.1, build a5ee5b1
```

2. Create a `config.json` file, a sample config file is provided in
   config.json. You need to modify the value for the target system. 
More configurations please refer to `Config.java` file.


3. Create a `prebuild` folder. Then inside `prebuild` folder, create a directory
   called the target system. In this case, the $SYSTEM is cassandra as the value
   in config.json

```bash
$ mkdir prebuild
$ mkdir prebuild/$SYSTEM
```

The binary of new version and the old version should be placed in `prebuild/$SYSTEM`.

```bash
prebuild
└── cassandra
    ├── apache-cassandra-3.11.14
    │   ├── bin
    │   ├── CASSANDRA-14092.txt
    │   ├── CHANGES.txt
    │   ├── conf
    │   ├── doc
    │   ├── interface
    │   ├── lib
    │   ├── LICENSE.txt
    │   ├── NEWS.txt
    │   ├── NOTICE.txt
    │   ├── pylib
    │   └── tools
    └── apache-cassandra-4.1.0
        ├── bin
        ├── CASSANDRA-14092.txt
        ├── CHANGES.txt
        ├── conf
        ├── doc
        ├── interface
        ├── lib
        ├── LICENSE.txt
        ├── NEWS.txt
        ├── NOTICE.txt
        ├── pylib
        └── tools
```

4. Copy the shell daemon file. For Cassandra, copy
   `src/main/resources/cqlsh_daemon2.py` or `src/main/resources/cqlsh_daemon3.py`
   to the `bin/` folder of the cassandra binary systems. 2 and 3 means the python
   version that the Cassandra supports. Cassandra-3.x still uses python2. You need
   to **change the name** to `cqlsh_daemon.py`.

E.g. If you are testing Cassandra 3.11 (pwd=/path/to/upfuzz/)
```bash
cp src/main/resources/cqlsh_daemon2.py prebuild/cassandra/apache-cassandra-3.11.14/bin/cqlsh_daemon.py
cp src/main/resources/cqlsh_daemon3_4.0.5_4.1.0.py  prebuild/cassandra/apache-cassandra-4.1.0/bin/cqlsh_daemon.py
```

> Cassandra document notification: If you are upgrading from 3.x to 4.x, you also need to modify the `conf/cassandra.yaml` to make sure the `num_tokens` matches. Since the default `num_tokens` for the 3.x is 256, while for 4.0 it's 16.

```bash
# Change configuration:
# num_tokens: 256 => num_tokens: 16
sed -i 's/num_tokens: 16/num_tokens: 256/' prebuild/cassandra/apache-cassandra-4.1.0/conf/cassandra.yaml

```

6. Build the docker image.

First, modify the first few lines in `src/main/resources/cassandra/normal/compile-src/cassandra-clusternode.sh` file. You should change the `ORG_VERSION` and `UPG_VERSION` to the name of the target system. In this example,
```bash
vim src/main/resources/cassandra/normal/compile-src/cassandra-clusternode.sh
ORG_VERSION=apache-cassandra-3.11.14
UPG_VERSION=apache-cassandra-4.1.0
```

Then build the docker image (pwd=/path/to/upfuzz/)
```bash
cd src/main/resources/cassandra/normal/compile-src/
docker build . -t upfuzz_cassandra:apache-cassandra-3.11.14_apache-cassandra-4.1.0
```

7. Compile the project. (pwd=/path/to/upfuzz/)
```bash
./gradlew copyDependencies
./gradlew :spotlessApply build
```

`gradlew copyDependencies` will copy all the jars into folder `upfuzz/dependencies`. Remember
to check whether you are using the correct Jacoco lib, there should be a file called
`org.jacoco.core-4e5168b9b5.jar`, if the name does not match, it means that you are using an
incorrect version of Jacoco. Remove all the jacoco related jar in this folder and redo the
copyDependencies.

If you have any modifications and encounter exceptions like `spotlessMiscCheck FAILED`, you
may run the spotless Apply to format the code

```bash
./gradlew :spotlessApply
```

8. Start testing. (pwd=/path/to/upfuzz/)

There are two scripts `start_server.sh` and `start_client.sh`. You can start up clients with this script.

start up a server
```bash
./start_server.sh config.json
```

start up N clients (replace N with a number)
```bash
./start_clients.sh N config.json
```

9. Stop testing

Checkout `cass_cl.sh`, this file contains how to kill the server/client process and all the containers.


## Minimal Set up for HDFS (Try upfuzz quickly!)
Requirement: jdk8, jdk11, docker (Docker version 23.0.1, build a5ee5b1)
> - Not test configurations.
> - 4 Nodes upgrade (NN, SNN, 2DN): 2.10.2 => 3.3.5

```bash
git clone --recursive git@github.com:zlab-purdue/upfuzz.git
cd upfuzz
export UPFUZZ_DIR=$PWD
export ORI_VERSION=2.10.2
export UP_VERSION=3.3.5

mkdir -p $UPFUZZ_DIR/prebuild/hdfs
cd $UPFUZZ_DIR/prebuild/hdfs
wget https://archive.apache.org/dist/hadoop/common/hadoop-"$ORI_VERSION"/hadoop-"$ORI_VERSION".tar.gz ; tar -xzvf hadoop-$ORI_VERSION.tar.gz
wget https://archive.apache.org/dist/hadoop/common/hadoop-"$UP_VERSION"/hadoop-"$UP_VERSION".tar.gz ; tar -xzvf hadoop-"$UP_VERSION".tar.gz

# Switch java/javac to jdk8
sudo update-alternatives --config java
sudo update-alternatives --config javac

# old version hdfs daemon
cp $UPFUZZ_DIR/src/main/resources/FsShellDaemon2.java $UPFUZZ_DIR/prebuild/hdfs/hadoop-"$ORI_VERSION"/FsShellDaemon.java
cd $UPFUZZ_DIR/prebuild/hdfs/hadoop-"$ORI_VERSION"/
javac -d . -cp "share/hadoop/hdfs/hadoop-hdfs-"$ORI_VERSION".jar:share/hadoop/common/hadoop-common-"$ORI_VERSION".jar:share/hadoop/common/lib/*" FsShellDaemon.java
sed -i "s/elif \[ \"\$COMMAND\" = \"dfs\" \] ; then/elif [ \"\$COMMAND\" = \"dfsdaemon\" ] ; then\n  CLASS=org.apache.hadoop.fs.FsShellDaemon\n  HADOOP_OPTS=\"\$HADOOP_OPTS \$HADOOP_CLIENT_OPTS\"\n&/" bin/hdfs

# new version hdfs daemon
cp $UPFUZZ_DIR/src/main/resources/FsShellDaemon_trunk.java $UPFUZZ_DIR/prebuild/hdfs/hadoop-"$UP_VERSION"/FsShellDaemon.java
cd $UPFUZZ_DIR/prebuild/hdfs/hadoop-"$UP_VERSION"/
javac -d . -cp "share/hadoop/hdfs/hadoop-hdfs-"$UP_VERSION".jar:share/hadoop/common/hadoop-common-"$UP_VERSION".jar:share/hadoop/common/lib/*" FsShellDaemon.java
sed -i "s/  case \${subcmd} in/&\n    dfsdaemon)\n      HADOOP_CLASSNAME=\"org.apache.hadoop.fs.FsShellDaemon\"\n    ;;/" bin/hdfs

cd $UPFUZZ_DIR/src/main/resources/hdfs/compile-src/
docker build . -t upfuzz_hdfs:hadoop-"$ORI_VERSION"_hadoop-"$UP_VERSION"

# Switch java/javac to jdk11
sudo update-alternatives --config java
sudo update-alternatives --config javac

cd $UPFUZZ_DIR
./gradlew copyDependencies
./gradlew :spotlessApply build

# open terminal1: start server
./start_server.sh hdfs_config.json

# open terminal2: start one client
./start_clients.sh 1 hdfs_config.json

# stop testing:
./hdfs_cl.sh
```

### Deploy HDFS
The first 3 steps are the same. We can start from the fourth step.

Upgrade from hadoop-2.10.2 to hadoop-3.3.0. You should replace them with the version you want to test.

3. Modify the `config.json` file.

4. Compile the daemon file

> FsShellDaemon_trunk.java is for Hadoop trunk version (>= 3.3.4)
>
> FsShellDaemon3.java is for Hadoop > 3.x
>
> FsShellDaemon2.java is for Hadoop > 2.8.5
>
> The exact suitable version might still change.

```bash
cp src/main/resources/FsShellDaemon3.java prebuild/hdfs/hadoop-3.3.0/FsShellDaemon.java
cd prebuild/hdfs/hadoop-3.3.0/
javac -d . -cp "share/hadoop/hdfs/hadoop-hdfs-3.3.0.jar:share/hadoop/common/hadoop-common-3.3.0.jar:share/hadoop/common/lib/*" FsShellDaemon.java
sed -i "s/  case \${subcmd} in/&\n    dfsdaemon)\n      HADOOP_CLASSNAME=\"org.apache.hadoop.fs.FsShellDaemon\"\n    ;;/" bin/hdfs
-----
hdfs 2.x
sed -i "s/elif \[ \"\$COMMAND\" = \"dfs\" \] ; then/elif [ \"\$COMMAND\" = \"dfsdaemon\" ] ; then\n  CLASS=org.apache.hadoop.fs.FsShellDaemon\n  HADOOP_OPTS=\"\$HADOOP_OPTS \$HADOOP_CLIENT_OPTS\"\n&/" bin/hdfs
```

5. Modify `bin/main/hdfs/compile-src/hdfs-clusternode.sh` file. Change the version.
```shell
vim bin/main/hdfs/compile-src/hdfs-clusternode.sh
# Change it to the target systems
ORG_VERSION=hadoop-2.10.2
UPG_VERSION=hadoop-3.3.0
```

6. Build the docker image.
```shell
cd src/main/resources/hdfs/compile-src/
docker build . -t upfuzz_hdfs:hadoop-2.10.2_hadoop-3.3.0
```

7. Compile the project
```shell
./gradlew copyDependencies
./gradlew :spotlessApply build
```

8. Start testing (Same as the command in Cassandra set up)


### Debug related

If the tool runs into problems, you can enter the container to check the log.

```bash
➜  upfuzz git:(main) ✗ docker ps   # get container id
➜  upfuzz git:(main) ✗ ./en.sh CONTAINERID
root@1c8e314a12a9:/# supervisorctl
sshd                             RUNNING   pid 8, uptime 0:01:03
upfuzz_cassandra:cassandra       RUNNING   pid 9, uptime 0:01:03
upfuzz_cassandra:cqlsh           RUNNING   pid 10, uptime 0:01:03
```

There could be several reasons (1) System starts up but the daemon in container cannot start up (2) The target system cannot start up due to configuration problem or jacoco agent instrumentation problem.


### Notes
- If jacoco jar is modified, make sure put the runtime jar into the compile/ so that it can be put into the container.
- If we want to test with functions with weight, use diffchecker to generate a diff_func.txt and put it in the cassandra folder, like `Cassandra-2.2.8/diff_func.txt`.
