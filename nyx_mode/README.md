# Upfuzz Nyx Mode Install Guide

This guide describes how to quickly setup upfuzz with Nyx Mode. If you are instead looking for general information about nyx, read `nyx_knowledge_transfer.md`.

## System Requirements

To use nyxnet a linux system is needed and with a supported processor and kernel version.

**OS**: ubuntu 22.04+ (glibc 2.35)

**Processor**: The processor must be an x86_64 architecture with virtualization support.
* This can quickly be checked on the machine with `egrep '^flags.*(vmx|svm)' /proc/cpuinfo`. If no output is provided your processor either is not supported or you may have virtualization disabled in your bios.

**Kernel**: Support `CONFIG_HAVE_KVM_DIRTY_RING=y`. 
* Kernels 5.17–5.19, 6.0–6.2, 6.3-rc+HEAD seem to be supported. However, our team has only confirmed DIRTY_RING is installed on the kernel version 5.19.0.
* To check your installed kernel version for support your boot configuration file can be checked for the config option above. For ubuntu similar systems the following command will only output if your kernel supports dirty ring.
    `cat /boot/config* | grep "CONFIG_HAVE_KVM_DIRTY_RING=y"`

## Nyx Build setup

1. Download nyx dependencies, add user to docker, kvm group
```bash
# Add user to docker, kvm group
sudo adduser $USER docker
sudo adduser $USER kvm
# Download dependencies
sudo apt-get update
sudo apt-get install -y build-essential git curl python3-dev pkg-config libglib2.0-dev libpixman-1-dev gcc-multilib flex bison pax-utils python3-msgpack python3-jinja2
# avoid the [QEMU-Nyx] ERROR: vmware backdoor is not enabled...
sudo modprobe -r kvm-intel
sudo modprobe -r kvm
sudo modprobe  kvm enable_vmware_backdoor=y
sudo modprobe  kvm-intel
cat /sys/module/kvm/parameters/enable_vmware_backdoor
# Install rust
curl https://sh.rustup.rs -sSf | sh
source $HOME/.cargo/env
```

2. Run the nyx environment setup script to set up build dependencies 
(Might take minutes)
```bash
export UPFUZZ_DIR=$PWD
cd $UPFUZZ_DIR/nyx_mode
./setup_nyx_mode.sh
```

## Creating Nyx VM

1. Create a new folder where the VM image will be stored
```bash
mkdir $UPFUZZ_DIR/nyx_mode/ubuntu
cd $UPFUZZ_DIR/nyx_mode/ubuntu
```

2. Download our predefined ubuntu image into `$UPFUZZ_DIR/nyx_mode/ubuntu`

```bash
rsync --progress -e ssh /home/khan/ubuntu_install/ubuntu.img Tingjia@c220g5-110915.wisc.cloudlab.us:/users/Tingjia/project/upfuzz/nyx_mode/
```
> If you do not have the pre-defined image, unfold the following instructions.
> Image location: mufasa server: /home/khan/ubuntu_install/ubuntu.img

<details>
  <summary>Click to unfold if you do not have a pre-defined image or want to test another version</summary>

### Pre-install: install os and related dependencies in VM

3.1 Create a new VM image file (which represents the virtual hard disk of our fuzzing VM)
```bash
# Create a VM image file (30GB in size)
../packer/qemu_tool.sh create_image ubuntu.img $((1024*30))
# Download the ubuntu server installation iso
wget https://releases.ubuntu.com/22.04.2/ubuntu-22.04.2-live-server-amd64.iso
# Launch the VM through the qemu system.
../packer/qemu_tool.sh install ubuntu.img ubuntu-22.04.2-live-server-amd64.iso
```

3.2 In a *new* terminal connect to the VM using **VNC** @ `localhost:5900`.

```bash
# port forwarding
ssh USER_NAME@IP_ADDRESS -L 9901:localhost:5900
# open vnc viewer, connect to localhost:9901
```

