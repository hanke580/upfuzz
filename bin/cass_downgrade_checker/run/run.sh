#!/bin/bash
rm cassandra_output.log

update_dockerfile() {
        # Assign arguments to variables
        cassandra_version_Y="$1"
        cassandra_version_X="$2"

        # Define the filename
        dockerfile_name="../compile-src/Dockerfile"

        # find the commented line
        commented_line=$(grep -n "# 1\. Put code in it" "$dockerfile_name" | cut -d':' -f1)

        # Use sed to remove the two lines after the commented line and add new lines in their place
        line_to_delete_start=$((commented_line + 1))
        line_to_delete_end=$((commented_line + 2))

        # Remove the next two lines
        sed -i "${line_to_delete_start},${line_to_delete_end}d" "$dockerfile_name"

        #..\\/..\\/..\\/prebuild\\/cassandra\\/
        # Add the commented line and new lines
        sed -i "${commented_line}a\\
COPY apache-cassandra-${cassandra_version_Y} \\/cassandra\\/apache-cassandra-${cassandra_version_Y}\\
COPY apache-cassandra-${cassandra_version_X} \\/cassandra\\/apache-cassandra-${cassandra_version_X}" "$dockerfile_name"
}

update_clusternode_script() {
        # Assign arguments to variables
        cassandra_version_X="$2"
        cassandra_version_Y="$1"

        # Define the filename
        clusternode_script="../compile-src/cassandra-clusternode.sh"

        # Use sed to replace the versions in the file
        sed -i "s/^ORG_VERSION=apache-cassandra-[0-9.]\+/ORG_VERSION=apache-cassandra-$cassandra_version_X/" "$clusternode_script"
        sed -i "s/^UPG_VERSION=apache-cassandra-[0-9.]\+/UPG_VERSION=apache-cassandra-$cassandra_version_Y/" "$clusternode_script"
}

build_test_docker_image() {
        cd ../compile-src/
        cp -r "../../../prebuild/cassandra/apache-cassandra-$1/" apache-cassandra-$1
        cp -r "../../../prebuild/cassandra/apache-cassandra-$2/" apache-cassandra-$2
        docker build . -t upfuzz_cassandra_test:apache-cassandra-"$1"_apache-cassandra-"$2"
        rm -rf apache-cassandra-$1
        rm -rf apache-cassandra-$2
        cd -
}

create_docker_compose_yaml() {
        # Assign arguments to variables
        cassandra_version_Y="$1"
        cassandra_version_X="$2"

        # Generate docker-compose.yaml content
        cat << EOF > docker-compose.yaml
version: '3'
services:
    DC3N0:
        container_name: cassandra-test_N0
        image: upfuzz_cassandra_test:apache-cassandra-${cassandra_version_Y}_apache-cassandra-${cassandra_version_X}
        command: bash -c 'sleep 0 && /usr/bin/supervisord'
        networks:
            network_cassandra_apache-cassandra-${cassandra_version_Y}_to_apache-cassandra-${cassandra_version_X}_8ea795f9-71b9-43fb-b578-7d6e69677f92:
                ipv4_address: 192.168.47.2
        volumes:
            - ./persistent/node_0/env.sh:/usr/bin/set_env
        environment:
            - CASSANDRA_CLUSTER_NAME=dev_cluster
            - CASSANDRA_SEEDS=192.168.47.2,
            - CASSANDRA_LOGGING_LEVEL=DEBUG
            - CQLSH_HOST=192.168.47.2
            - CASSANDRA_LOG_DIR=/var/log/cassandra
        expose:
            - 34979
            - 7000
            - 7001
            - 7199
            - 9042
            - 9160
            - 18251
        ulimits:
            memlock: -1
            nproc: 32768
            nofile: 100000

networks:
    network_cassandra_apache-cassandra-${cassandra_version_Y}_to_apache-cassandra-${cassandra_version_X}_8ea795f9-71b9-43fb-b578-7d6e69677f92:
        driver: bridge
        ipam:
            driver: default
            config:
                - subnet: 192.168.47.0/24
EOF
}

extract_first_segment() {
    # Extract characters before the first period (.) from $1
    first_segment="${1%%.*}"
    echo "$first_segment"
}

