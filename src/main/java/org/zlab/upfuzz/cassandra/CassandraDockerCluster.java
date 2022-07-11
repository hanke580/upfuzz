package org.zlab.upfuzz.cassandra;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.text.StringSubstitutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.docker.DockerCluster;
import org.zlab.upfuzz.docker.DockerMeta;
import org.zlab.upfuzz.docker.IDocker;
import org.zlab.upfuzz.docker.IDockerCluster;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.fuzzingengine.executor.Executor;
import org.zlab.upfuzz.utils.Utilities;

public class CassandraDockerCluster implements IDockerCluster {
    static Logger logger = LogManager.getLogger(CassandraDockerCluster.class);

    CassandraDocker[] dockers;

    String version;
    String originalVersion;
    String upgradedVersion;

    Executor executor;
    String executorID;
    String system;
    String type;
    String subnet;
    String seedIP;
    int agentPort;
    int subnetID;
    String networkName;
    String composeYaml;
    String hostIP;
    File workdir;

    static final String inclueds = "org.apache.cassandra.*";
    static final String excludes = "org.apache.cassandra.metrics.*:org.apache.cassandra.net.*:org.apache.cassandra.io.sstable.format.SSTableReader.*:org.apache.cassandra.service.*";

    CassandraDockerCluster(CassandraExecutor executor, String type,
            String version,
            int nodeNum) {
        this.networkName = MessageFormat.format(
                "network_{0}_{1}_to_{2}_{3}",
                executor.systemID, Config.getConf().originalVersion,
                Config.getConf().upgradedVersion,
                UUID.randomUUID());

        // replace subnet
        // rename services

        // 192.168.24.[(0001~1111)|0000] / 28
        //
        this.subnetID = RandomUtils.nextInt(1, 256);
        this.subnet = "192.168." + Integer.toString(subnetID) + ".1/24";
        this.hostIP = "192.168." + Integer.toString(subnetID) + ".1";
        this.seedIP = DockerCluster.getKthIP(hostIP, 0);
        this.agentPort = executor.agentPort;
        this.executor = executor;
        this.executorID = executor.executorID;
        this.version = version;
        this.type = type;
        this.originalVersion = Config.getConf().originalVersion;
        this.upgradedVersion = Config.getConf().upgradedVersion;
        this.system = executor.systemID;
        this.dockers = new CassandraDocker[nodeNum];
    }

    public boolean build() {
        for (int i = 0; i < dockers.length; ++i) {
            dockers[i] = new CassandraDocker(this, i);
        }
        dockers[0].build();
        return true;
    }

    public int start() {
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        String executorTimestamp = formatter.format(executor.timestamp);
        String dockerTimestamp = formatter.format(System.currentTimeMillis());

        workdir = new File(
                "fuzzing_storage/" + executor.systemID + "/" + originalVersion
                        + "/" +
                        upgradedVersion + "/" + executorTimestamp + "-"
                        + executor.executorID + "/" + dockerTimestamp
                        + "/" + type);

        File composeFile = new File(workdir, "docker-compose.yaml");
        if (!workdir.exists()) {
            workdir.mkdirs();
        }

        try {
            formatComposeYaml();
            composeFile.createNewFile();
            BufferedWriter writer = new BufferedWriter(
                    new FileWriter(composeFile));
            writer.write(composeYaml);
            writer.close();
            Process buildProcess = Utilities.exec(
                    new String[] { "docker-compose", "up", "-d" }, workdir);
            int ret = buildProcess.waitFor();
            if (ret == 0) {
                logger.info("docker-compose up " + workdir);
            } else {
                String errorMessage = Utilities.readProcess(buildProcess);
                logger.error("docker-compose up\n" + errorMessage);
                System.exit(ret);
            }
            for (int i = 0; i < dockers.length; ++i) {
                dockers[i].start();
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

    public Path getDataPath() {
        return Paths.get(workdir.toString(), "/persistent/data/n1data");
    }

    @Override
    public String getNetworkIP() {
        return hostIP;
    }

    @Override
    public IDocker getDocker(int i) {
        return dockers[i];
    }

    static String template = ""
            + "version: '3'\n"
            + "services:\n"
            + "\n"
            + "${dockers}"
            + "\n"
            + "networks:\n"
            + "    ${networkName}:\n"
            + "        driver: bridge\n"
            + "        ipam:\n"
            + "            driver: default\n"
            + "            config:\n"
            + "                - subnet: ${subnet}\n";

    private void formatComposeYaml() {
        Map<String, String> formatMap = new HashMap<>();

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < dockers.length; ++i) {
            sb.append(dockers[i].formatComposeYaml());
        }
        String dockersFormat = sb.toString();
        formatMap.put("dockers", dockersFormat);
        formatMap.put("subnet", subnet);
        formatMap.put("networkName", networkName);
        StringSubstitutor sub = new StringSubstitutor(formatMap);
        this.composeYaml = sub.replace(template);
    }

}