When connected with GUI, follow the entire ubuntu OS install process.
- Install **SSH** when asked.
- Set ubuntu username and password both to nyx (Feel free to set the password to anything you like, however we suggest `nyx`).
- When the installation is finished, it prompts `cancel update and reboot`, select it.

This step will be done after ubuntu install restarts.
- *After the restart the ubuntu system may error (`Failed unmounting /cdrom`), this is normal, `ctrl+C` the `qemu_tool.sh` script and move on to post_install mode*.

### Post-install: put upfuzz inside it and start the first snapshot from Nyx

3.3 Now startup the VM in post install mode.
```bash
sudo ../packer/qemu_tool.sh post_install ubuntu.img
```

3.4 In another terminal window, navigate to the `upfuzz/nyx_mode/ubuntu` directory and use scp to transfer the nyx pre-snapshot loader. *(Replace your username used in setup below)*
```bash
cd $UPFUZZ_DIR/nyx_mode/ubuntu
scp -P 2222 ../packer/packer/linux_x86_64-userspace/bin64/loader nyx@localhost:/home/nyx/ # password: nyx
```

3.5 Now connect to the VM while it is still in `post_install` mode
```bash
ssh -p 2222 nyx@localhost # password: nyx
```

3.6 Inside the nyx VM's terminal, clone another upfuzz repo into `/home/nyx/upfuzz`. *(This is needed for docker references inside the VM)*
```bash
# Run this inside Nyx VM
cd ~
# ssh-keygen -t ed25519 -C "kehan5800@gmail.com"
# cat ~/.ssh/id_ed25519.pub
ssh-keyscan github.com >> ~/.ssh/known_hosts
git clone git@github.com:zlab-purdue/upfuzz.git
cd upfuzz
bin/setup_dependency.sh
 
# test single version
bin/cass_nyx_single_test.sh
# test upgrade version
bin/cass_nyx_upgrade_test.sh
```
</details>

3. The qemu_tool.sh should close and we can begin creating the snapshot.
Navigate to the `upfuzz/nyx_mode/ubuntu` directory and start the tool in snapshot mode with 4096 MB of memory. *(This may take a minute to startup)*
```bash
cd $UPFUZZ_DIR/nyx_mode/ubuntu
sudo ../packer/qemu_tool.sh create_snapshot ubuntu.img 4096 ./nyx_snapshot/
```

4. In another terminal window, connect to the nyx VM using VNC @ `localhost:5900`

While inside the VNC, log in and allow nyx user access to docker. It shows `nyx login:[ xxx]`.
Fill both with nyx. (The keyboard is still messy, you should actually input nzx since z
equals to y)
Execute the script we previously put:
```bash
# execute inside vm using vnc
sudo bash load.sh # password: nyx but you should type nzx because of messy keyboard
```

<details>
  <summary>Click to unfold: Content of load.sh</summary>

```bash
# VNC viewer
# type: nyx
# type: nyx again, (this is the password)

# Inside Nyx VM (VNC)
sudo chmod 666 /var/run/docker.sock

# The keyboard is messy in vm:
# - => \
# z => y
# Inside Nyx VM (VNC)
# Now we can create the root snapshot. Wait until the docker daemon has fully started (i.e. when no more messages 
# are placed in the terminal). Then we can go ahead and create the snapshot.
chmod 777 ./loader # or chmod +x 777 ./loader
sudo ./loader
# it shows `kernel panic` and exist, this is normal
```
</details>

The nyx VM and qemu_tool.sh should close and your `./nyx_snapshot/` will contain the root snapshot.

5. Now we must configure the upfuzz to know where to find the snapshot.

Open the `../packer/packer/nyx.ini` file and point it to absolute paths of the image file and snapshot folder you just created. (Relative path is okay here)
```bash
cd $UPFUZZ_DIR/nyx_mode/ubuntu
vim ../packer/packer/nyx.ini
default_vm_hda = ../../../nyx_mode/ubuntu/ubuntu.img 
default_vm_presnapshot = ../../../nyx_mode/ubuntu/nyx_snapshot
```

