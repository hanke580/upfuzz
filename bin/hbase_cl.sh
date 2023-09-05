#!/bin/bash

pgrep -f config.json | xargs sudo kill -9
docker rm -f $(docker ps -a -q -f ancestor=upfuzz_hbase:hbase-2.4.17_hbase-2.5.5)
docker rm -f $(docker ps -a -q -f ancestor=upfuzz_hdfs:hadoop-2.10.2)

docker network prune -f
docker container prune -f
