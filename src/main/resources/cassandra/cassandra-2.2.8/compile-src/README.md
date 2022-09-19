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
