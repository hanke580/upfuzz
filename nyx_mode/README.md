# Upfuzz Nyx Mode Install Guide

This guide decribes how to quickly setup upfuzz with Nyx Mode. If you are instead looking for general information about nyx, read `nyx_knowledge_transfer.md`.

## System Requirements

To use nyxnet a linux system is needed and with a supported processor and kernel version.
* ubuntu 22.04+ (glibc 2.35)

### Processor

* The processor must be an x86_64 architecture with virtualization support. 

* This can quickly be checked on the machine with `egrep '^flags.*(vmx|svm)' /proc/cpuinfo`. If no output is provided your processor either is not supported or you may have virtualization disabled in your bios.

### Kernel

* The kernel must support `CONFIG_HAVE_KVM_DIRTY_RING=y`. Kernels 5.17–5.19, 6.0–6.2, 6.3-rc+HEAD seem to be supported. However our team has only confirmed DIRTY_RING is installed on the kernel version 5.19.0.

* To check your installed kernel version for support your boot configuration file can be checked for the config option above. For ubuntu similar systems the following command will only output if your kernel supports dirty ring.

    `cat /boot/config* | grep "CONFIG_HAVE_KVM_DIRTY_RING=y"`


# How to install

1. Insure you are added to the following groups: `kvm, docker`
```bash
sudo adduser $USER docker
sudo adduser $USER kvm
```
2. Follow the nyx build setup
3. Create your nyx VM

## Nyx Build setup

1. Download nyx dependencies
```bash
sudo apt-get update

sudo apt-get install -y build-essential git curl python3-dev pkg-config libglib2.0-dev libpixman-1-dev gcc-multilib flex bison pax-utils python3-msgpack python3-jinja2
```

2. Download latest rust compiler for your user
```bash
curl https://sh.rustup.rs -sSf | sh
source $HOME/.cargo/env
```

3. Navigate into the `upfuzz/nyx_mode` working directory
```bash
export UPFUZZ_DIR=$PWD
cd $UPFUZZ_DIR/nyx_mode 
```

4. Run the nyx environment setup script to setup build dependencies 

    * Warning: This may take a few minutes*

```bash
./setup_nyx_mode.sh
```

5. Now move on to create your nyx VM to fuzz on

## Creating Nyx VM

1. Navigate into the `upfuzz/nyx_mode` working directory

```bash
cd $UPFUZZ_DIR/nyx_mode 
```

2. Create a new folder where the VM image will be stored
```bash
mkdir $UPFUZZ_DIR/nyx_mode/ubuntu
cd $UPFUZZ_DIR/nyx_mode/ubuntu
```

2. Here, we will create a new VM image file (which represents the virtual hard disk of our fuzzing VM)
```bash
# create a VM image file (30GB in size)
../packer/qemu_tool.sh create_image ubuntu.img $((1024*30))
```

> PS: 
> Step 3-10 could be saved by directly using the ubuntu.image we created before
> mufasa server: /home/khan/ubuntu_install/ubuntu.img

3. Download the ubuntu server installation iso
```bash
wget https://releases.ubuntu.com/22.04.2/ubuntu-22.04.2-live-server-amd64.iso
```

4. Launch the VM through the qemu system.
```bash
../packer/qemu_tool.sh install ubuntu.img ubuntu-22.04.2-live-server-amd64.iso
```
*If you are asked to setup a VM backdoor, follow the commands given by the qemu_tool*
```bash
POST_INSTALL
[QEMU-NYX] Warning: Nyx block COW layer disabled for ubuntu.img (write operations are not cached!)
[QEMU-Nyx] Could not access KVM-PT kernel module!
[QEMU-Nyx] Trying vanilla KVM...

[QEMU-Nyx] ERROR: vmware backdoor is not enabled...

	Run the following commands to fix the issue:
	-----------------------------------------
	sudo modprobe -r kvm-intel
	sudo modprobe -r kvm
	sudo modprobe  kvm enable_vmware_backdoor=y
	sudo modprobe  kvm-intel
	cat /sys/module/kvm/parameters/enable_vmware_backdoor
	-----------------------------------------
```

5. In a new terminal connect to the VM using vnc @ `localhost:5900`. 

```bash
# port forwarding
ssh USER_NAME@IP_ADDRESS -L 9901:localhost:5900
# open vnc viewer, connect to localhost:9901
```

When connected with GUI, follow the entire ubuntu OS install process.
- Install **SSH** when asked. 
- ubuntu username (home dir): nyx
- password: nyx (Feel free to set the password to anything you like, however we suggest `nyx`).
- When the installation is finished, it prompts `cancel update and reboot`, select it.

