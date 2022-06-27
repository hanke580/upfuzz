#!/usr/bin/env bash

CQLSH="/cassandra/bin/cqlsh"

while true; do
  ${CQLSH} -e "describe cluster"
  if [[ "$?" -eq 0 ]]; then
    break
  fi
  sleep 5
done

python /cassandra/bin/cqlsh_daemon.py
