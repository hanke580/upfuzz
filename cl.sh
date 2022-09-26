#!/bin/bash

pgrep -f config.json | xargs sudo kill -9
docker rm -f $(docker ps -a -q -f ancestor=upfuzz_cassandra:apache-cassandra-3.11.13_apache-cassandra-4.0.0-16525)

docker network prune -f
