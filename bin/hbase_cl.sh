#!/bin/bash

OLD_VERSION=$1
NEW_VERSION=$2
pgrep -f config.json | xargs sudo kill -9
pgrep --euid $USER qemu | xargs kill -9 # kill all lurking qemu instances
docker rm -f $(docker ps -a -q -f ancestor=upfuzz_hbase:hbase-${OLD_VERSION}_hbase-${NEW_VERSION})
docker rm -f $(docker ps -a -q -f ancestor=upfuzz_hdfs:hadoop-${OLD_VERSION})
docker rm -f $(docker ps -a -q -f ancestor=upfuzz_hdfs:hadoop-2.10.2)

docker network prune -f
docker container prune -f
