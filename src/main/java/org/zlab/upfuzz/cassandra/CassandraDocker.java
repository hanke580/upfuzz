package org.zlab.upfuzz.cassandra;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.text.StringSubstitutor;
import org.zlab.upfuzz.docker.DockerCluster;
import org.zlab.upfuzz.docker.DockerMeta;
import org.zlab.upfuzz.docker.IDocker;

public class CassandraDocker
        extends DockerMeta implements IDocker {
    String composeYaml;

    public CassandraCqlshDaemon cqlsh;

    public CassandraDocker(CassandraDockerCluster cdc, int index) {
        this.index = index;
        workdir = cdc.workdir;
        type = cdc.type;
        system = cdc.system;
        version = cdc.version;
        networkName = cdc.networkName;
        subnet = cdc.subnet;
        hostIP = cdc.hostIP;
        networkIP = DockerCluster.getKthIP(hostIP, index);
        seedIP = cdc.seedIP;
        agentPort = cdc.agentPort;
        includes = cdc.inclueds;
        excludes = cdc.excludes;
        executorID = cdc.executorID;
    }

    @Override
    public String getNetworkIP() {
        return networkIP;
    }

    public String formatComposeYaml() {
        Map<String, String> formatMap = new HashMap<>();
        String javaToolOpts = "JAVA_TOOL_OPTIONS=-javaagent:"
                + "/org.jacoco.agent.rt.jar"
                + "=append=false"
                + ",includes=" + includes + ",excludes=" + excludes +
                ",output=dfe,address=" + hostIP + ",port="
                + agentPort +
                ",sessionid=" + system + "-" + executorID + "_"
                + type;

        formatMap.put("system", system);
        formatMap.put("version", version);
        formatMap.put("index", Integer.toString(index));
        formatMap.put("networkName", networkName);
        formatMap.put("JAVA_TOOL_OPTIONS", javaToolOpts);
        formatMap.put("subnet", subnet);
        formatMap.put("seedIP", seedIP);
        formatMap.put("networkIP", networkIP);
        formatMap.put("agentPort", Integer.toString(agentPort));
        formatMap.put("executorID", executorID);
        StringSubstitutor sub = new StringSubstitutor(formatMap);
        this.composeYaml = sub.replace(template);

        return composeYaml;
    }

    @Override
    public int start() {
        cqlsh = new CassandraCqlshDaemon(getNetworkIP());
        return 0;
    }

    @Override
    public void teardown() {
    }

    @Override
    public boolean build() {
        // FIXME skip this for local test
        // URL pyScript = CassandraDockerCompose.class.getClassLoader()
        // .getResource("build.py");
        // String pyScriptPath = pyScript.getPath();
        // File scriptPath = Paths.get(pyScriptPath).getParent().toFile();
        // try {
        // logger.info("Build Dockerfile " + systemID + " " + originalVersion);
        // Process buildProcess = Utilities.exec(
        // new String[] { "python3", pyScriptPath, systemID,
        // originalVersion },
        // scriptPath);
        // int exit = buildProcess.waitFor();
        // if (exit == 0) {
        // logger.info("Build docker succeed.");
        // } else {
        // String errorMessage = Utilities.readProcess(buildProcess);
        // logger.error("Build docker failed\n" + errorMessage);
        // }
        // return (exit == 0);
        // } catch (IOException | InterruptedException e) {
        // e.printStackTrace();
        // }
        return false;
    }

    @Override
    public Path getDataPath() {
        return Paths.get(workdir.toString(),
                "/persistent/data/node_" + index + "_data");
    }

    public Path getWorkPath() {
        return workdir.toPath();
    }

    static String template = ""
            + "    DC3N${index}:\n"
            + "        container_name: cassandra-3.11.13_${executorID}_N${index}\n"
            + "        image: image_${system}_${version}-compile\n"
            + "        command: bash -c 'sleep 0 && /usr/bin/supervisord'\n"
            + "        networks:\n"
            + "            ${networkName}:\n"
            + "                ipv4_address: ${networkIP}\n"
            + "        volumes:\n"
            + "            - ./persistent/data/node_${index}_data:/var/lib/cassandra\n"
            + "            - ./persistent/log/node_${index}_log:/var/log/cassandra\n"
            +
            "            - ./persistent/consolelog/node_${index}_consolelog:/var/log/supervisor\n"
            + "        environment:\n"
            + "            - CASSANDRA_CONFIG=/cassandra/conf\n"
            + "            - CASSANDRA_CLUSTER_NAME=dev_cluster\n"
            + "            - CASSANDRA_SEEDS=${seedIP},\n"
            + "            - CASSANDRA_LOGGING_LEVEL=DEBUG\n"
            + "            - CQLSH_HOST=${networkIP}\n"
            + "            - ${JAVA_TOOL_OPTIONS}\n"
            + "        expose:\n"
            + "            - ${agentPort}\n"
            + "            - 7000\n"
            + "            - 7001\n"
            + "            - 7199\n"
            + "            - 9042\n"
            + "            - 9160\n"
            + "            - 18251\n"
            + "        ulimits:\n"
            + "            memlock: -1\n"
            + "            nproc: 32768\n"
            + "            nofile: 100000\n";
}
