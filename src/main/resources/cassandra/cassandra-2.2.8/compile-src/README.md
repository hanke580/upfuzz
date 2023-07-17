For old version cassandra we need to change the `bin/cassandra` script to set up the log folder.
E.g. for cassandra-2.2.8 or cassandra-3.0.15

```bash

launch_service()
{
    pidpath="$1"
    foreground="$2"
    props="$3"
    class="$4"
    cassandra_parms="-Dlogback.configurationFile=logback.xml"
    cassandra_parms="$cassandra_parms -Dcassandra.logdir=/var/log/cassandra"
```