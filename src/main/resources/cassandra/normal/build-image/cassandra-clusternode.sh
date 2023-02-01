#!/usr/bin/env bash

# Get running container's IP
IP=`hostname --ip-address | cut -f 1 -d ' '`
if [ $# == 1 ]; then SEEDS="$1,$IP"; 
else SEEDS="$IP"; fi

# Setup cluster name
if [ -z "$CASSANDRA_CLUSTER_NAME" ]; then
        echo "No cluster name specified, preserving default one" 
else
        sed -i -e "s/^cluster_name:.*/cluster_name: $CASSANDRA_CLUSTER_NAME/" $CASSANDRA_CONFIG/cassandra.yaml
fi

echo "Before starting Cassandra on $IP... Config dir $CASSANDRA_CONFIG" > /tmp.log
grep address $CASSANDRA_CONFIG/cassandra.yaml >> /tmp.log 2>&1 

# Dunno why zeroes here
sed -i -e "s/^rpc_address.*/rpc_address: $IP/" $CASSANDRA_CONFIG/cassandra.yaml >>  /tmp.log 2>&1 

# Listen on IP:port of the container
sed -i -e "s/^listen_address.*/listen_address: $IP/" $CASSANDRA_CONFIG/cassandra.yaml >>  /tmp.log 2>&1 

# Change the logging level accordingly
if [ -z "$CASSANDRA_LOGGING_LEVEL" ]; then
        echo "No log level specified, preserving default INFO" 
else
        sed -i -e "s/^log4j.rootLogger=.*/log4j.rootLogger=$CASSANDRA_LOGGING_LEVEL,stdout,R/" $CASSANDRA_CONFIG/log4j-server.properties >>  /tmp.log 2>&1 
fi

echo "after" >>  /tmp.log
grep address $CASSANDRA_CONFIG/cassandra.yaml >>  /tmp.log 2>&1 

# Configure Cassandra seeds
if [ -z "$CASSANDRA_SEEDS" ]; then
	echo "No seeds specified, being my own seed..."
	CASSANDRA_SEEDS=$SEEDS
fi
sed -i -e "s/- seeds: \"127.0.0.1\"/- seeds: \"$CASSANDRA_SEEDS\"/" $CASSANDRA_CONFIG/cassandra.yaml

# With virtual nodes disabled, we need to manually specify the token
# Not needed for Cassandra 0.8
#if [ -z "$CASSANDRA_TOKEN" ]; then
#	echo "Missing initial token for Cassandra"
#	exit -1
#fi
#echo "JVM_OPTS=\"\$JVM_OPTS -Dcassandra.initial_token=$CASSANDRA_TOKEN\"" >> $CASSANDRA_CONFIG/cassandra-env.sh

# Most likely not needed
#echo "JVM_OPTS=\"\$JVM_OPTS -Djava.rmi.server.hostname=$IP\"" >> $CASSANDRA_CONFIG/cassandra-env.sh

echo "Starting Cassandra on $IP..."
echo "Starting Cassandra on $IP... Config dir $CASSANDRA_CONFIG" >>  /tmp.log

#exec /cassandra/bin/cassandra -fR
exec /cassandra/bin/cassandra -R
# use R so that Cassandra can be run as root
