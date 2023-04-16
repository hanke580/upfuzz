
# Nyx_Mode Enviroment Setup

 This guide is aimed to be a knowledge transfer of what has been learned throughout the install process and how to quickly install nyxnet on a compatible machine.
*(The guide installs nyxnet to be used as a tool, not to further develop nyxnet.)*

* If you are looking to quickly startup upfuzz, please look in nyx_mode/README.md

## Note
Much of this guide is based on the original nyx-fuzz installation guide that can be found at https://github.com/nyx-fuzz/Nyx/blob/main/docs/01-Nyx-VMs.md.


## System Requirements

To use nyxnet a linux system is needed and with a supported processor and kernel version. 

### Processor

The processor must be an x86_64 architecture with virtualization support. 

This can quickly be checked on the machine with `egrep '^flags.*(vmx|svm)' /proc/cpuinfo`. If no output is provided your processor either is not supported or you may have virtualization disabled in your bios.

### Kernel

The kernel must support `CONFIG_HAVE_KVM_DIRTY_RING=y`. Kernels 5.17–5.19, 6.0–6.2, 6.3-rc+HEAD seem to be suppported. However our team has only confirmed DIRTY_RING is installed on the kernel verison 5.19.0.

To check your intalled kernel version for support your boot configuration file can be checked for the config option above. For ubuntu similar systems the following command will only output if your kernel supports dirty ring.

`cat /boot/config* | grep "CONFIG_HAVE_KVM_DIRTY_RING=y"`



### Additional Notes

This guide requires root access to install dependencies and enable the KVM backdoor. If attempting to setup nyxnet inside a VM, this guide will most likely fail due to VM specific options such as nested virtualization that must be enabled. 



## Quick install for developing fuzzers from ground up

Note this guide will require around 30GB of storage.

### Installing dependencies

1. Intall apt dependencies
```bash
sudo apt-get update
sudo apt-get install -y build-essential git curl python3-dev pkg-config libglib2.0-dev libpixman-1-dev gcc-multilib flex bison pax-utils python3-msgpack python3-jinja2
```

2. Install the latest rust compiler

```bash
curl https://sh.rustup.rs -sSf | sh
source $HOME/.cargo/env
```

### Installing nyx enviroment

1. Create a folder and navigate into it. This is where nyxnet will be installed
```bash
mkdir nyx_mode
cd nyx_mode 
```

2. Download the nyxnet repositories
```bash
git clone https://github.com/nyx-fuzz/qemu-nyx QEMU-Nyx
git clone https://github.com/nyx-fuzz/packer
git clone https://github.com/nyx-fuzz/libnyx
```

3. Intialize packer
```bash
cd packer/linux_initramfs/
sh pack.sh
cd -
```

4. Initialize and build libnyx
```bash
cd libnyx/libnyx
cargo build --release
cd -
```

5. Remove GTK3 dependency inside QEMU-Nyx
```bash
 sed -i 's/--enable-gtk//g' QEMU-Nyx/compile_qemu_nyx.sh 
```

6. Intialize QEMU-Nyx
```bash
cd QEMU-Nyx
./compile_qemu_nyx.sh static
cd -
```

7. Create a nyx.ini config file
```bash
cd packer/packer
python3 nyx_packer.py > /dev/null 
cd -
```

*Note:* If any of the nyx systems faill to initialize recheck for missing dependencies or a possible rust index directory is corrupted in which can be fixed by deleting it: `rm -r ~/.cargo/registry/index`

8. Add any users that plan on using nyxnet to the kvm group, replace the <username> below
```bash
sudo adduser <username> kvm
```
or only add yourself to the group
```bash
sudo adduser `id -un` kvm
```

9. *(Optional)* Copy libnyx shared libraries to the nyx_mode folder. This is needed for future development to access the nyxnet front end.

```bash
cp libnyx/libnyx/target/release/liblibnyx.so ./libnyx.so
cp libnyx/libnyx/libnyx.h ./libnyx.h 
```

10. *(Optional)* Copy nyxnet custom agent interface and shared library for accessing hypervisor commands from within the nyx-vm.

```bash
cp packer/nyx.h ./nyx.h 
```


Nyxnet enviroment now is installed and ready to be used.

### OS Install 

This portion of the guide is mostly taken from https://github.com/nyx-fuzz/Nyx/blob/main/docs/01-Nyx-VMs.md with some modifications to keep the install smooth.

This portion of the guide will install a nyxnet VM running `ubuntu-22.04.2-live-server-amd64`, however you can feel free to replace it with another linux instance. 

1. Navigate to your nyx_mode folder

2. Here, we will create a new VM image file (which represents the virtual hard disk of our fuzzing VM) and download the install image: 

```bash 
mkdir ubuntu
cd ubuntu

# create a VM image file (15GB in size)
../packer/qemu_tool.sh create_image ubuntu.img $((1024*15))

# download Ubuntu 22.04 LTS server image
wget https://releases.ubuntu.com/22.04.2/ubuntu-22.04.2-live-server-amd64.iso
```

3. You can use the following command to start the virtual machine and begin with the OS install. This command will also open a VNC port on port 5900. Note that this port is not limited to the loopback device. *(Note this command may ask you to open a vmware backdoor: in that case follow the instructions it gives to you and retry the following command)*

```bash
../packer/qemu_tool.sh install ubuntu.img ubuntu-22.04.2-live-server-amd64.iso
```

