#!/usr/bin/env bash

CQLSH="${CASSANDRA_HOME}/bin/cqlsh"
echo "cqlsh ${CQLSH}:${CQLSH_DAEMON_PORT}"

if ! command -v $PYTHON2 &>/dev/null; then
    echo "no $PYTHON2 available"
else
    PYTHON_VERSION=$(${PYTHON2} --version)
    echo "use python: ${PYTHON2} ${PYTHON_VERSION}"
fi

while true; do
    ${CQLSH} -e "describe cluster"
    if [[ "$?" -eq 0 ]]; then
        break
    fi
    sleep 5
done

${PYTHON} ${CASSANDRA_HOME}/bin/cqlsh_daemon.py
