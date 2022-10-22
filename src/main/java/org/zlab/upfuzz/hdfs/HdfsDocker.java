package org.zlab.upfuzz.hdfs;

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

public class HdfsDocker extends Docker {
    protected final Logger logger = LogManager.getLogger(getClass());

    String composeYaml;
    String javaToolOpts;
    int hdfsDaemonPort = 18251;

    public String namenodeIP;

    public HDFSShellDaemon hdfsShell;

    public HdfsDocker(HdfsDockerCluster dockerCluster, int index)
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
        namenodeIP = dockerCluster.namenodeIP;
        agentPort = dockerCluster.agentPort;
        includes = dockerCluster.includes;
        excludes = dockerCluster.excludes;
        executorID = dockerCluster.executorID;
        name = "hdfs-" + originalVersion + "_" + upgradedVersion + "_" +
                executorID + "_N" + index;
        serviceName = "DC3N" + index; // Remember update the service name
    }

    @Override
    public String getNetworkIP() {
        return networkIP;
    }

    public String formatComposeYaml() {
        Map<String, String> formatMap = new HashMap<>();

        containerName = "hdfs-" + originalVersion + "_" + upgradedVersion +
                "_" + executorID + "_N" + index;
        formatMap.put("projectRoot", System.getProperty("user.dir"));
        formatMap.put("system", system);
        formatMap.put("originalVersion", originalVersion);
        formatMap.put("upgradedVersion", upgradedVersion);
        formatMap.put("index", Integer.toString(index));
        formatMap.put("networkName", networkName);
        formatMap.put("JAVA_TOOL_OPTIONS", javaToolOpts);
        formatMap.put("subnet", subnet);
        formatMap.put("namenodeIP", namenodeIP);
        formatMap.put("networkIP", networkIP);
        formatMap.put("agentPort", Integer.toString(agentPort));
        formatMap.put("executorID", executorID);
        StringSubstitutor sub = new StringSubstitutor(formatMap);
        this.composeYaml = sub.replace(template);

        return composeYaml;
    }

    @Override
    public int start() throws IOException, InterruptedException {
        // Connect to the HDFS daemon
        hdfsShell = new HDFSShellDaemon(getNetworkIP(), hdfsDaemonPort,
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
        String hdfsHome = "/hdfs/" + originalVersion;
        String hdfsConf = "/etc/" + originalVersion;
        javaToolOpts = "JAVA_TOOL_OPTIONS=\"-javaagent:"
                + "/org.jacoco.agent.rt.jar"
                + "=append=false"
                + ",includes=" + includes + ",excludes=" + excludes +
                ",output=dfe,address=" + hostIP + ",port=" + agentPort +
                ",sessionid=" + system + "-" + executorID + "_"
                + type + "-" + index +
                "\"";

        env = new String[] {
                "HADOOP_HOME=\"" + hdfsHome + "\"",
                "HDFS_CONF=\"" + hdfsConf + "\"", javaToolOpts,
                "HDFS_SHELL_DAEMON_PORT=\"" + hdfsDaemonPort + "\"",
                "PYTHON=python3" };
        setEnvironment();
        return false;
    }

    public void upgrade() throws Exception {
        logger.info("upgrading index " + index);

        type = "upgraded";
        javaToolOpts = "JAVA_TOOL_OPTIONS=\"-javaagent:"
                + "/org.jacoco.agent.rt.jar"
                + "=append=false"
                + ",includes=" + includes + ",excludes=" + excludes +
                ",output=dfe,address=" + hostIP + ",port=" + agentPort +
                ",sessionid=" + system + "-" + executorID + "_"
                + type + "-" + index +
                "\"";

        hdfsDaemonPort ^= 1;
        String hdfsHome = "/hdfs/" + upgradedVersion;
        String hdfsConf = "/etc/" + upgradedVersion;
        env = new String[] {
                "HADOOP_HOME=\"" + hdfsHome + "\"",
                "HDFS_CONF=\"" + hdfsConf + "\"", javaToolOpts,
                "HDFS_SHELL_DAEMON_PORT=\"" + hdfsDaemonPort + "\"",
                "PYTHON=python3" };
        setEnvironment();
        String restartCommand = "supervisorctl restart upfuzz_hdfs:";
        // Seems the env doesn't really matter...
        Process restart = runInContainer(
                new String[] { "/bin/bash", "-c", restartCommand });
        int ret = restart.waitFor();
        String message = Utilities.readProcess(restart);
        logger.debug("upgrade version start: " + ret + "\n" + message);
        hdfsShell = new HDFSShellDaemon(getNetworkIP(), hdfsDaemonPort,
                executorID);
    }

    @Override
    public boolean shutdown() {
        String nodeType;
        if (index == 0) {
            nodeType = "namenode";
        } else if (index == 1) {
            nodeType = "secondarynamenode";
        } else {
            nodeType = "datanode";
        }

        String orihadoopDaemonPath = "/" + system + "/" + originalVersion + "/"
                + "sbin/hadoop-daemon.sh";

        if (!nodeType.equals("secondarynamenode")) {
            // Secondary is stopped in a specific op (HDFSStopSNN)
            String[] stopNode = new String[] { orihadoopDaemonPath, "stop",
                    nodeType };
            int ret = runProcessInContainer(stopNode);
            if (ret != 0)
                return false;
        }
        return true;
    }

    @Override
    public Map<String, String> readSystemState() {
        return null;
    }

    @Override
    public Path getDataPath() {
        return Paths.get(workdir.toString(),
                "/persistent/node_" + index + "/nndata");
    }

    public Path getWorkPath() {
        return workdir.toPath();
    }

    static String template = ""
            + "    DC3N${index}:\n"
            + "        container_name: hdfs-${originalVersion}_${upgradedVersion}_${executorID}_N${index}\n"
            + "        image: upfuzz_${system}:${originalVersion}_${upgradedVersion}\n"
            + "        command: bash -c 'sleep 0 && /usr/bin/supervisord'\n"
            + "        networks:\n"
            + "            ${networkName}:\n"
            + "                ipv4_address: ${networkIP}\n"
            + "        volumes:\n"
            + "            - ./persistent/node_${index}/nndata:/var/hadoop/data/nameNode\n"
            + "            - ./persistent/node_${index}/dndata:/var/hadoop/data/dataNode\n"
            + "            - ./persistent/node_${index}/log:/var/log/hdfs\n"
            + "            - ./persistent/node_${index}/env.sh:/usr/bin/set_env\n"
            + "            - ./persistent/node_${index}/consolelog:/var/log/supervisor\n"
            + "            - ${projectRoot}/prebuild/${system}/${originalVersion}:/${system}/${originalVersion}\n"
            + "            - ${projectRoot}/prebuild/${system}/${upgradedVersion}:/${system}/${upgradedVersion}\n"
            + "        environment:\n"
            + "            - HDFS_CLUSTER_NAME=dev_cluster\n"
            + "            - namenodeIP=${namenodeIP},\n"
            + "            - HDFS_LOGGING_LEVEL=DEBUG\n"
            + "            - HDFS_SHELL_HOST=${networkIP}\n"
            + "            - HDFS_LOG_DIR=/var/log/hadoop\n"
            + "        expose:\n"
            + "            - ${agentPort}\n"
            + "            - 50020\n"
            + "            - 50010\n"
            + "            - 50075\n"
            + "            - 9000\n"
            + "            - 50070\n"
            + "            - 50090\n"
            + "        ulimits:\n"
            + "            memlock: -1\n"
            + "            nproc: 32768\n"
            + "            nofile: 100000\n";

    @Override
    public void chmodDir() throws IOException, InterruptedException {
        runInContainer(
                new String[] { "chmod", "-R", "777", "/var/hadoop/data" });
        runInContainer(
                new String[] { "chmod", "-R", "777", "/var/log/hdfs" });
        runInContainer(
                new String[] { "chmod", "-R", "777", "/var/log/supervisor" });
        runInContainer(
                new String[] { "chmod", "-R", "777", "/usr/bin/set_env" });
    }
}
