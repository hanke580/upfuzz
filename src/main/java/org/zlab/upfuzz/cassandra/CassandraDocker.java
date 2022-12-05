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
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.fuzzingengine.LogInfo;
import org.zlab.upfuzz.utils.Utilities;

public class CassandraDocker extends Docker {
    protected final Logger logger = LogManager.getLogger(getClass());

    String composeYaml;
    String javaToolOpts;
    int cqlshDaemonPort = 18250;

    public String seedIP;

    public CassandraCqlshDaemon cqlsh;

    public CassandraDocker(CassandraDockerCluster dockerCluster, int index) {
        this.index = index;
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
        configPath = dockerCluster.configpath;
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
    public int start() throws Exception {
        cqlsh = new CassandraCqlshDaemon(getNetworkIP(), cqlshDaemonPort, this);
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
        type = "original";
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

        String pythonVersion = "python2";

        String[] spStrings = originalVersion.split("-");
        try {
            int main_version = Integer
                    .parseInt(spStrings[spStrings.length - 1].substring(0, 1));
            logger.debug("[HKLOG] original main version = " + main_version);
            if (main_version > 3)
                pythonVersion = "python3";
        } catch (Exception e) {
            e.printStackTrace();
        }

        env = new String[] {
                "CASSANDRA_HOME=\"" + cassandraHome + "\"",
                "CASSANDRA_CONF=\"" + cassandraConf + "\"", javaToolOpts,
                "cassandra.broadcast_interval_ms=100",
                "cassandra.ring_delay_ms=100",
                "CQLSH_DAEMON_PORT=\"" + cqlshDaemonPort + "\"",
                "PYTHON=" + pythonVersion };

        setEnvironment();

        // Copy the cassandra-ori.yaml and cassandra-up.yaml
        if (configPath != null) {
            copyConfig(configPath);
        }
        return true;
    }

    @Override
    public void flush() throws Exception {
        if (Config.getConf().originalVersion.contains("2.1.0")) {
            Process flushCass = this.runInContainer(new String[] {
                    "/" + system + "/" + originalVersion + "/"
                            + "bin/nodetool",
                    "-h", "::FFFF:127.0.0.1",
                    "drain" });
            flushCass.waitFor();
        } else {
            Process flushCass = this.runInContainer(new String[] {
                    "/" + system + "/" + originalVersion + "/"
                            + "bin/nodetool",
                    "drain" });
            flushCass.waitFor();
        }
    }

    @Override
    public void upgrade() throws Exception {
        prepareUpgradeEnv();
        String restartCommand = "supervisorctl restart upfuzz_cassandra:";
        Process restart = runInContainer(
                new String[] { "/bin/bash", "-c", restartCommand }, env);
        int ret = restart.waitFor();
        String message = Utilities.readProcess(restart);
        logger.debug("upgrade version start: " + ret + "\n" + message);
        cqlsh = new CassandraCqlshDaemon(getNetworkIP(), cqlshDaemonPort, this);
    }

    @Override
    public void upgradeFromCrash() throws Exception {
        prepareUpgradeEnv();
        restart();
    }

    public void prepareUpgradeEnv() throws IOException {
        type = "upgraded";
        String cassandraHome = "/cassandra/" + upgradedVersion;
        String cassandraConf = "/etc/" + upgradedVersion;
        javaToolOpts = "JAVA_TOOL_OPTIONS=\"-javaagent:"
                + "/org.jacoco.agent.rt.jar"
                + "=append=false"
                + ",includes=" + includes + ",excludes=" + excludes +
                ",output=dfe,address=" + hostIP + ",port=" + agentPort +
                ",sessionid=" + system + "-" + executorID + "_" + type +
                "-" + index +
                "\"";
        cqlshDaemonPort ^= 1;

        String pythonVersion = "python2";
        String[] spStrings = upgradedVersion.split("-");
        try {
            int main_version = Integer
                    .parseInt(spStrings[spStrings.length - 1].substring(0, 1));
            if (main_version > 3)
                pythonVersion = "python3";
        } catch (Exception e) {
            e.printStackTrace();
        }
        env = new String[] {
                "CASSANDRA_HOME=\"" + cassandraHome + "\"",
                "CASSANDRA_CONF=\"" + cassandraConf + "\"", javaToolOpts,
                "cassandra.broadcast_interval_ms=100",
                "cassandra.ring_delay_ms=100",
                "CQLSH_DAEMON_PORT=\"" + cqlshDaemonPort + "\"",
                "PYTHON=" + pythonVersion };
        setEnvironment();
    }

    @Override
    public void downgrade() throws Exception {
        type = "original";
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
        cqlshDaemonPort ^= 1;

        String pythonVersion = "python2";
        String[] spStrings = originalVersion.split("-");
        try {
            int main_version = Integer
                    .parseInt(spStrings[spStrings.length - 1].substring(0, 1));
            logger.debug("[HKLOG] original main version = " + main_version);
            if (main_version > 3)
                pythonVersion = "python3";
        } catch (Exception e) {
            e.printStackTrace();
        }

        env = new String[] {
                "CASSANDRA_HOME=\"" + cassandraHome + "\"",
                "CASSANDRA_CONF=\"" + cassandraConf + "\"", javaToolOpts,
                "cassandra.broadcast_interval_ms=100",
                "cassandra.ring_delay_ms=100",
                "CQLSH_DAEMON_PORT=\"" + cqlshDaemonPort + "\"",
                "PYTHON=" + pythonVersion };

        setEnvironment();

        String restartCommand = "supervisorctl restart upfuzz_cassandra:";
        // TODO remove the env arguments, we already have /usr/bin/set_env
        Process restart = runInContainer(
                new String[] { "/bin/bash", "-c", restartCommand }, env);
        int ret = restart.waitFor();
        String message = Utilities.readProcess(restart);
        logger.debug("downgrade version start: " + ret + "\n" + message);
        cqlsh = new CassandraCqlshDaemon(getNetworkIP(), cqlshDaemonPort, this);
    }

    @Override
    public void shutdown() {
        String[] stopNode = new String[] {
                "/" + system + "/" + originalVersion + "/"
                        + "bin/nodetool",
                "stopdaemon" };
        int ret = runProcessInContainer(stopNode);
        logger.debug("cassandra shutdown ret = " + ret);
    }

    @Override
    public boolean clear() {
        int ret = runProcessInContainer(new String[] {
                "rm", "-rf", "/var/lib/cassandra/*"
        });
        logger.debug("cassandra clear data ret = " + ret);
        return true;
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

    // add the configuration test files

    static String template = ""
            + "    ${serviceName}:\n"
            + "        container_name: cassandra-${originalVersion}_${upgradedVersion}_${executorID}_N${index}\n"
            + "        image: upfuzz_${system}:${originalVersion}_${upgradedVersion}\n"
            + "        command: bash -c 'sleep 0 && /usr/bin/supervisord'\n"
            + "        networks:\n"
            + "            ${networkName}:\n"
            + "                ipv4_address: ${networkIP}\n"
            + "        volumes:\n"
            // + " - ./persistent/node_${index}/data:/var/lib/cassandra\n"
            + "            - ./persistent/node_${index}/log:/var/log/cassandra\n"
            + "            - ./persistent/node_${index}/env.sh:/usr/bin/set_env\n"
            + "            - ./persistent/node_${index}/consolelog:/var/log/supervisor\n"
            + "            - ./persistent/config:/test_config\n"
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
        Map<String, String> stateValues = new HashMap<>();
        // Cassandra do not distinguish nodes
        // HDFS might get state from different nodes
        for (String stateName : targetSystemStates) {
            Path filePath = Paths.get("/var/log/cassandra/system.log");
            String target = String.format("\\[InconsistencyDetector\\]\\[%s\\]",
                    stateName);
            String[] grepStateCmd = new String[] {
                    "/bin/sh", "-c",
                    "grep -a \"" + target + "\" " + filePath
                            + " | tail -n 1"
            };
            try {
                System.out.println("\n\n");
                Process grepProc = runInContainer(grepStateCmd);
                String result = new String(
                        grepProc.getInputStream().readAllBytes());
                String stateValue = "";
                if (!result.isEmpty()) {
                    int index = result.indexOf("=");
                    if (index != -1) {
                        stateValue = result.substring(index + 1);
                    }
                }
                logger.info(String.format("State [%s] =  %s", stateName,
                        stateValue));
                stateValues.put(stateName, Utilities.encodeString(stateValue));
            } catch (IOException e) {
                logger.error(String.format(
                        "Problem when reading state in docker[%d]", index));
                e.printStackTrace();
            }
        }
        return stateValues;
    }

    @Override
    public LogInfo grepLogInfo() {
        LogInfo logInfo = new LogInfo();
        Path filePath = Paths.get("/var/log/cassandra/system.log");

        constructLogInfo(logInfo, filePath);
        return logInfo;
    }

}
