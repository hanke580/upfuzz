#!/bin/bash

pgrep -u $(id -u) -f '.*config\.json$' | xargs kill -9
pgrep --euid $USER qemu | xargs kill -9 # kill all lurking qemu instances

docker rm -f $(docker ps -a -q)
# docker stop $(docker ps -q)
# docker rm $(docker ps -a -q)


docker network prune -f
docker container prune -f
