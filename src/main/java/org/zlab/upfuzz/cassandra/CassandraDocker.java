package org.zlab.upfuzz.cassandra;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.text.StringSubstitutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.docker.DockerCluster;
import org.zlab.upfuzz.docker.DockerMeta;
import org.zlab.upfuzz.docker.IDocker;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.utils.Utilities;

public class CassandraDocker extends DockerMeta implements IDocker {
    protected final Logger logger = LogManager.getLogger(getClass());

    String composeYaml;
    String javaToolOpts;
    int cqlshDaemonPort = 18251;

    public CassandraCqlshDaemon cqlsh;

    public CassandraDocker(CassandraDockerCluster dockerCluster, int index)
            throws IOException {
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
        includes = dockerCluster.inclueds;
        excludes = dockerCluster.excludes;
        executorID = dockerCluster.executorID;
        name = "cassandra-" + originalVersion + "_" + upgradedVersion + "_" +
                executorID + "_N" + index;
    }

    @Override
    public String getNetworkIP() {
        return networkIP;
    }

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
        StringSubstitutor sub = new StringSubstitutor(formatMap);
        this.composeYaml = sub.replace(template);

        return composeYaml;
    }

    @Override
    public int start() throws IOException, InterruptedException {
        // String dir = "/" + system + "/" + originalVersion;
        // Process cass = exec(new String[] { "-e", "CASSANDRA_HOME=" + dir,
        // name,
        // dir + "/bin/cqlsh_init.sh" });
        // int res = cass.waitFor();
        // String log = Utilities.readProcess(cass);
        // logger.debug("start cassandra docker " + index + ": " + dir +
        // " ret: " + res + "\n" + log);
        //
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
        for (int i = 0; i < env.length; ++i) {
            fw.write("export " + env[i] + "\n");
        }
        fw.close();
    }

    @Override
    public void teardown() {
    }

    @Override
    public boolean build() throws IOException {
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
        int ret = 0;

        logger.info("[HKLOG] FLUSING!");

        if (Config.getConf().originalVersion.contains("2.1.0")) {
            Process flushCass = runInContainer(new String[] {
                    "/" + system + "/" + originalVersion + "/"
                            + "bin/nodetool",
                    "-h", "::FFFF:127.0.0.1",
                    "flush" });
            ret = flushCass.waitFor();
        } else {
            Process flushCass = runInContainer(new String[] {
                    "/" + system + "/" + originalVersion + "/"
                            + "bin/nodetool",
                    "flush" });
            ret = flushCass.waitFor();
        }

        Process stopCass = runInContainer(new String[] {
                "/" + system + "/" + originalVersion + "/"
                        + "bin/nodetool",
                "stopdaemon" });
        ret = stopCass.waitFor();

        logger.debug("original version stop: " + ret);
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
        ret = restart.waitFor();
        String message = Utilities.readProcess(restart);
        logger.debug("original version restart: " + ret + "\n" + message);
        cqlsh = new CassandraCqlshDaemon(getNetworkIP(), cqlshDaemonPort,
                executorID);
    }

    @Override
    public Path getDataPath() {
        return Paths.get(workdir.toString(),
                "/persistent/node_" + index + "/data");
    }

    public Path getWorkPath() {
        return workdir.toPath();
    }

    public void chmodDir() throws IOException, InterruptedException {
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
            + "    DC3N${index}:\n"
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

}
