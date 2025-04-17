FROM openjdk:8-jdk
# 1. General setup.

# Use supervisor to run multiple processes, in case the target system requires
# multiple processes.
# Remember to open port 22 for sshd.
RUN apt-get update && apt-get install -y openssh-server supervisor vim
COPY supervisord.conf /etc/supervisor/conf.d/supervisord.conf
ADD general-setup.sh /usr/local/bin/general-setup.sh
RUN sh /usr/local/bin/general-setup.sh

# 2. System setup.

COPY cassandra.tar.gz /cassandra.tar.gz
ADD cass-setup.sh /usr/local/bin/cass-setup.sh
# This file contains relatively permanent configurations.
RUN sh /usr/local/bin/cass-setup.sh
ENV CASSANDRA_CONFIG /cassandra/conf
# This file contains all the runtime configurations (configurations that might
# need to be modified everytime Cassandra is run, e.g., logging level.)
ADD cassandra-clusternode.sh /usr/local/bin/cassandra-clusternode

# This can be overwritten in docker-compose.yaml by setting command
EXPOSE 22 9160
CMD ["/usr/bin/supervisord"]
