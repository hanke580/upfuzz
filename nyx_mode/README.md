# Upfuzz Nyx Mode Install Guide

This guide decribes how to quickly setup upfuzz with Nyx Mode. If you are instead looking for general information about nyx, read `nyx_knowledge_transfer.md`.

## System Requirements

* To use nyxnet a linux system is needed and with a supported processor and kernel version. 

### Processor

* The processor must be an x86_64 architecture with virtualization support. 

* This can quickly be checked on the machine with `egrep '^flags.*(vmx|svm)' /proc/cpuinfo`. If no output is provided your processor either is not supported or you may have virtualization disabled in your bios.

### Kernel

* The kernel must support `CONFIG_HAVE_KVM_DIRTY_RING=y`. Kernels 5.17–5.19, 6.0–6.2, 6.3-rc+HEAD seem to be supported. However our team has only confirmed DIRTY_RING is installed on the kernel version 5.19.0.

* To check your installed kernel version for support your boot configuration file can be checked for the config option above. For ubuntu similar systems the following command will only output if your kernel supports dirty ring.

    `cat /boot/config* | grep "CONFIG_HAVE_KVM_DIRTY_RING=y"`


# How to install

1. Insure you are added to the following groups: `kvm, docker`
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

    *Warning: This may take a few minutes*

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

3. Download the ubuntu server installation iso
```bash
wget https://releases.ubuntu.com/22.04.2/ubuntu-22.04.2-live-server-amd64.iso
```

4. Launch the VM through the qemu system.
```bash
../packer/qemu_tool.sh install ubuntu.img ubuntu-22.04.2-live-server-amd64.iso
```
*If you are asked to setup a VM backdoor, follow the commands given by the qemu_tool*

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
mkdir $UPFUZZ_DIR/nyx_mode/ubuntu
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

17. Ensure the `upfuzz/config.json` is set to 
```bash
"nyxMode": true,
"testingMode": 0,
```

19. Nyx Mode should now be ready. Go on to now complete the `Minimal Set up for Cassandra` in this host environment!