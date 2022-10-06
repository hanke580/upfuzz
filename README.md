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


## Usage

### Deploy Cassandra
Current we only support the testing for Cassandra.

1. Clone this repo.

2. Make sure `docker` and `docker-compose` is correctly installed.

2. Create a `config.json` file, a sample config file is provided in
   config.json.example. You need to modify the value for the target system.

```json
{
  "originalVersion" : "apache-cassandra-3.11.13",
  "upgradedVersion" : "apache-cassandra-4.0.0",
  "system" : "cassandra"
}

```

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
    ├── apache-cassandra-3.11.13
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
    └── apache-cassandra-4.0.0
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

4. [Cassandra Only] For Cassandra, you also need to copy the
   `src/main/resources/cqlsh_daemon2.py` or `src/main/resources/cqlsh_daemon3.py`
   to the `bin/` folder of the cassandra binary systems. 2 and 3 means the python
   version that the Cassandra supports. Cassandra-3.x still uses python2. You need
   to change the name to `cqlsh_daemon.py`.

E.g. If you are testing Cassandra 3.11
```bash
cp src/main/resources/cqlsh_daemon2.py prebuild/cassandra/apache-cassandra-3.11.13/bin/cqlsh_daemon.py
cp src/main/resources/cqlsh_daemon2.py prebuild/cassandra/apache-cassandra-4.0.0/bin/cqlsh_daemon.py
```

Notification: If you are upgrading from 3.x to 4.0.0, you also need to modify the
`conf/cassandra.yaml` to make sure the `num_tokens` matches.
```
vim prebuild/cassandra/apache-cassandra-4.0.0/conf/cassandra.yaml
```
Modify `num_tokens: 16` to `num_tokens: 256`. 256 is the same token number as the 3.x version systems.

5. Modify the `src/main/resources/cassandra/cassandra-3.11.13/compile-src/cassandra-clusternode.sh`
   file. You should change the `ORG_VERSION` and `UPG_VERSION` to the name of the target system.
   In this example, it would be
```bash
vim src/main/resources/cassandra/cassandra-3.11.13/compile-src/cassandra-clusternode.sh
ORG_VERSION=apache-cassandra-3.11.13
UPG_VERSION=apache-cassandra-4.0.0
```

6. Build the docker image.
```bash
cd src/main/resources/cassandra/cassandra-3.11.13/compile-src/
docker build . -t upfuzz_cassandra:apache-cassandra-3.11.13_apache-cassandra-4.0.0
```

7. Compile the project.
```bash
./gradlew copyDependencies
./gradlew build
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

8. Start testing.

open one terminal, run
```bash
java -cp "build/classes/java/main/:dependencies/*:dependencies/:build/resources/main" org/zlab/upfuzz/fuzzingengine/Main -class server -config ./config.json
```
open another terminal, run
```bash
java -cp "build/classes/java/main/:dependencies/*:dependencies/:build/resources/main" org/zlab/upfuzz/fuzzingengine/Main -class client -config ./config.json
```

### Deploy HDFS
The first 2 steps are the same. We can start from the third step.

Upgrade from hadoop-2.10.2 to hadoop-2.10.2-dup. You should replace them with the version you want to test.

3. create the `config.json` file
```json
{
  "originalVersion" : "hadoop-2.10.2",
  "upgradedVersion" : "hadoop-2.10.2-dup",
  "system" : "hdfs",
}
```

4. [HDFS] copy the hdfs daemon file to the binary folder
```shell
cp src/main/resources/hdfs_daemon3.py prebuild/hdfs/hadoop-2.10.2/bin/hdfs_daemon.py
cp src/main/resources/hdfs_daemon3.py prebuild/hdfs/hadoop-2.10.2-dup/bin/hdfs_daemon.py
```

5. Modify `bin/main/hdfs/compile-src/hdfs-clusternode.sh` file. Change the version.
```shell
vim bin/main/hdfs/compile-src/hdfs-clusternode.sh
# Change it to the target systems
ORG_VERSION=hadoop-2.10.2
UPG_VERSION=hadoop-2.10.2-dup
```

6. Build the docker image.
```shell
cd src/main/resources/hdfs/compile-src/
docker build . -t upfuzz_hdfs:hadoop-2.10.2_hadoop-2.10.2-dup
```

7. Compile the project
```shell
./gradlew copyDependencies
./gradlew :spotlessApply
./gradlew build
```

8. Start testing (Same as the command in Cassandra set up)
