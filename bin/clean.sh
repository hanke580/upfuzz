#!/bin/bash

# Use this script when you are the single user
# Usage: ./clean.sh [--force]
#   --force: Skip confirmation and remove containers directly

# Check if --force flag is provided
FORCE=false
if [[ "$1" == "--force" ]]; then
    FORCE=true
fi

echo "Killing upfuzz processes..."
pgrep -u $(id -u) -f '.*config\.json$' | xargs kill -9
pgrep --euid $USER qemu | xargs kill -9 # kill all lurking qemu instances

echo "Removing upfuzz containers..."
# Only remove containers created by upfuzz project
# Look for containers with upfuzz-specific naming patterns based on actual project usage
UPFUZZ_CONTAINERS=$(docker ps -a --format "table {{.Names}}\t{{.Image}}" | grep -E "(upfuzz_|cassandra-.*_.*_.*_N|hdfs-.*_.*_.*_N|hbase-.*_.*_.*_N|ozone-.*_.*_.*_N)" | awk '{print $1}')

if [ -n "$UPFUZZ_CONTAINERS" ]; then
    echo "Found upfuzz containers:"
    echo "$UPFUZZ_CONTAINERS"
    echo ""
    
    if [ "$FORCE" = true ]; then
        echo "Force mode: removing containers without confirmation..."
        echo "$UPFUZZ_CONTAINERS" | xargs -r docker rm -f
    else
        read -p "Do you want to remove these containers? (y/N): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            echo "Removing these containers..."
            echo "$UPFUZZ_CONTAINERS" | xargs -r docker rm -f
        else
            echo "Skipping container removal."
        fi
    fi
else
    echo "No upfuzz containers found."
fi

echo "Cleaning up unused Docker resources..."

docker network prune -f
docker container prune -f

echo "Cleanup completed!"
