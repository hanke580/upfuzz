#!/bin/bash

pgrep -f config.json | xargs sudo kill -9
docker rm -f $(docker ps -a -q -f ancestor=upfuzz_cassandra:apache-cassandra-2.1.0_apache-cassandra-3.0.17)

docker network prune -f
