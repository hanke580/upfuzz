#!/bin/bash

pgrep -f config.json | xargs sudo kill -9
pgrep --euid $USER qemu | xargs kill -9 # kill all lurking qemu instances
docker rm -f $(docker ps -a -q -f ancestor=upfuzz_cassandra:apache-cassandra-3.11.14_apache-cassandra-4.1.0)

docker network prune -f
docker container prune -f
