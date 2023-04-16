#!/bin/sh
########
# Fuzzing startup script to be called first in the nyx VM when nyx_new is called.
#######

echo "====================================================="
echo " WARNING IF RAN OUTSIDE NYX_VM BAD STUFF MAY OCCUR  "
echo "====================================================="


#get hget script - downloading from sharedir
chmod +x ./hget
#get hypervisor printing script
./hget "hcat_no_pt" "hcat"
chmod +x ./hcat

# get c_agent + chmod
./hget "c_agent" "/home/nyx/upfuzz/build/libs/c_agent"
chmod +x /home/nyx/upfuzz/build/libs/c_agent

# get miniclient + chmod
./hget "MiniClient.jar" "/home/nyx/upfuzz/MiniClient.jar"
chmod +x /home/nyx/upfuzz/MiniClient.jar

# setup mini agent workspace
mkdir -p "/miniClientWorkdir/stackedTestConfigFile"

./hget "stackedTestPackets/defaultStackedPacket.ser" "/miniClientWorkdir/defaultStackedTestPacket.ser"
./hget "archive.tar.gz" "stackedTestConfigFile"
tar -xzf stackedTestConfigFile -C /miniClientWorkdir/stackedTestConfigFile

# start the c agent
cd /home/nyx/upfuzz
/home/nyx/upfuzz/build/libs/c_agent