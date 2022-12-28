# Env set up for upfuzz

Mount SSD

```bash
mkdir project
sudo fdisk -l
sudo mkfs.ext4 /dev/sda4
sudo mount /dev/sda4 project/
sudo chown $USER project/
```

Generate ssh key

```bash
ssh-keygen -t ed25519 -P '' -f ~/.ssh/ed25519  -C "kehan5800@gmail.com"
cat ~/.ssh/ed25519.pub >> ~/.ssh/authorized_keys
cat ~/.ssh/ed25519.pub
```

Env

```bash
sudo apt-get update
sudo apt-get install openjdk-11-jdk openjdk-8-jdk -y
sh -c "$(curl -fsSL https://raw.githubusercontent.com/ohmyzsh/ohmyzsh/master/tools/install.sh)"

git config --global user.name "Ke Han"
git config --global user.email "kehan5800@gmail.com"
git config --global core.editor "vim"

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
sudo apt-get install docker-ce docker-ce-cli containerd.io docker-compose-plugin

sudo usermod -aG docker $USER
newgrp docker
```

Get binaries: all binaries are stored in `in6:/data/khan/test_binary`

```bash
# Current server
sudo mkdir /mydata/test_binary
sudo chown $USER /mydata/test_binary

# Enter indy6 server
scp -r /data/khan/test_binary/* Tingjia@c220g5-111026.wisc.cloudlab.us:/mydata/test_binary/
scp -r /data/khan/test_binary/* Tingjia@c220g5-xxxxxx.wisc.cloudlab.us:/mydata/test_binary/
```

Add ssh to [github](https://github.com/settings/keys)

Set up upfuzz

```bash
cd project
git clone --recursive git@github.com:zlab-purdue/upfuzz.git
git checkout -b server-client origin/server-client

cd upfuzz
./gradlew copyDependencies
./gradlew :spotlessApply build

mkdir -p ~/project/upfuzz/prebuild/hdfs
mkdir -p ~/project/upfuzz/prebuild/cassandra

cd ~/project/upfuzz/prebuild/hdfs
tar -xzvf /mydata/test_binary/hdfs/hadoop-3.4.0_trunk.tar.gz

cd ~/project/upfuzz/prebuild/cassandra
tar -xzvf /mydata/test_binary/cassandra/apache-cassandra-4.0-bin.tar.gz
```

Cassandra Daemon

```bash
cp src/main/resources/cqlsh_daemon2.py prebuild/cassandra/apache-cassandra-3.11.13/bin/cqlsh_daemon.py
cp src/main/resources/cqlsh_daemon2.py prebuild/cassandra/apache-cassandra-4.0.0/bin/cqlsh_daemon.py
```

HDFS Daemon

```bash
cp src/main/resources/FsShellDaemon3.java prebuild/hdfs/hadoop-3.3.0/FsShellDaemon.java
cd prebuild/hdfs/hadoop-3.3.0/

javac -d . -cp "share/hadoop/hdfs/hadoop-hdfs-2.10.2.jar:share/hadoop/common/hadoop-common-2.10.2.jar:share/hadoop/common/lib/*" FsShellDaemon.java

# hdfs 3.x
sed -i "s/  case \${subcmd} in/&\n    dfsdaemon)\n      HADOOP_CLASSNAME=\"org.apache.hadoop.fs.FsShellDaemon\"\n    ;;/" bin/hdfs
# ---
# hdfs 2.x
sed -i "s/elif \[ \"\$COMMAND\" = \"dfs\" \] ; then/elif [ \"\$COMMAND\" = \"dfsdaemon\" ] ; then\n  CLASS=org.apache.hadoop.fs.FsShellDaemon\n  HADOOP_OPTS=\"\$HADOOP_OPTS \$HADOOP_CLIENT_OPTS\"\n&/" bin/hdfs
```

Build image

```bash
# CASSANDRA
cd src/main/resources/cassandra/cassandra-3.11.13/compile-src/
docker build . -t upfuzz_cassandra:apache-cassandra-3.11.13_apache-cassandra-4.0.0

# HDFS
cd src/main/resources/hdfs/compile-src/
docker build . -t upfuzz_hdfs:hadoop-2.10.2_hadoop-3.3.0
```
