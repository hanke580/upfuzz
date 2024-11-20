FROM ubuntu:18.04

# 0. use tuna apt sources
RUN apt-get update
RUN apt install apt-transport-https ca-certificates -y

# ENV http_proxy "http://172.17.0.1:2334"
# ENV https_proxy "http://172.17.0.1:2334"

# 1. General setup.

# Use supervisor to run multiple processes, in case the target system requires
# multiple processes.
RUN apt-get update && apt-get install -y openssh-server iproute2 net-tools iputils-ping supervisor vim ant maven python python3 python3-pip python-six psmisc lsof

RUN pip3 install six
RUN apt-get install iptables -y

RUN apt-get install openjdk-8-jdk -y
RUN ln -sf /usr/lib/jvm/java-1.8.0-openjdk-amd64/bin/java /usr/bin/java

ADD general-setup.sh /usr/local/bin/general-setup.sh
RUN sh /usr/local/bin/general-setup.sh
COPY org.jacoco.agent.rt.jar /org.jacoco.agent.rt.jar
COPY supervisord.conf /etc/supervisor/conf.d/supervisord.conf

# 2. System setup.

RUN mkdir /ozone-config
COPY ozone_config/* /ozone-config/

COPY ozone_shell_init.sh /
RUN chmod +x /ozone_shell_init.sh

COPY config /root/.ssh/
RUN chmod 644 /root/.ssh/config

ADD ozone-clusternode.sh /usr/local/bin/ozone-clusternode
RUN chmod +x /usr/local/bin/ozone-clusternode

# This can be overwritten in docker-compose.yaml by setting command
EXPOSE 22 9160
CMD ["/usr/bin/supervisord"]
