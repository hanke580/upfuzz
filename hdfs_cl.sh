#!/bin/bash

pgrep -f config.json | xargs sudo kill -9
docker rm -f $(docker ps -a -q -f ancestor=upfuzz_hdfs:hadoop-3.1.3_hadoop-3.3.0)

docker network prune -f
