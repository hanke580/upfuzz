#!/bin/bash

OLD_VERSION=hadoop-2.10.2
NEW_VERSION=hadoop-3.3.6

pgrep -u $(id -u) -f config.json | xargs sudo kill -9
docker rm -f $(docker ps -a -q -f ancestor=upfuzz_hdfs:${OLD_VERSION}_${NEW_VERSION})

docker network prune -f
docker container prune -f
