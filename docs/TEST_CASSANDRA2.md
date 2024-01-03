Cassandra 2.x (2.2.8) doesn't set up ``$CASSANDRA_LOG_DIR`, so we need to modify bin/cassandra to set it up

```bash
if [ -z "$CASSANDRA_LOG_DIR" ]; then
  CASSANDRA_LOG_DIR=$CASSANDRA_HOME/logs
fi

launch_service()
{
    pidpath="$1"
    foreground="$2"
    props="$3"
    class="$4"
    cassandra_parms="-Dlogback.configurationFile=logback.xml"
    cassandra_parms="$cassandra_parms -Dcassandra.logdir=$CASSANDRA_LOG_DIR"
    cassandra_parms="$cassandra_parms -Dcassandra.storagedir=$cassandra_storagedir"
```