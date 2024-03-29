#!/bin/bash

docker rm -f $(docker ps -a -q)

# docker stop $(docker ps -q)
# docker rm $(docker ps -a -q)