6. Modify the config.ron (_upfuzz/nyx_mode/config.ron_) file's `include_default_config_path` to be an absolute path by changing `<ADD_HERE>` to your respective path.
```bash
cd $UPFUZZ_DIR/nyx_mode
vim config.ron
# replace the path with real path <ADD_HERE>
```

7. Generate default configurations

```bash
cd $UPFUZZ_DIR/nyx_mode/packer/packer/fuzzer_configs
mkdir tmp
python3 ../nyx_config_gen.py tmp Snapshot -m 4096 # generate default configurations
```

```
➜  fuzzer_configs git:(ef990c6) ✗ tree
.
├── default_config_vm.ron
└── tmp
    └── config.ron

1 directory, 2 files
```

9. Nyx Mode should now be ready. Go on to now complete the `Minimal Set up for Cassandra` in this host environment.
When the normal build has finished, remember execute the following command to build nyx.

**Test Single Version**
```bash
cd ${UPFUZZ_DIR}
export ORI_VERSION=3.11.15
mkdir -p ${UPFUZZ_DIR}/prebuild/cassandra
cd prebuild/cassandra
wget https://archive.apache.org/dist/cassandra/"$ORI_VERSION"/apache-cassandra-"$ORI_VERSION"-bin.tar.gz ; tar -xzvf apache-cassandra-"$ORI_VERSION"-bin.tar.gz

cd ${UPFUZZ_DIR}
cp src/main/resources/cqlsh_daemon2.py prebuild/cassandra/apache-cassandra-"$ORI_VERSION"/bin/cqlsh_daemon.py

cd ${UPFUZZ_DIR}
./gradlew copyDependencies
./gradlew :spotlessApply build

sed -i 's/"testSingleVersion": false,/"testSingleVersion": true,/g' config.json
sed -i 's/"nyxMode": false,/"nyxMode": true,/g' config.json

# open terminal1: start server
bin/start_server.sh config.json
# open terminal2: start one client
bin/start_clients.sh 1 config.json
```

**Test Upgrade**
```bash
cd ${UPFUZZ_DIR}
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
./gradlew copyDependencies
./gradlew :spotlessApply build
./gradlew :spotlessApply nyxBuild
# Ensure the `$UPFUZZ_DIR/config.json` or `$UPFUZZ_DIR/hdfs_config.json` is set to "nyxMode": true,
sed -i 's/"testSingleVersion": true,/"testSingleVersion": false,/g' config.json
sed -i 's/"nyxMode": false,/"nyxMode": true,/g' config.json

# Terminal1
bin/start_server.sh config.json 
# Terminal2
bin/start_clients.sh 1 config.json 
```

## Problem Shooting

GLIBC version: The tested GLIBC version is 2.35 (UBUNTU 22.04). 
If you encounter the following problems when running testing framework, check your glibc version. 
You better upgrade to ubuntu **22.04** which comes with glibc 2.35.

```
Exception in thread "main" java.lang.UnsatisfiedLinkError: /users/Tingjia/project/upfuzz/build/libs/libnyxJNI.so: /users/Tingjia/project/upfuzz/build/libs/libnyxJNI.so: undefined symbol: mq_setattr
	at java.base/java.lang.ClassLoader$NativeLibrary.load0(Native Method)
	at java.base/java.lang.ClassLoader$NativeLibrary.load(ClassLoader.java:2445)
	at java.base/java.lang.ClassLoader$NativeLibrary.loadLibrary(ClassLoader.java:2501)
	at java.base/java.lang.ClassLoader.loadLibrary0(ClassLoader.java:2700)
	at java.base/java.lang.ClassLoader.loadLibrary(ClassLoader.java:2630)
	at java.base/java.lang.Runtime.load0(Runtime.java:768)
	at java.base/java.lang.System.load(System.java:1837)
	at org.zlab.upfuzz.nyx.LibnyxInterface.<init>(LibnyxInterface.java:22)
	at org.zlab.upfuzz.fuzzingengine.FuzzingClient.<init>(FuzzingClient.java:57)
	at org.zlab.upfuzz.fuzzingengine.Main.main(Main.java:101)
Exception in thread "Thread-0" java.lang.NullPointerException
	at org.zlab.upfuzz.fuzzingengine.FuzzingClient.lambda$new$0(FuzzingClient.java:49)
	at java.base/java.lang.Thread.run(Thread.java:829)
```

