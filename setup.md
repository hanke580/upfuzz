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
ssh-keygen -t ed25519 -C "kehan5800@gmail.com"
cat ~/.ssh/*.pub
```

Env

```bash
sudo apt-get update
sudo apt-get install openjdk-11-jdk openjdk-8-jdk -y
sh -c "$(curl -fsSL https://raw.githubusercontent.com/ohmyzsh/ohmyzsh/master/tools/install.sh)"

git config --global user.name "Ke Han"
git config --global user.email "kehan5800@gmail.com"


```

Get binaries: all binaries are stored in `in6:/data/khan/test_binary` 

```bash
# Current server
sudo mkdir /mydata/test_binary
sudo chown $USER /mydata/test_binary

# Enter indy6 server
scp -r /data/khan/test_binary/* Tingjia@c220g5-111026.wisc.cloudlab.us:/mydata/test_binary/
```

Add ssh to [github](https://github.com/settings/keys)

Set up upfuzz

```bash
cd project
git clone --recursive git@github.com:zlab-purdue/upfuzz.git
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