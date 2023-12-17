#!/bin/bash

OLD_VERSION=hadoop-$1
NEW_VERSION=hadoop-$2
pgrep -u $(id -u) -f config.json | xargs sudo kill -9
pgrep --euid $USER qemu | xargs kill -9 # kill all lurking qemu instances
docker rm -f $(docker ps -a -q -f ancestor=upfuzz_hdfs:${OLD_VERSION}_${NEW_VERSION})
docker rm -f $(docker ps -a -q -f ancestor=upfuzz_hdfs:hadoop-${OLD_VERSION})

docker network prune -f
docker container prune -f
