#!/bin/bash

pgrep -f config.json | xargs sudo kill -9
docker rm -f $(docker ps -a -q -f ancestor=upfuzz_hdfs:hadoop-2.10.2_hadoop-2.10.2-dup)

docker network prune -f
