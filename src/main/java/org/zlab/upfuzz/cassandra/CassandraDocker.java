package org.zlab.upfuzz.cassandra;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.text.StringSubstitutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.docker.Docker;
import org.zlab.upfuzz.docker.DockerCluster;
import org.zlab.upfuzz.utils.Utilities;

public class CassandraDocker extends Docker {
    protected final Logger logger = LogManager.getLogger(getClass());

    String composeYaml;
    String javaToolOpts;
    int cqlshDaemonPort = 18251;

    public String seedIP;

    public CassandraCqlshDaemon cqlsh;

    public CassandraDocker(CassandraDockerCluster dockerCluster, int index) {
        this.index = index;
        type = "original";
        workdir = dockerCluster.workdir;
        system = dockerCluster.system;
        originalVersion = dockerCluster.originalVersion;
        upgradedVersion = dockerCluster.upgradedVersion;
        networkName = dockerCluster.networkName;
        subnet = dockerCluster.subnet;
        hostIP = dockerCluster.hostIP;
        networkIP = DockerCluster.getKthIP(hostIP, index);
        seedIP = dockerCluster.seedIP;
        agentPort = dockerCluster.agentPort;
        includes = CassandraDockerCluster.includes;
        excludes = CassandraDockerCluster.excludes;
        executorID = dockerCluster.executorID;
        name = "cassandra-" + originalVersion + "_" + upgradedVersion + "_" +
                executorID + "_N" + index;
        serviceName = "DC3N" + index;
        targetSystemStates = dockerCluster.targetSystemStates;
    }

    @Override
    public String getNetworkIP() {
        return networkIP;
    }

    @Override
    public String formatComposeYaml() {
        Map<String, String> formatMap = new HashMap<>();

        containerName = "cassandra-" + originalVersion + "_" + upgradedVersion +
                "_" + executorID + "_N" + index;
        formatMap.put("projectRoot", System.getProperty("user.dir"));
        formatMap.put("system", system);
        formatMap.put("originalVersion", originalVersion);
        formatMap.put("upgradedVersion", upgradedVersion);
        formatMap.put("index", Integer.toString(index));
        formatMap.put("networkName", networkName);
        formatMap.put("JAVA_TOOL_OPTIONS", javaToolOpts);
        formatMap.put("subnet", subnet);
        formatMap.put("seedIP", seedIP);
        formatMap.put("networkIP", networkIP);
        formatMap.put("agentPort", Integer.toString(agentPort));
        formatMap.put("executorID", executorID);
        formatMap.put("serviceName", serviceName);

        StringSubstitutor sub = new StringSubstitutor(formatMap);
        this.composeYaml = sub.replace(template);
        return composeYaml;
    }

    @Override
    public int start() throws IOException, InterruptedException {
        cqlsh = new CassandraCqlshDaemon(getNetworkIP(), cqlshDaemonPort,
                executorID);
        return 0;
    }

    private void setEnvironment() throws IOException {
        File envFile = new File(workdir,
                "./persistent/node_" + index + "/env.sh");

        FileWriter fw;
        envFile.getParentFile().mkdirs();
        fw = new FileWriter(envFile, false);
        for (String s : env) {
            fw.write("export " + s + "\n");
        }
        fw.close();
    }

    @Override
    public void teardown() {
    }

    @Override
    public boolean build() throws IOException {
        String cassandraHome = "/cassandra/" + originalVersion;
        String cassandraConf = "/etc/" + originalVersion;
        javaToolOpts = "JAVA_TOOL_OPTIONS=\"-javaagent:"
                + "/org.jacoco.agent.rt.jar"
                + "=append=false"
                + ",includes=" + includes + ",excludes=" + excludes +
                ",output=dfe,address=" + hostIP + ",port=" + agentPort +
                ",sessionid=" + system + "-" + executorID + "_"
                + type + "-" + index +
                "\"";

        env = new String[] {
                "CASSANDRA_HOME=\"" + cassandraHome + "\"",
                "CASSANDRA_CONF=\"" + cassandraConf + "\"", javaToolOpts,
                "CQLSH_DAEMON_PORT=\"" + cqlshDaemonPort + "\"",
                "PYTHON=python2" };
        setEnvironment();
        return false;
    }

    public void upgrade() throws Exception {
        type = "upgraded";
        javaToolOpts = "JAVA_TOOL_OPTIONS=\"-javaagent:"
                + "/org.jacoco.agent.rt.jar"
                + "=append=false"
                + ",includes=" + includes + ",excludes=" + excludes +
                ",output=dfe,address=" + hostIP + ",port=" + agentPort +
                ",sessionid=" + system + "-" + executorID + "_" + type +
                "-" + index +
                "\"";
        cqlshDaemonPort ^= 1;
        String cassandraHome = "/cassandra/" + upgradedVersion;
        String cassandraConf = "/etc/" + upgradedVersion;
        env = new String[] {
                "CASSANDRA_HOME=\"" + cassandraHome + "\"",
                "CASSANDRA_CONF=\"" + cassandraConf + "\"", javaToolOpts,
                "CQLSH_DAEMON_PORT=\"" + cqlshDaemonPort + "\"",
                "PYTHON=python2" };
        setEnvironment();
        String restartCommand = "supervisorctl restart upfuzz_cassandra:";
        Process restart = runInContainer(
                new String[] { "/bin/bash", "-c", restartCommand }, env);
        int ret = restart.waitFor();
        String message = Utilities.readProcess(restart);
        logger.debug("upgrade version start: " + ret + "\n" + message);
        cqlsh = new CassandraCqlshDaemon(getNetworkIP(), cqlshDaemonPort,
                executorID);
    }