# 2. Start up cluster (docker compose)
test () {
        echo "hh"

        echo "export CASSANDRA_HOME=\"/cassandra/apache-cassandra-$1\"" > ./persistent/node_0/env.sh
        echo "export CASSANDRA_CONF=\"/etc/apache-cassandra-$1\"" >> ./persistent/node_0/env.sh
        if [[ $(extract_first_segment "$1") -le 3 ]]; then
                echo "export PYTHON=python2" >> ./persistent/node_0/env.sh
        else
                echo "export PYTHON=python3" >> ./persistent/node_0/env.sh
        fi

        docker compose up --force-recreate &

        CQLSH="/cassandra/apache-cassandra-$1/bin/cqlsh"

        # Wait for all nodes to upgrade
        while true; do
                docker exec cassandra-test_N0 ${CQLSH} -e "describe cluster"
                if [[ "$?" -eq 0 ]]; then
                        break
                fi
                sleep 5
        done

        # 3. Execute commands
        docker exec cassandra-test_N0 ${CQLSH} --request-timeout=40 -e "CREATE KEYSPACE  uuid5f86250110a247d48c481c5579cb2ea1 WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };"
        docker exec cassandra-test_N0 ${CQLSH} --request-timeout=40 -e "CREATE TABLE  uuid5f86250110a247d48c481c5579cb2ea1.kattmG (kattmG TEXT,VldG TEXT,Ajqm TEXT,lTQSQ INT,EVzlSKrbkUGyhJshH TEXT, PRIMARY KEY (lTQSQ ));"
        docker exec cassandra-test_N0 ${CQLSH} --request-timeout=40 -e "INSERT INTO uuid5f86250110a247d48c481c5579cb2ea1.kattmG (Ajqm, EVzlSKrbkUGyhJshH, lTQSQ) VALUES ('nZJzNjYnXOwPLpVoFSVwxcvznsDFBYqmlprrVXYJQLzYvYkrmfEsiuAcCtggypnxIkIevRHyPQGOWrIZNObJ','RaAhbVKUQzgJaupaupKPVnNLLYDaZEaMyFteVwhLePqZwikuBEsVDxTuTqBfkFYmeMMsOFXjVkObZduPfAFsLzuYlrgpYsPPxDNQCRzzPaEdWHARnnWbAFAUUnbYnvEESeHDRHSkEhSnoREprrHWasYLMSocIYiMGQXjzsaKptqbtPgrztIdpQLgDAZOPfhJIblmwTFAWiFbzrbTkFwJGP',1693380861);"
        
        read_res1=$(docker exec cassandra-test_N0 ${CQLSH} -e "SELECT lTQSQ FROM uuid5f86250110a247d48c481c5579cb2ea1.kattmG;")
        
        # 4. Perform full-stop upgrade

        # Node shutdown
        docker exec cassandra-test_N0 /cassandra/apache-cassandra-$1/bin/nodetool drain
        docker exec cassandra-test_N0 /cassandra/apache-cassandra-$1/bin/nodetool stopdaemon

        # Upgrade
        echo "export CASSANDRA_HOME=\"/cassandra/apache-cassandra-$2\"" > ./persistent/node_0/env.sh
        echo "export CASSANDRA_CONF=\"/etc/apache-cassandra-$2\"" >> ./persistent/node_0/env.sh
        if [[ $(extract_first_segment "$2") -le 3 ]]; then
                echo "export PYTHON=python2" >> ./persistent/node_0/env.sh
        else
                echo "export PYTHON=python3" >> ./persistent/node_0/env.sh
        fi

        # Node restart
        CQLSH="/cassandra/apache-cassandra-$2/bin/cqlsh"

        docker exec cassandra-test_N0 /bin/bash -c "supervisorctl restart upfuzz_cassandra:" >> cassandra_output.log
                while true; do
                docker exec cassandra-test_N0 ${CQLSH} -e "describe cluster" 
                if [[ "$?" -eq 0 ]]; then
                        break
                fi
                docker exec cassandra-test_N0 /bin/bash -c "grep -i \"exit status 3\" /var/log/supervisor/supervisord.log"
                # echo "$?"
                if [[ "$?" -eq 0 ]]; then
                        docker rm -f $(docker ps -a -q -f ancestor=upfuzz_cassandra_test:apache-cassandra-"$1"_apache-cassandra-"$2")
                        docker container prune -f
                        docker network prune -f
                        return 1
                fi
                sleep 5 
        done

        read_res2=$(docker exec cassandra-test_N0 ${CQLSH} -e "SELECT lTQSQ FROM uuid5f86250110a247d48c481c5579cb2ea1.kattmG;")
        
        printf "old read = \n${read_res1}\n" >> res.log
        printf "new read = \n${read_res2}\n" >> res.log
        printf "\n" >> res.log

        docker rm -f $(docker ps -a -q -f ancestor=upfuzz_cassandra_test:apache-cassandra-"$1"_apache-cassandra-"$2")
        docker container prune -f
        docker network prune -f
        return 0
}

update_dockerfile $1 $2
update_clusternode_script $1 $2
build_test_docker_image $1 $2
create_docker_compose_yaml $1 $2
test $1 $2
# for i in {0..0}
# do
#         test
#         res=$?
#         echo $res
# done
