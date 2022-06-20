FROM openjdk:8-jdk

# 0. use tuna apt sources

# RUN apt install apt-transport-https ca-certificates
# RUN mv /etc/apt/sources.list /etc/apt/sources.list.bak && \
#     echo "deb https://mirrors.tuna.tsinghua.edu.cn/debian/ bullseye main contrib non-free" >> /etc/apt/sources.list && \
#     echo "deb https://mirrors.tuna.tsinghua.edu.cn/debian/ bullseye-updates main contrib non-free" >> /etc/apt/sources.list && \
#     echo "deb https://mirrors.tuna.tsinghua.edu.cn/debian/ bullseye-backports main contrib non-free" >> /etc/apt/sources.list && \
#     echo "deb https://mirrors.tuna.tsinghua.edu.cn/debian-security bullseye-security main contrib non-free" >> /etc/apt/sources.list

# 1. General setup.

# Use supervisor to run multiple processes, in case the target system requires
# multiple processes. 
# Remember to open port 22 for sshd.
#RUN apt-get update && apt-get install -y --allow-unauthenticated openssh-server supervisor
RUN apt-get update && apt-get install -y openssh-server supervisor vim ant maven
COPY supervisord.conf /etc/supervisor/conf.d/supervisord.conf
ADD general-setup.sh /usr/local/bin/general-setup.sh
RUN sh /usr/local/bin/general-setup.sh
COPY org.jacoco.agent.rt.jar /org.jacoco.agent.rt.jar

# 2. System setup.

COPY cassandra /cassandra
WORKDIR /cassandra
RUN ant
COPY cqlsh_daemon.py /cassandra/bin/cqlsh_daemon.py
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
