#!/bin/bash

pgrep -f config.json | xargs sudo kill -9
docker rm -f $(docker ps -a -q -f ancestor=upfuzz_hdfs:hadoop-2.10.2_hadoop-3.4.0_trunk)

docker network prune -f
docker container prune -f
