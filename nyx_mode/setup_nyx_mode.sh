#!/bin/sh
#set -e

echo "================================================="
echo "       Nyx Mode Install and Build Script"
echo "================================================="
echo
echo "Note: this script downloads and builds: QEMU-Nyx, libnyx and packer"
echo 

echo "[*] Confirming OS compatibility"

# Basic system checks
if [ "$(uname -s)" = "Linux" ]; then
    echo "[+] Confirmed linux operating system."
else
    echo "[!] Error: Nyx mode is only available on Linux."
    exit 1
fi

if [ "$(uname -m)" = "x86_64" ]; then
    echo "[+] Confirmed x86_64 architecture avaliable."
else
    echo "[!] Error: Nyx mode is only available on x86_64."
    exit 1
fi

#TODO confirm CPU capability, (kvm avaliability + dirty ring support)

echo "[*] Downloading Nyx Components"

git submodule init

#QEMU-Nyx download
echo "[*] Downloading QEMU-Nyx submodule"
git submodule update ./QEMU-Nyx 2>/dev/null
test -d QEMU-Nyx/.git || git clone https://github.com/nyx-fuzz/qemu-nyx QEMU-Nyx # if the dir doesnt exist then clone it in
if [ -e "QEMU-Nyx/.git" ]; then
    echo "[+] QEMU-Nyx Downloaded."
else
    echo "[!] Error: Unable to download QEMU-Nyx. Please install git or check your internet connection."
    exit 1
fi


#packer download
echo "[*] Downloading packer submodule"
git submodule update ./packer 2>/dev/null
test -d packer/.git || git clone https://github.com/nyx-fuzz/packer
if [ -e "packer/.git" ]; then
    echo "[+] packer Downloaded."
else
    echo "[!] Error: Unable to download packer. Please install git or check your internet connection."
    exit 1
fi

#libnyx download
echo "[*] Downloading libnyx submodule"
git submodule update ./libnyx 2>/dev/null
test -d libnyx/.git || git clone https://github.com/nyx-fuzz/libnyx
if [ -e "libnyx/.git" ]; then
    echo "[+] libnyx Downloaded."
else
    echo "[!] Error: Unable to download libnyx. Please install git or check your internet connection."
    exit 1
fi

#create the compiled hget_no_pt, hcat_no_pt, etc
cd packer/packer/linux_x86_64-userspace/
chmod +x compile_64.sh
echo "[*] Creating the necessary binary files" 
./compile_64.sh
cd -

#need to build required components
echo "[*] Building QEMU-Nyx"
if [ -f "QEMU-Nyx/x86_64-softmmu/qemu-system-x86_64" ]; then
    echo "[+] QEMU-Nyx previously built. skipping..."
else
    if ! dpkg -s gtk3-devel > /dev/null 2>&1; then # reference: https://github.com/AFLplusplus/AFLplusplus/blob/stable/nyx_mode/build_nyx_support.sh
        echo "[-] Disabling GTK because gtk3-devel is not installed."
        sed -i 's/--enable-gtk//g' QEMU-Nyx/compile_qemu_nyx.sh
    fi
    
    #comment out lines (to call hget from inside a snapshot) in handle_hypercall_kafl_req_stream_data method inside nyx_mode/QEMU-Nyx/nyx/hypercall/hypercall.c
    start_line=213
    end_line=215
    hypercall_file="QEMU-Nyx/nyx/hypercall/hypercall.c"
    
    #check if the lines are already commented out
    if grep -qE "^[[:space:]]*//.*" <(sed -n "$start_line","$end_line"p "$hypercall_file"); then
        echo "[-] The specified lines in $hypercall_file are already commented out"
    else
    #comment out the lines from start_line to end_line
        if sed -i "$start_line","$end_line"s/^/"\/\/ "/ "$hypercall_file" 2>/dev/null; then
            echo "[+] Commented out lines in $hypercall_file"
        else
            echo "[!] Error: Unable to comment out the specified lines. Check file permissions on $hypercall_file"
            exit 1
        fi
    fi

    (cd QEMU-Nyx && ./compile_qemu_nyx.sh static)
    
    if [ -f "QEMU-Nyx/x86_64-softmmu/qemu-system-x86_64" ]; then
        echo "[+] QEMU-Nyx successfully built."
    else
        echo "[!] Error: Unable to compile QEMU-Nyx"
        exit 1
    fi
fi

echo "[*] Creating packer 'nyx.ini' file"
if [ -f "packer/packer/nyx.ini" ]; then
    echo "[+] Packer 'nyx.ini' file already exists. skipping..."
else
    (cd packer/packer && python3 nyx_packer.py > /dev/null)
    if [ -f "packer/packer/nyx.ini" ]; then
        "[+] Packer 'nyx.ini' successfully created."
    else
        echo "[!] Error: Packer 'nyx.ini' could not be created"
    fi
fi

echo "[*] Building packer loader agent"
if  [ -f "packer/packer/linux_x86_64-userspace/bin64/loader" ]; then
    echo "[+] Packer loader agent already exists. skipping..."
else
    (cd packer/packer/linux_x86_64-userspace/ && make)
    if  [ -f "packer/packer/linux_x86_64-userspace/bin64/loader" ]; then
        echo "[+] Packer loader agent successfully built."
    else
        echo "[!] Error: Packer loader agent could not be built"
        exit 1
    fi
fi

# if needed to compile default bzImage
# cd packer/linux_initramfs/ 
# sh pack.sh
# cd -


echo "[*] Building libnyx Components"

if [ -f "libnyx/libnyx/target/release/liblibnyx.so" ]; then
    echo "[+] libnyx previously built. skipping..."
else
    (cd libnyx/libnyx && cargo build --release)
    if [ -f "libnyx/libnyx/target/release/liblibnyx.so" ]; then
        echo "[+] libnyx successfully built."
    else
        echo "[!] Error: Unable to build libnyx"
        exit 1
    fi
fi

echo "[*] Copying libnyx.a"
if ! cp "libnyx/libnyx/target/release/liblibnyx.a" "libnyx.a"; then
    echo "[!] Error: cp failed"
    exit 1
fi

echo "[*] Copying libnyx.h"
if ! cp "libnyx/libnyx/libnyx.h" "libnyx.h"; then
    echo "[!] Error: cp failed"
    exit 1
fi

echo "[*] Copying nyx.h"
if ! cp "packer/nyx.h" "nyx.h"; then
    echo "[!] Error: cp failed"
    exit 1
fi


echo "[+] Nyx_mode setup complete!"