This step will be done after ubuntu install restarts. 
- *After the restart the ubuntu system may error (`Failed unmounting /cdrom`), this is normal, `ctrl+C` the `qemu_tool.sh` script and move on to post_install mode*.

6. Now startup the VM in post install mode.
```bash
sudo ../packer/qemu_tool.sh post_install ubuntu.img
```

7. In another terminal window, navigate to the `upfuzz/nyx_mode/ubuntu` directory and use scp to transfer the nyx pre-snapshot loader. *(Replace your username used in setup below)*
```bash
cd $UPFUZZ_DIR/nyx_mode/ubuntu
scp -P 2222 ../packer/packer/linux_x86_64-userspace/bin64/loader nyx@localhost:/home/nyx/ # password: nyx
```

8. Now connect to the VM while it is still in `post_install` mode
```bash
ssh -p 2222 nyx@localhost # password: nyx
```

9. Inside the nyx VM's terminal install vmtouch
```bash
# Run this inside Nyx VM
sudo apt-get install -y vmtouch # password: nyx
```

10. Still while inside the nyx VM's terminal clone another upfuzz repo into `/home/nyx/upfuzz`. *(This is needed for docker references inside the VM)*
```bash
# Run this inside Nyx VM
cd /home/nyx

# set up ssh
ssh-keygen -t ed25519 -C "kehan5800@gmail.com"
cat ~/.ssh/id_ed25519.pub
git clone git@github.com:zlab-purdue/upfuzz.git
```

11. Inside the nyx VM follow the `Prerequsite` and `Minimal Set up for Cassandra` in README.md of upfuzz. However do **NOT** startup the upfuzz server and client inside the VM.
This is only used for setting up the folder structure. We shouldn't start up testing here.

12. After following the setup for cassandra, gracefully shutdown the nyx VM.
```bash
# Run this inside Nyx VM
sudo shutdown now
```

13. The qemu_tool.sh should close and we can begin creating the snapshot.

    Navigate to the `upfuzz/nyx_mode/ubuntu` directory and start the tool in snapshot mode with 4096 MB of memory. *(This may take a minute to startup)*
```bash
cd $UPFUZZ_DIR/nyx_mode/ubuntu
sudo ../packer/qemu_tool.sh create_snapshot ubuntu.img 4096 ./nyx_snapshot/
```

14. In another terminal window, connect to the nyx VM using VNC @ `localhost:5900`

While inside the VNC, log in and allow nyx user access to docker. It shows `nyx login:[ xxx]`.
```bash
# VNC viewer
# type: nyx
# type: nyx again, (this is the password)

# Inside Nyx VM (VNC)
sudo chmod 666 /var/run/docker.sock

# The keyboard is messy in vm:
# - => \
# z => y
```
Now we can create the root snapshot. Wait until the docker daemon has fully started (i.e. when no more messages are placed in the terminal). Then we can go ahead and create the snapshot.
```bash
# Inside Nyx VM (VNC)
chmod 777 ./loader # or chmod +x 777 ./loader
sudo ./loader
# it shows `kernel panic` and exist, this is normal
```
The nyx VM and qemu_tool.sh should close and your `./nyx_snapshot/` will contain the root snapshot.

15. Now we must configure the upfuzz to know where to find the snapshot.

Open the `../packer/packer/nyx.ini` file and point it to absolute paths of the image file and snapshot folder you just created.
```bash
# Example
cd $UPFUZZ_DIR/nyx_mode/ubuntu
vim ../packer/packer/nyx.ini
default_vm_hda = PATH_TO_FOLDER/upfuzz/nyx_mode/ubuntu/ubuntu.img
default_vm_presnapshot = .../upfuzz/nyx_mode/ubuntu/nyx_snapshot
```

16. Modify the config.ron (_upfuzz/nyx_mode/config.ron_) file's `include_default_config_path` to be an absolute path by changing `<ADD_HERE>` to your respective path.
```bash
cd $UPFUZZ_DIR/nyx_mode
vim config.ron
# replace the path with real path <ADD_HERE>
```

17. Generate default configurations

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

18. Ensure the `$UPFUZZ_DIR/config.json` is set to 
```bash
"nyxMode": true,
"testingMode": 0,
```

19. Nyx Mode should now be ready. Go on to now complete the `Minimal Set up for Cassandra` in this host environment.
When the normal build has finished, execute the following command to build nyx.
```bash
./gradlew :spotlessApply nyxBuild
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
- Make sure **config.Ron** ($UPFUZZ_DIR/nyx_mode/config.ron) has debug enabled
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





