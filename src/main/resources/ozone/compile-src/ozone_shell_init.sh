#!/usr/bin/env bash

OZONE="$OZONE_HOME/bin/ozone"

# Get running container's IP
IP=$(hostname --ip-address | cut -f 1 -d ' ')
LAST_OCTET=$(echo $IP | awk -F. '{print $NF}')

# Current Node Status
time=0
while true; do
    proc=$(jps)
    echo "processes = $proc"
    if [[ $proc == *"OzoneManager"* || $proc == *"StorageContainerManager"* || $proc == *"HddsDatanodeService"* || $proc == *"ReconServer"* ]]; then
      echo "It's there!"
      time=$((time+1))
      echo "time = $time"
    fi
    if [[ $time -eq 4 ]]; then
      break
    fi
    sleep 5
done

# Create volume if it doesn't exist
if [[ $LAST_OCTET -eq 3 ]]; then 
    $OZONE sh volume info /volume
    if [[ "$?" -ne 0 ]]; then
        $OZONE sh volume create /volume
    fi

    # Create bucket if it doesn't exist
    $OZONE sh bucket info /volume/bucket
    if [[ "$?" -ne 0 ]]; then
        $OZONE sh bucket create /volume/bucket
    fi
fi

# Connection to OM
while true; do
    $OZONE fs -ls o3fs://bucket.volume.om/
    if [[ "$?" -eq 0 ]]; then
        echo "Successfully connected to OM!"
        break
    fi
    sleep 5
done

$OZONE fsShellDaemon
