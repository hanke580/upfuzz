# upfuzz

This is a tool to test the upgrade process of distributed systems

## Usage

Current we only support the testing for Cassandra.

1. Clone this repo.

2. Create a `config.json` file, a sample config file is provided in config.json.example. You need to modify the value for the target system.

```json
{
  "originalVersion" : "apache-cassandra-3.11.13",
  "upgradedVersion" : "apache-cassandra-3.11.14",
  "system" : "cassandra"
}

```

3. Create a `prebuild` folder. Then inside `prebuild` folder, create a directory called the target system. In this case, the $SYSTEM is cassandra as the value in config.json

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
    └── apache-cassandra-3.11.14
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

4. [Cassandra Only] For Cassandra, you also need to copy the `src/main/resources/cqlsh_daemon2.py` or `src/main/resources/cqlsh_daemon3.py` to the `bin/` folder of the cassandra binary systems. 2 and 3 means the python version that the Cassandra supports. Cassandra-3.x still uses python2. You need to change the name to `cqlsh_daemon.py`.

Suppose you are testing Cassandra 3.11
```bash
cp src/main/resources/cqlsh_daemon2.py prebuild/cassandra/apache-cassandra-3.11.13/bin/cqlsh_daemon.py
cp src/main/resources/cqlsh_daemon2.py prebuild/cassandra/apache-cassandra-3.11.14/bin/cqlsh_daemon.py
```

5. Modify the `src/main/resources/cassandra/cassandra-3.11.13/compile-src/cassandra-clusternode.sh` file. You should change the `ORG_VERSION` and `UPG_VERSION` to the name of the target system. In this example, it would be
```bash
ORG_VERSION=apache-cassandra-3.11.13
UPG_VERSION=apache-cassandra-3.11.14
```

6. Build the docker image. (We currently only support the testing for Cassandra)
```bash
cd src/main/resources/cassandra/cassandra-3.11.13/compile-src/
docker build . -t upfuzz_cassandra:apache-cassandra-3.11.13_apache-cassandra-3.11.14
```

7. Compile the project
```bash
./gradlew build
./gradlew copyDependencies
```

If you have any modifications and encounter exceptions like `spotlessMiscCheck FAILED`, you may run the spotless Apply to format the code

```bash
./gradlew :spotlessApply
```

8. Start testing

open one terminal, run
```bash
java -cp "build/classes/java/main/:dependencies/*:dependencies/:build/resources/main" org/zlab/upfuzz/fuzzingengine/Main -class server -config ./config.json
```
open another terminal, run
```bash
java -cp "build/classes/java/main/:dependencies/*:dependencies/:build/resources/main" org/zlab/upfuzz/fuzzingengine/Main -class client -config ./config.json
```

Usually after ~20 attempts, the Cassandra cluster should be stable and run the test.
