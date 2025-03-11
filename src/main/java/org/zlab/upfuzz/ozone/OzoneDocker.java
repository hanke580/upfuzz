package org.zlab.upfuzz.ozone;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.text.StringSubstitutor;
import org.zlab.upfuzz.docker.Docker;
import org.zlab.upfuzz.docker.DockerCluster;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.fuzzingengine.LogInfo;
import org.zlab.upfuzz.utils.Utilities;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class OzoneDocker extends Docker {

    protected final Logger logger = LogManager.getLogger(getClass());

    public static final int SAFEMODE_WAIT_INTERVAL = 30; // s
    String composeYaml;
    String javaToolOpts;
    int ozoneDaemonPort = 11116;
    public int direction;

    public String omNodeIP;

    public OzoneShellDaemon ozoneShell;

    public OzoneDocker(OzoneDockerCluster dockerCluster, int index) {
        this.index = index;
        this.direction = dockerCluster.direction;
        type = "original";
        workdir = dockerCluster.workdir;
        system = dockerCluster.system;
        originalVersion = (dockerCluster.direction == 0)
                ? dockerCluster.originalVersion
                : dockerCluster.upgradedVersion;
        upgradedVersion = (dockerCluster.direction == 0)
                ? dockerCluster.upgradedVersion
                : dockerCluster.originalVersion;
        networkName = dockerCluster.networkName;
        subnet = dockerCluster.subnet;
        hostIP = dockerCluster.hostIP;
        networkIP = DockerCluster.getKthIP(hostIP, index);
        omNodeIP = dockerCluster.omNodeIP;
        agentPort = dockerCluster.agentPort;
        includes = dockerCluster.includes;
        excludes = dockerCluster.excludes;
        executorID = dockerCluster.executorID;
        serviceName = "DC3N" + index; // Remember update the service name

        collectFormatCoverage = dockerCluster.collectFormatCoverage;
        configPath = dockerCluster.configpath;
        if (Config.getConf().testSingleVersion)
            containerName = "ozone-" + originalVersion + "_" + executorID + "_N"
                    + index;
        else
            containerName = "ozone-" + originalVersion + "_" + upgradedVersion +
                    "_" + executorID + "_N" + index;
    }

    public String formatComposeYaml() {
        Map<String, String> formatMap = new HashMap<>();
        formatMap.put("projectRoot", System.getProperty("user.dir"));
        formatMap.put("system", system);
        formatMap.put("originalVersion", originalVersion);
        formatMap.put("upgradedVersion", upgradedVersion);
        formatMap.put("index", Integer.toString(index));
        formatMap.put("networkName", networkName);
        formatMap.put("JAVA_TOOL_OPTIONS", javaToolOpts);
        formatMap.put("subnet", subnet);
        formatMap.put("omNodeIP", omNodeIP);
        formatMap.put("networkIP", networkIP);
        formatMap.put("agentPort", Integer.toString(agentPort));
        formatMap.put("formatCoveragePort",
                Integer.toString(Config.instance.formatCoveragePort));
        formatMap.put("executorID", executorID);
        StringSubstitutor sub = new StringSubstitutor(formatMap);
        this.composeYaml = sub.replace(template);

        return composeYaml;
    }

    @Override
    public void teardown() {
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
    public boolean build() throws IOException {
        type = (direction == 0) ? "original" : "upgraded";
        String ozoneHome = "/ozone/" + originalVersion;
        String ozoneConf = "/etc/" + originalVersion + "/etc/hadoop";

        javaToolOpts = "JAVA_TOOL_OPTIONS=\"-javaagent:"
                + "/org.jacoco.agent.rt.jar"
                + "=append=false"
                + ",includes=" + includes + ",excludes=" + excludes +
                ",output=dfe,address=" + hostIP + ",port=" + agentPort +
                // ",weights=" + ozoneHome + "/diff_func.txt" +
                ",sessionid=" + system + "-" + executorID + "_"
                + type + "-" + index +
                "\"";

        env = new String[] {
                "OZONE_HOME=" + ozoneHome,
                "OZONE_CONF_DIR=" + ozoneConf, javaToolOpts,
                "OZONE_SHELL_DAEMON_PORT=\"" + ozoneDaemonPort + "\"",
                "PYTHON=python3",
                "ENABLE_FORMAT_COVERAGE=" + (Config.getConf().useFormatCoverage
                        && collectFormatCoverage),
                "ENABLE_NET_COVERAGE=" + Config.getConf().useTrace
        };
        setEnvironment();

        // Copy the cassandra-ori.yaml and cassandra-up.yaml
        if (configPath != null) {
            copyConfig(configPath, direction);
        }
        return false;
    }

    @Override
    public void shutdown() {

        String nodeType;
        if (index == 0) {
            nodeType = "scm";
        } else if (index == 1) {
            nodeType = "om";
        }
        // else if (index == 2) {
        // nodeType = "recon";
        // }
        else {
            nodeType = "datanode";
        }

        String curVersion = type.equals("upgraded") ? upgradedVersion
                : originalVersion;
        String oriOzoneDaemonPath = "/" + system + "/" + curVersion + "/"
                + "bin/ozone";

        String[] stopNode = new String[] { oriOzoneDaemonPath, "--daemon",
                "stop",
                nodeType };

        int ret = runProcessInContainer(stopNode, env);
        logger.info("daemon stopped ret = " + ret);
        logger.debug("shutdown " + nodeType);
    }

    static String template = ""
            + "    DC3N${index}:\n"
            + "        container_name: ozone-${originalVersion}_${upgradedVersion}_${executorID}_N${index}\n"
            + "        image: upfuzz_${system}:${originalVersion}_${upgradedVersion}\n"
            + "        command: bash -c 'sleep 0 && /usr/bin/supervisord'\n"
            + "        networks:\n"
            + "            ${networkName}:\n"
            + "                ipv4_address: ${networkIP}\n"
            + "        volumes:\n"
            // + " -
            // ./persistent/node_${index}/nndata:/var/hadoop/data/nameNode\n"
            // + " -
            // ./persistent/node_${index}/dndata:/var/hadoop/data/dataNode\n"
            + "            - ./persistent/node_${index}/log:/var/log/ozone\n"
            + "            - ./persistent/node_${index}/env.sh:/usr/bin/set_env\n"
            + "            - ./persistent/node_${index}/consolelog:/var/log/supervisor\n"
            + "            - ./persistent/config:/test_config\n"
            + "            - /tmp/upfuzz/ozone:/tmp/upfuzz/ozone\n"
            + "            - ${projectRoot}/prebuild/${system}/${originalVersion}:/${system}/${originalVersion}\n"
            + "            - ${projectRoot}/prebuild/${system}/${upgradedVersion}:/${system}/${upgradedVersion}\n"
            + "        environment:\n"
            + "            - OZONE_CLUSTER_NAME=dev_cluster\n"
            + "            - namenodeIP=${omNodeIP},\n"
            + "            - OZONE_LOGGING_LEVEL=DEBUG\n"
            + "            - OZONE_SHELL_HOST=${networkIP}\n"
            + "            - OZONE_LOG_DIR=/var/log/ozone\n"
            + "            - JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/\n"
            + "        expose:\n"
            + "            - ${agentPort}\n"
            + "            - 9858\n"
            + "            - 9860\n"
            + "            - 9861\n"
            + "            - 9864\n"
            + "            - 9874\n"
            + "            - 9876\n"
            + "            - 9878\n"
            + "            - 9882\n"
            + "            - 9888\n"
            + "            - 9891\n"
            + "            - 9892\n"
            + "        ulimits:\n"
            + "            memlock: -1\n"
            + "            nproc: 32768\n"
            + "            nofile: 100000\n";

    @Override
    public void chmodDir() throws IOException, InterruptedException {
        runInContainer(
                new String[] { "chmod", "-R", "777", "/var/ozone/data" });
        runInContainer(
                new String[] { "chmod", "-R", "777", "/var/log/ozone" });
        runInContainer(
                new String[] { "chmod", "-R", "777", "/var/log/supervisor" });
        runInContainer(
                new String[] { "chmod", "-R", "777", "/usr/bin/set_env" });
    }

    @Override
    public void flush() throws Exception {
    }

    @Override
    public void restart() throws Exception {
        String[] containerRecoverCMD = new String[] {
                "docker", "compose", "restart", serviceName
        };
        Process containerRecoverProcess = Utilities.exec(
                containerRecoverCMD,
                workdir);
        containerRecoverProcess.waitFor();

        // recreate connection
        start();

        waitSafeModeInterval();
        logger.info(
                String.format("Node%d restart successfully!", index));
    }

    public void prepareUpgradeEnv() throws IOException {
        type = "upgraded";
        String ozoneHome = "/ozone/" + upgradedVersion;
        String ozoneConf = "/etc/" + upgradedVersion + "/etc/hadoop";

        javaToolOpts = "JAVA_TOOL_OPTIONS=\"-javaagent:"
                + "/org.jacoco.agent.rt.jar"
                + "=append=false"
                + ",includes=" + includes + ",excludes=" + excludes +
                ",output=dfe,address=" + hostIP + ",port=" + agentPort +
                // ",weights=" + ozoneHome + "/diff_func.txt" +
                ",sessionid=" + system + "-" + executorID + "_"
                + type + "-" + index +
                "\"";

        // ozoneDaemonPort ^= 1;
        env = new String[] {
                "OZONE_HOME=" + ozoneHome,
                "OZONE_CONF_DIR=" + ozoneConf, javaToolOpts,
                "OZONE_SHELL_DAEMON_PORT=\"" + ozoneDaemonPort + "\"",
                "PYTHON=python3",
                "ENABLE_FORMAT_COVERAGE=false",
                "ENABLE_NET_COVERAGE=" + Config.getConf().useTrace,
                "JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/\n"
        };
        setEnvironment();
    }

    public void waitSafeModeInterval() {
        // For NN, if it's a rolling upgrade, add a safemode waiting time for 30
        // seconds.
        // This might be changed if HA is enabled.
        if (index == 0) {
            logger.info("NN waiting for safemode interval for 30s");
            try {
                Thread.sleep(SAFEMODE_WAIT_INTERVAL * 1000);
            } catch (InterruptedException ignored) {
            }
        }
    }

    public void upgradeSCM() throws Exception {
        prepareUpgradeEnv();
        String restartCommand = "/usr/bin/supervisorctl restart upfuzz_ozone:";
        // Seems the env doesn't really matter...
        Process restart = runInContainer(
                new String[] { "/bin/bash", "-c", restartCommand });
        int ret = restart.waitFor();
        String message = Utilities.readProcess(restart);
    }

    public void startSCMShell() throws Exception {
        // logger.debug("upgrade version start: " + ret + "\n" + message);
        ozoneShell = new OzoneShellDaemon(getNetworkIP(), ozoneDaemonPort,
                executorID, this);
    }

    @Override
    public void upgrade() throws Exception {
        prepareUpgradeEnv();
        String restartCommand = "/usr/bin/supervisorctl restart upfuzz_ozone:";
        // Seems the env doesn't really matter...
        Process restart = runInContainer(
                new String[] { "/bin/bash", "-c", restartCommand });
        int ret = restart.waitFor();
        String message = Utilities.readProcess(restart);
        logger.debug("upgrade version start: " + ret + "\n" + message);
        ozoneShell = new OzoneShellDaemon(getNetworkIP(), ozoneDaemonPort,
                executorID, this);
        // waitSafeModeInterval();
    }

    @Override
    public void upgradeFromCrash() throws Exception {
        prepareUpgradeEnv();
        restart();
        waitSafeModeInterval();
    }

    @Override
    public int start() throws Exception {
        // Connect to the Ozone daemon
        ozoneShell = new OzoneShellDaemon(getNetworkIP(), ozoneDaemonPort,
                executorID, this);
        return 0;
    }

    @Override
    public String getNetworkIP() {
        return networkIP;
    }

    @Override
    public void downgrade() throws Exception {
        type = "original";
        String ozoneHome = "/ozone/" + originalVersion;
        String ozoneConf = "/etc/" + originalVersion + "/etc/hadoop";

        javaToolOpts = "JAVA_TOOL_OPTIONS=\"-javaagent:"
                + "/org.jacoco.agent.rt.jar"
                + "=append=false"
                + ",includes=" + includes + ",excludes=" + excludes +
                ",output=dfe,address=" + hostIP + ",port=" + agentPort +
                // ",weights=" + ozoneHome + "/diff_func.txt" +
                ",sessionid=" + system + "-" + executorID + "_"
                + type + "-" + index +
                "\"";

        // ozoneDaemonPort ^= 1;
        env = new String[] {
                "OZONE_HOME=" + ozoneHome,
                "OZONE_CONF_DIR=" + ozoneConf, javaToolOpts,
                "OZONE_SHELL_DAEMON_PORT=\"" + ozoneDaemonPort + "\"",
                "PYTHON=python3",
                "ENABLE_FORMAT_COVERAGE=false",
                "ENABLE_NET_COVERAGE=" + Config.getConf().useTrace,
                "JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/\n"
        };
        setEnvironment();
        String restartCommand = "/usr/bin/supervisorctl restart upfuzz_ozone:";
        // Seems the env doesn't really matter...
        Process restart = runInContainer(
                new String[] { "/bin/bash", "-c", restartCommand });
        int ret = restart.waitFor();
        String message = Utilities.readProcess(restart);
        logger.debug(
                "downgrade to original version start: " + ret + "\n" + message);
        ozoneShell = new OzoneShellDaemon(getNetworkIP(), ozoneDaemonPort,
                executorID, this);
        waitSafeModeInterval();
    }

    @Override
    public boolean clear() {
        try {
            runInContainer(new String[] {
                    "rm", "-rf", "/var/hadoop/data/*"
            });
        } catch (IOException e) {
            logger.error(e);
            // FIXME: remove this line after debugging
            System.exit(1);
        }
        return true;
    }

    @Override
    public LogInfo grepLogInfo(Set<String> blackListErrorLog) {
        LogInfo logInfo = new LogInfo();
        Path filePath = Paths.get("/var/log/ozone/*.log");

        constructLogInfo(logInfo, filePath, blackListErrorLog);
        return logInfo;
    }
}
