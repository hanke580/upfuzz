#!/usr/bin/env bash

CQLSH="${CASSANDRA_HOME}/bin/cqlsh"
echo "cqlsh ${CQLSH}:${CQLSH_DAEMON_PORT}"

if ! command -v $PYTHON &>/dev/null; then
    echo "no $PYTHON available"
else
    PYTHON_VERSION=$(${PYTHON} --version)
    echo "use python: ${PYTHON} ${PYTHON_VERSION}"
fi

while true; do
    ${CQLSH} -e "describe cluster"
    if [[ "$?" -eq 0 ]]; then
        break
    fi
    sleep 1
done

${PYTHON} ${CASSANDRA_HOME}/bin/cqlsh_daemon.py
