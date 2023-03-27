# Config Test
Config test is disabled by default.

If you want to test configuration, checkout TEST_CONFIG.md.

> You don't to this step if (1) the config info has been generated in `configInfo` folder or (2) opt not to test config temporarily.
>
> The config for example upgrade has been generated already.

We implemented a static analysis tool [diffchecker](https://github.com/hanke580/diffchecker) to detect configurations that are likely to induce upgrade failures.

We need to use diffchecker to find all such configurations and then feed them to upfuzz so that upfuzz can generate them at runtime.

#### Get configurations

In diffchecker repo

1. Clone and build diffchecker, this requires JDK11
```bash
mvn -T 2C -e clean package
```

2. Create a folder called `symbolic_link`
```bash
mkdir src/main/resources/symbolic_link
```

3. Clone the source code of the target system by git, create a symbolic link in the folder we just created.

Example
```bash
# clone the system source code somewhere and make a sym link
cd ~
git clone --recursive https://github.com/apache/cassandra.git ~/cassandra_old
git clone --recursive https://github.com/apache/cassandra.git ~/cassandra_new

cd ${DIFFCHECKER}/src/main/resources/symbolic_link
ln -s ~/cassandra_old
ln -s ~/cassandra_new
```

4. Run the diffcheck to get the target configurations
Example (Cassandra)
```bash
# collect newly added configurations
java -jar target/diffchecker-1.0-SNAPSHOT-shaded.jar -p cassandra_old -np cassandra_new --type config --action modified
# collect common configurations (deprecated or contain size)
java -jar target/diffchecker-1.0-SNAPSHOT-shaded.jar -p cassandra_old -np cassandra_new --type config --action common --configpath conf
```

This will generate configurations and their type in json (the above two commands generate different json files and do not overwrite each other's result). **Later we will move these files to the upfuzz repo, under upfuzz/configInfo/OLDSYSTEM_NEWSYSTEM/**

```bash
➜  diffchecker git:(main) ✗ ls configInfo
addedClassConfig2Init.json  addedClassConfig.json         addedSysConfig.json     commonConfig2Type.json  commonEnum2Constant.json
addedClassConfig2Type.json  addedClassEnum2Constant.json  commonConfig2Init.json  commonConfig.json
```




Create a configInfo directory for store the configurations.
```bash
mkdir configInfo
mkdir configInfo/${originalVersion}_${upgradedVersion}
# In the example it would be
mkdir configInfo/apache-cassandra-3.11.14_apache-cassandra-4.1.0
mv ${diffchecker}/configInfo/* configInfo/apache-cassandra-3.11.14_apache-cassandra-4.1.0/
```