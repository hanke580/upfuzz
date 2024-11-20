#!/bin/bash

OLD_VERSION=ozone-$1
NEW_VERSION=ozone-$2
pgrep -u $(id -u) -f ozone_config.json | xargs sudo kill -9
pgrep --euid $USER qemu | xargs kill -9 # kill all lurking qemu instances
docker rm -f $(docker ps -a -q -f ancestor=upfuzz_ozone:${OLD_VERSION}_${NEW_VERSION})
docker rm -f $(docker ps -a -q -f ancestor=upfuzz_ozone:ozone-${OLD_VERSION})

docker network prune -f
docker container prune -f
