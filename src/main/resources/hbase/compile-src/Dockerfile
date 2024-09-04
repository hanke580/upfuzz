FROM ubuntu:18.04

# 0. use tuna apt sources
RUN apt-get update
RUN apt install apt-transport-https ca-certificates -y

# ENV http_proxy "http://172.17.0.1:2334"
# ENV https_proxy "http://172.17.0.1:2334"

# 1. General setup.

# Use supervisor to run multiple processes, in case the target system requires
# multiple processes.
# Remember to open port 22 for sshd.
#RUN apt-get update && apt-get install -y --allow-unauthenticated openssh-server supervisor
RUN apt-get update && apt-get install -y openssh-server iproute2 net-tools iputils-ping supervisor vim ant maven python python3 python3-pip python-six psmisc

RUN pip3 install six
RUN pip3 install pexpect
RUN apt-get install iptables -y

RUN apt-get install openjdk-8-jdk -y
RUN apt-get install openjdk-17-jdk -y
RUN ln -sf /usr/lib/jvm/java-1.8.0-openjdk-amd64/bin/java /usr/bin/java

ADD general-setup.sh /usr/local/bin/general-setup.sh
COPY org.jacoco.agent.rt.jar /org.jacoco.agent.rt.jar
COPY supervisord.conf /etc/supervisor/conf.d/supervisord.conf
COPY hbase-init.sh /usr/local/bin/hbase-init.sh
COPY hbase-daemon-init.sh /usr/local/bin/hbase-daemon-init.sh
COPY rolling-upgrade.sh /usr/local/bin/rolling-upgrade.sh

RUN sh /usr/local/bin/general-setup.sh
RUN chmod +777 /usr/local/bin/hbase-init.sh
RUN chmod +777 /usr/local/bin/hbase-daemon-init.sh
RUN chmod +777 /usr/local/bin/rolling-upgrade.sh

# 2. System setup.

# RUN echo "export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/" >> /hbase/conf/hbase-env.sh

# build locally
# COPY cassandra /cassandra
# WORKDIR /cassandra
# RUN ant
# COPY hbase_daemon3.py /hbase/hbase_daemon.py

# TODO: copy hshell init
# when the system has started up, we start up the hshell
# daemon process
# COPY cqlsh_init.sh /cassandra/cqlsh_init.sh


# This file contains relatively permanent configurations.
# ADD cass-setup.sh /usr/local/bin/cass-setup.sh
# RUN sh /usr/local/bin/cass-setup.sh

# set CASSANDRA_HOME manually through docker shell
# ENV CASSANDRA_CONFIG /cassandra/conf

# This file contains all the runtime configurations (configurations that might
# need to be modified everytime Cassandra is run, e.g., logging level.)

#ADD cassandra-clusternode.sh /usr/local/bin/cassandra-clusternode

# This can be overwritten in docker-compose.yaml by setting command
EXPOSE 22 9160
CMD ["/usr/bin/supervisord"]
