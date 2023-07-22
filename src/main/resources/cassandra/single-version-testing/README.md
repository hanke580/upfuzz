How: Run `docker build -t test ./` under the current dir. 

This only needs to be run once in order to build a docker image with the target
version of Cassandra. 

This bunch of scripts is exactly the same as the trunk build image script,
except for one thing - the branch name in compile-src.sh

There's one thing that needs to be changed in compile-src.sh:
cp -r /Users/yongle/research/projects/upgrade/src/cassandra ./
Here /Users/yongle/research/projects/upgrade/src/cassandra is the src code dir. 

This is because there's no difference between cassandra-3.11.4 and trunk
(cassandra-4.0) on 1) runtime dependency and 2) configuration. 


## Run old version Cassandra
For old version cassandra we need to change the `bin/cassandra` script to set up the log folder.
E.g. for cassandra-2.2.8 or cassandra-3.0.15

```bash
sed -i 's|\$CASSANDRA_HOME/logs|/var/log/cassandra|g' bin/cassandra
```

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