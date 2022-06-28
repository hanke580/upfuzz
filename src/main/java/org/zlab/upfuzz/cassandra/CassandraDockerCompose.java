package org.zlab.upfuzz.cassandra;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.text.StringSubstitutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.docker.DockerBuilder;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.utils.Utilities;

public class CassandraDockerCompose implements DockerBuilder {
    static Logger logger = LogManager.getLogger(CassandraDockerCompose.class);
    static String template = ""
            + "version: '3'\n"
            + "services:\n"
            + "\n"
            + "    DC3N1:\n"
            + "        container_name: cassandra-3.11.13_${executorID}_N1\n"
            + "        image: image_cassandra_cassandra-3.11.13-compile\n"
            + "        command: bash -c 'sleep 0 && /usr/bin/supervisord'\n"
            + "        networks:\n"
            + "            network_cassandra-3.11.13_to_cassandra-4.0-alpha1_stressTest_dc1ring:\n"
            + "                ipv4_address: ${originalClusterIP}\n"
            + "        volumes:\n"
            + "            - ./persistent/data/n1data:/var/lib/cassandra\n"
            + "            - ./persistent/log/n1log:/var/log/cassandra\n"
            + "            - ./persistent/consolelog/n1consolelog:/var/log/supervisor\n"
            + "        environment:\n"
            + "            - CASSANDRA_CONFIG=/cassandra/conf\n"
            + "            - CASSANDRA_CLUSTER_NAME=dev_cluster\n"
            + "            - CASSANDRA_SEEDS=${originalClusterIP},\n"
            + "            - CASSANDRA_LOGGING_LEVEL=DEBUG\n"
            + "            - CQLSH_HOST=${originalClusterIP}\n"
            + "            - ${JAVA_TOOL_OPTIONS_ORIGINAL}\n"
            + "        expose:\n"
            + "            - 6300\n"
            + "            - 7000\n"
            + "            - 7001\n"
            + "            - 7199\n"
            + "            - 9042\n"
            + "            - 9160\n"
            + "            - 18251\n"
            + "        ulimits:\n"
            + "            memlock: -1\n"
            + "            nproc: 32768\n"
            + "            nofile: 100000\n"
            + "\n"
            + "    DC3N2:\n"
            + "        container_name: cassandra-4.0_${executorID}_N2\n"
            + "        image: image_cassandra_cassandra-3.11.13-compile\n"
            + "        command: bash -c 'sleep 0 && /usr/bin/supervisord'\n"
            + "        networks:\n"
            + "            network_cassandra-3.11.13_to_cassandra-4.0-alpha1_stressTest_dc1ring:\n"
            + "                ipv4_address: ${upgradedClusterIP}\n"
            + "        volumes:\n"
            + "            - ./persistent/data/n2data:/var/lib/cassandra\n"
            + "            - ./persistent/log/n2log:/var/log/cassandra\n"
            + "            - ./persistent/consolelog/n2consolelog:/var/log/supervisor\n"
            + "        environment:\n"
            + "            - CASSANDRA_CONFIG=/cassandra/conf\n"
            + "            - CASSANDRA_CLUSTER_NAME=dev_cluster\n"
            + "            - CASSANDRA_SEEDS=${originalClusterIP},\n"
            + "            - CASSANDRA_LOGGING_LEVEL=DEBUG\n"
            + "            - CQLSH_HOST=${upgradedClusterIP}\n"
            + "            - ${JAVA_TOOL_OPTIONS_ORIGINAL}\n"
            + "        depends_on:\n"
            + "                - DC3N1\n"
            + "        expose:\n"
            + "            - 6300\n"
            + "            - 7000\n"
            + "            - 7001\n"
            + "            - 7199\n"
            + "            - 9042\n"
            + "            - 9160\n"
            + "            - 18251\n"
            + "        ulimits:\n"
            + "            memlock: -1\n"
            + "            nproc: 32768\n"
            + "            nofile: 100000\n"
            + "\n"
            + "networks:\n"
            + "    network_cassandra-3.11.13_to_cassandra-4.0-alpha1_stressTest_dc1ring:\n"
            + "        driver: bridge\n"
            + "        ipam:\n"
            + "            driver: default\n"
            + "            config:\n"
            + "                - subnet: ${subnet}\n";

    String systemID;
    String executorID;
    String originalVersion;
    String upgradedVersion;

    String subnet;
    int subnetID;
    String composeYaml;
    String hostIP;
    String originalClusterIP;
    String upgradedClusterIP;
    File workdir;

    static final String inclueds = "org.apache.cassandra.*";
    static final String excludes = "org.apache.cassandra.metrics.*:org.apache.cassandra.net.*:org.apache.cassandra.io.sstable.format.SSTableReader.*:org.apache.cassandra.service.*";