Upgrade GLIBC from 2.31
```bash
# Check GLIBC version
ldd --version
# download GLIBC from https://ftp.gnu.org/gnu/libc/
cd glibc-2.35
mkdir build
cd build
../configure --prefix='/usr'
make
sudo make install
```

## Debug

The most common exception is `nyx instance was null` since the exception in the vm is not exposed to
user directly. (TODO: expose error message to user directly for better debugging)

```bash
java.lang.RuntimeException: Error: nyx instance was null
	at org.zlab.upfuzz.nyx.LibnyxInterface.nyxNew(Native Method)
	at org.zlab.upfuzz.nyx.LibnyxInterface.nyxNew(LibnyxInterface.java:44)
	at org.zlab.upfuzz.fuzzingengine.FuzzingClient.executeStackedTestPacketNyx(FuzzingClient.java:321)
	at org.zlab.upfuzz.fuzzingengine.FuzzingClient.executeStackedTestPacket(FuzzingClient.java:196)
	at org.zlab.upfuzz.fuzzingengine.FuzzingClientSocket.run(FuzzingClientSocket.java:62)
	at java.base/java.lang.Thread.run(Thread.java:829)
Exception in thread "Thread-1" java.lang.RuntimeException: java.lang.RuntimeException: Error: nyx instance was null
	at org.zlab.upfuzz.fuzzingengine.FuzzingClientSocket.run(FuzzingClientSocket.java:98)
	at java.base/java.lang.Thread.run(Thread.java:829)
Caused by: java.lang.RuntimeException: Error: nyx instance was null
	at org.zlab.upfuzz.nyx.LibnyxInterface.nyxNew(Native Method)
	at org.zlab.upfuzz.nyx.LibnyxInterface.nyxNew(LibnyxInterface.java:44)
	at org.zlab.upfuzz.fuzzingengine.FuzzingClient.executeStackedTestPacketNyx(FuzzingClient.java:321)
	at org.zlab.upfuzz.fuzzingengine.FuzzingClient.executeStackedTestPacket(FuzzingClient.java:196)
	at org.zlab.upfuzz.fuzzingengine.FuzzingClientSocket.run(FuzzingClientSocket.java:62)
	... 1 more
Exception in thread "Thread-0" java.lang.NullPointerException
	at org.zlab.upfuzz.fuzzingengine.FuzzingClient.lambda$new$0(FuzzingClient.java:49)
	at java.base/java.lang.Thread.run(Thread.java:829)
```

To debug, we need to
- In c agent (`src/main/c/custom_agent/nyx_agent.c`) place a sleep before the abort is called so that we can view the VM with vncviewer
  - In abort function: `int abort_operation(char* message)`
- Make sure **config.Ron** `$UPFUZZ_DIR/nyx_mode/config.ron`: debug is set to `true`.
- And use system.err prints in miniclient
- Then we can use vnc to check the snapshot of the vm. 
  - It's snapshot since once we connect to it via vnc, the vm will stop running. 


# TODOs
- A small refactor that combines the c agent + mini agent -> mini agent by using JNI. 
- Or at least creating a JNI interface for the hprintf command (so error logging can be placed outside of the VM more easily)
- Use jni inside the vm for miniclient
- Hprintf is those yellow messages in the host
- It’s the easiest way to print text to host
- So you can write debug statements and immediately see them outside the vm 


- Is *vmtouch* needed for optimizing?

