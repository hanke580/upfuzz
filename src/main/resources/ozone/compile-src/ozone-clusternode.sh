#!/usr/bin/env bash
set -euo pipefail

# Get running container's IP
IP=$(hostname --ip-address | cut -f 1 -d ' ')
if [ $# == 1 ]; then
    OM_NODE="$1,$IP"
else 
    OM_NODE="$IP"
fi

# Change it to the target systems
ORI_VERSION=ozone-1.2.1
UP_VERSION=ozone-1.4.1

# Create necessary directories
mkdir -p /var/log/ozone
mkdir -p /var/lib/ozone

if [[ ! -f "/var/log/.setup_conf" ]]; then
    if [ "$ENABLE_FORMAT_COVERAGE" = "true" ]; then
        # Copy the file to /tmp
        echo "Enable format coverage"
        cp "$OZONE_HOME/topObjects.json" /tmp/
        cp "$OZONE_HOME/serializedFields_alg1.json" /tmp/
        cp "$OZONE_HOME/comparableClasses.json" /tmp/ || true
        cp "$OZONE_HOME/modifiedEnums.json" /tmp/ || true
        cp "$OZONE_HOME/modifiedFields.json" /tmp/ || true
    fi

    # ENABLE_NET_COVERAGE
    if [ "$ENABLE_NET_COVERAGE" = "true" ]; then
        # Copy the file to /tmp
        echo "Enable net coverage"
        cp "$CASSANDRA_HOME/modifiedFields.json" /tmp/ || true
    fi

    echo "Setting up Apache Ozone configurations"
    for VERSION in ${ORI_VERSION} ${UP_VERSION}; do
        mkdir /etc/${VERSION}
        cp -r /ozone/${VERSION}/etc /etc/${VERSION}/
        cp /ozone-config/* /etc/${VERSION}/etc/hadoop/

        CONFIG="/etc/${VERSION}/etc/hadoop/"
        if [[ $VERSION == "${ORI_VERSION}" ]]; then
            cp /test_config/oriconfig/* ${CONFIG}/
        fi
        if [[ $VERSION == "${UP_VERSION}" ]]; then
            cp /test_config/upconfig/* ${CONFIG}/
        fi

        echo "export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/" >> ${CONFIG}/ozone-env.sh
        echo "export OZONE_OM_USER=\"root\"" >> ${CONFIG}/ozone-env.sh
        echo "export OZONE_SCM_USER=\"root\"" >> ${CONFIG}/ozone-env.sh
        echo "export OZONE_DATANODE_USER=\"root\"" >> ${CONFIG}/ozone-env.sh
        # echo "export OZONE_RECON_USER=\"root\"" >> ${CONFIG}/ozone-env.sh

        echo "export OZONE_LOG_DIR=/var/log/ozone" >> ${CONFIG}/ozone-env.sh

        echo "export OZONE_CLASSPATH=\${OZONE_CLASSPATH}:/ozone/${VERSION}" >> ${CONFIG}/ozone-env.sh

        echo datanode1 > ${CONFIG}/ozone-workers
        echo datanode2 >> ${CONFIG}/ozone-workers
        echo datanode3 >> ${CONFIG}/ozone-workers

        # Configuring a 5 node Ozone cluster
    done
    echo "Setup done"
    touch "/var/log/.setup_conf"
fi

IP_MASK=$(echo $IP | cut -d "." -f -3)
OZONE_SCM=$IP_MASK.2
OZONE_OM=$IP_MASK.3
# OZONE_RECON=$IP_MASK.4

if [[ -z $(grep -F "ozone-master" "/etc/hosts") ]]; then
    echo "$OZONE_OM    om" >> /etc/hosts
    echo "$OZONE_SCM    ozone-scm" >> /etc/hosts
    # echo "$OZONE_RECON    ozone-recon" >> /etc/hosts
    echo "$IP_MASK.4    datanode1" >> /etc/hosts
    echo "$IP_MASK.5    datanode2" >> /etc/hosts
    echo "$IP_MASK.6    datanode3" >> /etc/hosts

    # Uncomment the following lines if you need to set Ozone home in bashrc
    # echo "OZONE_HOME=$OZONE_HOME" >> ~/.bashrc
    # echo "PATH=\${PATH}:\${OZONE_HOME}/bin:\${OZONE_HOME}/sbin" >> ~/.bashrc
    # source ~/.bashrc
fi

echo "Starting Apache Ozone on $IP..."

if [[ "$IP" == "$OZONE_OM" ]]; then
    if [[ ! -f /var/log/ozone/.formatted ]]; then
        echo "Formatting Ozone Manager (OM)"
        $OZONE_HOME/bin/ozone om --init
        touch /var/log/ozone/.formatted
    fi

    splitArr=(${OZONE_HOME//\// })
    CUR_VERSION=${splitArr[1]}
    echo "cur version = $CUR_VERSION"
    echo "org version = $ORI_VERSION"
    echo "up  version = $UP_VERSION"

    $OZONE_HOME/bin/ozone --daemon start om
    # if [[ $CUR_VERSION == $ORI_VERSION ]]; then
    #     echo "Start up old version $OZONE_HOME"
    #     $OZONE_HOME/bin/ozone --daemon start om
    # else
    #     echo "Start up new version $OZONE_HOME"
    #     $OZONE_HOME/bin/ozone --daemon start om -rollingUpgrade started
    # fi
elif [[ "$IP" == "$OZONE_SCM" ]]; then
    $OZONE_HOME/bin/ozone scm --init
    $OZONE_HOME/bin/ozone --daemon start scm
# elif [[ "$IP" == "$OZONE_RECON" ]]; then
#     $OZONE_HOME/bin/ozone --daemon start recon
else
    $OZONE_HOME/bin/ozone --daemon start datanode
fi
