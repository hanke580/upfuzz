UPFUZZ_DIR="$1"
cd $UPFUZZ_DIR/nyx_mode 
./setup_nyx_mode.sh
ubuntu_dir="$UPFUZZ_DIR/nyx_mode/ubuntu"
if [ ! -d "$ubuntu_dir" ]; then
  mkdir "$ubuntu_dir"
fi
cd "$ubuntu_dir"
../packer/qemu_tool.sh create_image ubuntu.img $((1024*30))
wget https://releases.ubuntu.com/22.04.2/ubuntu-22.04.2-live-server-amd64.iso
sed -i.bak 's/-k de//g' ../packer/qemu_tool.sh
../packer/qemu_tool.sh install ubuntu.img ubuntu-22.04.2-live-server-amd64.iso