4. Once the VM is launched, open a new terminal instance and launch use a VNC viewer to connect and finish the OS install. I recommend using the username and password of `nyx`.

*Tip: if using SSH make sure X11 forwarding is enabled*

<details>
<summary>Dropdown: Connecting Via Tigervnc</summary>
<br>
    First install tigervnc-viewer if not already installed with: `sudo apt install tigervnc-viewer`. Then run tigervnc with: `vncviewer`. Finally when prompted input the address of `locahost` or any equivalent and connect.
<br>
<br>
</details>
<br>


5. After the OS install is complete reboot the VM and install the following packages via `apt` in the VM. Use `post_install` mode if you need to restart the VM. Additionally install OpenSSH if you did not do so during the OS install. 

```bash
sudo apt-get update
sudo apt-get install openssh-server vmtouch
```

6. Next, shutdown the VM gracefully using `sudo shutdown now` inside the VM. Then start the VM in `post_install` mode: 

```bash
../packer/qemu_tool.sh post_install ubuntu.img
```

7. The VM's port 22 (SSH) in this mode is redirected to the host's port 2222. You can use the following `scp` command to transfer files from the host into the VM. (depending on the chosen user name, the command might need to be adjusted accordingly):
```
scp -P 2222 <file_to_upload> <user>@localhost:<location_on_nyx_vm>
```
Or use the following `ssh` command to connect into the VM. (similarly to above adjust the usersname accordingly):
```
ssh -p 2222 <user>@localhost
```
8. At this time your nyx agent can be copied into the VM. You must develop the user agent binary using the c interface provided in `step 9` of `installing nyx enviroment`. If following the `01-Nyx-VMs` mentioned above, you can copy over their user agent using the following command:

*Replace `nyx` with your created user on the the VM*
```bash
scp -P 2222 packer/packer/linux_x86_64-userspace/bin64/loader nyx@localhost:/home/nyx/
```

9. Once the loader executable is copied into the VM, everything is ready at this point to create your first snapshot. You can now use SSH or VNC to shutdown the VM.



### Create a Nyx Pre-Snapshot (using nyx's provided agent/loader)

In the final step, we use the `create_snapshot` mode of the `qemu_tool.sh` utility program to create the pre-snapshot. No network devices are emulated in this mode, and write accesses to the hard disks are cached in memory and not stored in the image file (and later saved in a dedicated file as part of the snapshot). 


1. Start the VM with a RAM size of 2048 MB 
```bash
../packer/qemu_tool.sh create_snapshot ubuntu.img 2048 ./nyx_snapshot/
```

2. Connect via SSH (or VNC) and launch the agent program in the virtual machine (preferably as root):

```bash
sudo ./loader
```

3. At this point, the snapshot is created, and once that process is finished, the VM and QEMU-Nyx are automatically terminated. The snapshot folder should now contain the following files: 

```bash 
$ ls nyx_snapshot/
fast_snapshot.mem_dump fast_snapshot.qemu_state fs_drv_ubuntu.img.khash global.state ready.lock
fast_snapshot.mem_meta fs_cache.meta fs_drv_ubuntu.img.pcow INFO.txt
```

### Prepare a Nyx Fuzzer Configuration

To use our VM image and the pre-snapshot as a base image, we need to modify the `nyx.ini` file first and then generate a new configuration file. 

1. Set the following options in the `../packer/packer/nyx.ini` file and point both to the image file and snapshot folder (replace the following with your respective locations): 

```bash
default_vm_hda = .../ubuntu/ubuntu.img
default_vm_presnapshot = .../ubuntu/nyx_snapshot/
```
2.  Now we can simply generate a new configuration. Instead of the `Kernel` option, which we usually use, we chose the `Snapshot` option. Also, don't forget to specify the RAM size used (in our case 2048MB): 

```bash
python3 ../packer/packer/nyx_config_gen.py <target-shardir> Snapshot -m 2048
```

3. At this point, you can now ready to use Nyx-Net or AFL++ to start fuzzing.

### Additional Notes And Performance Considerations
OpenSSH will ease file transfer from the host to the guest. But you can also use any other tool to copy files into the virtual machine. The utility program `vmtouch` allows it to prefetch files and entire directories, such that data is not fetched from the hard disk during fuzzing. Nyx supports snapshots with `read` and `write` accesses to emulated hard disks. Still, these accesses are notoriously slow (due to device emulation) -- something you wouldn't notice in normal use cases because the data is usually cached in RAM by the kernel after the first access. If data is fetched from emulated hard disks on each iteration of the fuzzing loop, it might have an impact on the overall performance. To avoid that, you can use `vmtouch` to prefetch all essential application files before the fuzzing snapshot is created.

It is strongly recommend prefetching as much application data as possible to avoid as many IOPS as possible (remember that these will re-occur after every execution due to snapshot resets). This is especially important for huge targets such as web browsers with multiple application files (fonts, images, etc.). To do so, you can simply use `vmtouch` to prefetch data between the time the pre-snapshot is loaded and the creation of the fuzzing snapshot. Then, simply add the following line to the `fuzz.sh` script (or `fuzz_no_pt.sh` if you are running Nyx without KVM-Nyx): 
```bash
vmtouch -t <folder>
```

Another simple yet effective optimization is to avoid input writes to the file system and instead use a RAM disk for that. So, instead of configuring the packer with the following options

```bash
python3 nyx_packer.py <...> -args `/tmp/input` -file `/tmp/input`
```

we can use the `/dev/shm` directory for that. By doing so, we can gain a significant performance boost.