#!/bin/bash

OLD_VERSION=$1
NEW_VERSION=$2
pgrep -u $(id -u) -f config.json | xargs sudo kill -9
pgrep --euid $USER qemu | xargs kill -9 # kill all lurking qemu instances
docker rm -f $(docker ps -a -q -f ancestor=upfuzz_cassandra:apache-cassandra-${OLD_VERSION}_apache-cassandra-${NEW_VERSION})
docker rm -f $(docker ps -a -q -f ancestor=upfuzz_cassandra:apache-cassandra-${OLD_VERSION})

docker network prune -f
docker container prune -f
