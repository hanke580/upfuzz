#!/bin/sh
########
# Note this is only meant to run inside the nyx VM
#######

echo "====================================================="
echo " WARNING THIS MAY DO BAD STUFF IF RAN OUTSIDE NYX_VM"
echo "====================================================="

mkdir "/miniClientWorkdir"

hget "stackedTestPackets/defaultStackedPacket.ser" "/miniClientWorkdir/defaultStackedTestPacket.ser"
hget "sharedConfigFile" "/miniClientWorkdir/stackedTestConfigFile"

/home/nyx/upfuzz/build/libs/c_agent