#!/bin/bash

pgrep -f config.json | xargs sudo kill -9
docker rm -f $(docker ps -a -q -f ancestor=upfuzz_hdfs:hadoop-2.10.0_hadoop-3.1.0)

docker network prune -f