    @Override
    public boolean shutdown() {
        String[] stopNode = new String[] {
                "/" + system + "/" + originalVersion + "/"
                        + "bin/nodetool",
                "stopdaemon" };
        int ret = runProcessInContainer(stopNode);
        return ret == 0;
    }

    @Override
    public Path getDataPath() {
        return Paths.get(workdir.toString(),
                "/persistent/node_" + index + "/data");
    }

    public Path getWorkPath() {
        return workdir.toPath();
    }

    public void chmodDir() throws IOException {
        runInContainer(
                new String[] { "chmod", "-R", "777", "/var/log/cassandra" });
        runInContainer(
                new String[] { "chmod", "-R", "777", "/var/lib/cassandra" });
        runInContainer(
                new String[] { "chmod", "-R", "777", "/var/log/supervisor" });
        runInContainer(
                new String[] { "chmod", "-R", "777", "/usr/bin/set_env" });
    }

    static String template = ""
            + "    ${serviceName}:\n"
            + "        container_name: cassandra-${originalVersion}_${upgradedVersion}_${executorID}_N${index}\n"
            + "        image: upfuzz_${system}:${originalVersion}_${upgradedVersion}\n"
            + "        command: bash -c 'sleep 0 && /usr/bin/supervisord'\n"
            + "        networks:\n"
            + "            ${networkName}:\n"
            + "                ipv4_address: ${networkIP}\n"
            + "        volumes:\n"
            + "            - ./persistent/node_${index}/data:/var/lib/cassandra\n"
            + "            - ./persistent/node_${index}/log:/var/log/cassandra\n"
            + "            - ./persistent/node_${index}/env.sh:/usr/bin/set_env\n"
            + "            - ./persistent/node_${index}/consolelog:/var/log/supervisor\n"
            + "            - ${projectRoot}/prebuild/${system}/${originalVersion}:/${system}/${originalVersion}\n"
            + "            - ${projectRoot}/prebuild/${system}/${upgradedVersion}:/${system}/${upgradedVersion}\n"
            + "        environment:\n"
            + "            - CASSANDRA_CLUSTER_NAME=dev_cluster\n"
            + "            - CASSANDRA_SEEDS=${seedIP},\n"
            + "            - CASSANDRA_LOGGING_LEVEL=DEBUG\n"
            + "            - CQLSH_HOST=${networkIP}\n"
            + "            - CASSANDRA_LOG_DIR=/var/log/cassandra\n"
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

    @Override
    public Map<String, String> readSystemState() {
        Map<String, String> states = new HashMap<>();
        // Cassandra do not distinguish nodes
        // HDFS might get state from different nodes
        for (String stateName : targetSystemStates) {
            Path filePath = Paths.get("/var/log/cassandra/system.log");
            String targetEnd = String.format(
                    "\\[InconsistencyDetectorEnd\\]\\[%s\\]",
                    stateName);
            String[] grepStateCmd = new String[] {
                    "/bin/sh", "-c",
                    "grep -a -B 20 \"" + targetEnd + "\" " + filePath
                            + " | tail -n 20"
            };

            try {
                System.out.println("\n\n");
                Process grepProc = runInContainer(grepStateCmd);
                String result = new String(
                        grepProc.getInputStream().readAllBytes());

                logger.info("grep result = \n" + result);
                String stateValue = "";

                if (!result.isEmpty()) {
                    String targetStart_ = String.format(
                            "[InconsistencyDetectorStart][%s]",
                            stateName);
                    String targetEnd_ = String.format(
                            "[InconsistencyDetectorEnd][%s]",
                            stateName);

                    int lastIdx = result.lastIndexOf(targetStart_);
                    if (lastIdx != -1) {
                        String subString = result.substring(lastIdx);
                        stateValue = subString.substring(
                                subString.indexOf("\n") + 1,
                                subString.lastIndexOf(targetEnd_)
                                        - 1);
                    } else {
                        logger.error(String.format(
                                "Node[%d] State[%s] cannot find target start",
                                index, stateName));
                    }
                } else {
                    logger.error(
                            String.format("Node[%d] State[%s] result is empty",
                                    index, stateName));
                }
                logger.info(String.format("State [%s] =  %s", stateName,
                        stateValue));
                states.put(stateName, stateValue);
            } catch (IOException e) {
                logger.error(String.format(
                        "Problem when reading state in docker[%d]", index));
                e.printStackTrace();
            }
        }
        return states;
    }

}
