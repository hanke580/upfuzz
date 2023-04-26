# Likely Invariants for Upgrade Process

## Command for invariants

Inside container

```bash

# generate inv from data trace
java -cp /daikon.jar daikon.Daikon CassandraDaemon.dtrace.gz


# Check whether any invariants are broken given a new data trace and an inv file.
java -cp /daikon.jar daikon.tools.InvariantChecker --verbose --output /broken_inv /targetInv.inv.gz /CassandraDaemon.dtrace.gz
java -cp /daikon.jar daikon.tools.InvariantChecker --verbose --output /broken_inv /CassandraDaemon.inv.gz /CassandraDaemon.dtrace.gz
```

Host
```bash
# generate inv from data trace
java -cp $DAIKONDIR/daikon.jar daikon.Daikon CassandraDaemon.dtrace.gz

# Check broken invariants
java -cp $DAIKONDIR/daikon.jar daikon.tools.InvariantChecker --verbose --output tmp CassandraDaemon.inv.gz CassandraDaemon.dtrace.gz
```

## Cassandra with Daikon's instrumentation

```bash
# Modify launch_service() function in bin/cassandra
launch_service()
{
    pidpath="$1"
    foreground="$2"
    props="$3"
    class="$4"
    cassandra_parms="-Dlogback.configurationFile=logback.xml"
    cassandra_parms="$cassandra_parms -Dcassandra.logdir=$CASSANDRA_LOG_DIR"
    cassandra_parms="$cassandra_parms -Dcassandra.storagedir=$cassandra_storagedir"

    if [ "x$pidpath" != "x" ]; then
        cassandra_parms="$cassandra_parms -Dcassandra-pidfile=$pidpath"
    fi

    # The cassandra-foreground option will tell CassandraDaemon not
    # to close stdout/stderr, but it's up to us not to background.
    if [ "x$foreground" != "x" ]; then
        cassandra_parms="$cassandra_parms -Dcassandra-foreground=yes"
        if [ "x$JVM_ON_OUT_OF_MEMORY_ERROR_OPT" != "x" ]; then
            exec $NUMACTL "$JAVA" -cp "$CLASSPATH":/daikon.jar daikon.Chicory --output-dir="/" --instrument-methods-file-path="/target_methods" --system-jvm-opts="-Dxxx $JVM_OPTS $cassandra_parms" "$class"
```