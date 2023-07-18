export UPFUZZ_DIR=$PWD
cd $UPFUZZ_DIR/nyx_mode 
./setup_nyx_mode.sh
mkdir $UPFUZZ_DIR/nyx_mode/ubuntu
cd $UPFUZZ_DIR/nyx_mode/ubuntu
../packer/qemu_tool.sh create_image ubuntu.img $((1024*30))
wget https://releases.ubuntu.com/22.04.2/ubuntu-22.04.2-live-server-amd64.iso
sed -i.bak 's/-k de//g' ../packer/qemu_tool.sh
