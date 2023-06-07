#!/bin/bash

pgrep -f config.json | xargs sudo kill -9
docker rm -f $(docker ps -a -q -f ancestor=upfuzz_cassandra:apache-cassandra-3.11.15_apache-cassandra-4.1.2)

docker network prune -f
docker container prune -f