    CassandraDockerCompose(CassandraExecutor executor) {
        // TODO update docker-compose template
        // replace subnet
        // rename services

        // 192.168.24.[(0001~1111)|0000] / 28
        //
        this.subnetID = RandomUtils.nextInt(1, 256);
        this.subnet = "192.168." + Integer.toString(subnetID) + ".1/24";
        this.hostIP = "192.168." + Integer.toString(subnetID) + ".1";
        this.originalClusterIP = "192.168." + Integer.toString(subnetID)
                + ".2";
        this.upgradedClusterIP = "192.168." + Integer.toString(subnetID)
                + ".3";
        this.systemID = executor.systemID;
        this.executorID = executor.executorID;
        this.originalVersion = Config.getConf().originalVersion;
        this.upgradedVersion = Config.getConf().upgradedVersion;

        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        String timestamp = formatter.format(System.currentTimeMillis());

        workdir = new File(
                "fuzzing_storage/" + systemID + "/" + originalVersion + "/" +
                        upgradedVersion + "/" + timestamp);

        formatComposeYaml();
    }

    private void formatComposeYaml() {
        Map<String, String> variableMap = new HashMap<>();
        String javaToolOptsOri = "JAVA_TOOL_OPTIONS=-javaagent:"
                + "/org.jacoco.agent.rt.jar" + "=append=false"
                + ",includes=" + inclueds +
                ",excludes=" + excludes +
                ",output=dfe,address=" + hostIP + ",sessionid=" + systemID + "-"
                +
                executorID + "_original";

        String javaToolOptsUpg = "JAVA_TOOL_OPTIONS=-javaagent:"
                + "/org.jacoco.agent.rt.jar" + "=append=false"
                + ",includes=" + inclueds +
                ",excludes=" + excludes +
                ",output=dfe,address=" + hostIP + ",sessionid=" + systemID + "-"
                +
                executorID + "_upgraded";

        variableMap.put("JAVA_TOOL_OPTIONS_ORIGINAL", javaToolOptsOri);
        variableMap.put("JAVA_TOOL_OPTIONS_UPGRADED", javaToolOptsUpg);
        variableMap.put("subnet", subnet);
        variableMap.put("executorID", executorID);
        variableMap.put("originalClusterIP", originalClusterIP);
        variableMap.put("upgradedClusterIP", upgradedClusterIP);
        StringSubstitutor sub = new StringSubstitutor(variableMap);
        this.composeYaml = sub.replace(template);
    }

    public boolean buildDocker() {
        URL pyScript = CassandraDockerCompose.class.getClassLoader()
                .getResource("build.py");
        String pyScriptPath = pyScript.getPath();
        File scriptPath = Paths.get(pyScriptPath).getParent().toFile();
        try {
            logger.info("Build Dockerfile " + systemID + " " + originalVersion);
            Process buildProcess = Utilities.exec(
                    new String[] { "python3", pyScriptPath, systemID,
                            originalVersion },
                    scriptPath);
            int exit = buildProcess.waitFor();
            if (exit == 0) {
                logger.info("Build docker succeed.");
            } else {
                String errorMessage = Utilities.readProcess(buildProcess);
                logger.error("Build docker failed\n" + errorMessage);
            }
            return (exit == 0);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

    public int start() {

        File composeFile = new File(workdir, "docker-compose.yaml");
        if (!workdir.exists()) {
            workdir.mkdirs();
        }

        try {
            composeFile.createNewFile();
            BufferedWriter writer = new BufferedWriter(
                    new FileWriter(composeFile));
            writer.write(composeYaml);
            writer.close();
            Process buildProcess = Utilities.exec(
                    new String[] { "docker-compose", "up", "-d" }, workdir);
            int ret = buildProcess.waitFor();
            if (ret == 0) {
                logger.info("docker-compose up");
            } else {
                String errorMessage = Utilities.readProcess(buildProcess);
                logger.error("docker-compose up\n" + errorMessage);
                System.exit(ret);
            }
            return ret;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public void teardown() {
        try {
            Process buildProcess = Utilities.exec(
                    new String[] { "docker-compose", "down" }, workdir);
            int ret = buildProcess.waitFor();
            logger.info("teardown docker-compose in " + workdir);
        } catch (IOException | InterruptedException e) {
            logger.error("failed to teardown docker", e);
        }

    }

    @Override
    public String getHostIP() {
        return hostIP;
    }

    @Override
    public String originalClusterIP() {
        return originalClusterIP;
    }

    @Override
    public String upgradedClusterIP() {
        return upgradedClusterIP;
    }
}
