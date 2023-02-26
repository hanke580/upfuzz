package org.zlab.upfuzz.hbase;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.text.StringSubstitutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.docker.Docker;
import org.zlab.upfuzz.docker.DockerCluster;
import org.zlab.upfuzz.docker.DockerMeta;
import org.zlab.upfuzz.docker.IDocker;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.utils.Utilities;

public class HBaseDockerCluster extends DockerCluster {
    static Logger logger = LogManager.getLogger(HBaseDockerCluster.class);

    String seedIP;

    static final String includes = "org.apache.hadoop.hbase.*";
    static final String excludes = "";

    HBaseDockerCluster(HBaseExecutor executor, String version,
            int nodeNum) {
        super(executor, version, nodeNum, null);

        this.dockers = new HBaseDocker[nodeNum];
        this.extranodes = new HBaseHDFSDocker[1];
        this.seedIP = DockerCluster.getKthIP(hostIP, 0);
    }

    HBaseDockerCluster(HBaseExecutor executor, String version,
            int nodeNum, Set<String> targetSystemStates, Path configPath,
            Boolean exportComposeOnly) {
        super(executor, version, nodeNum, targetSystemStates,
                exportComposeOnly);

        this.dockers = new HBaseDocker[nodeNum];
        this.extranodes = new HBaseHDFSDocker[1];
        this.seedIP = DockerCluster.getKthIP(hostIP, 0);
        this.configpath = configPath;
    }

    public boolean build() throws Exception {
        for (int i = 0; i < dockers.length; ++i) {
            dockers[i] = new HBaseDocker(this, i);
            dockers[i].build();
        }
        extranodes[0] = new HBaseHDFSDocker(this, 100);
        extranodes[0].build();
        return true;
    }

    @Override
    public void refreshNetwork() {
        this.subnetID = RandomUtils.nextInt(1, 256);
        this.subnet = "192.168." + subnetID + ".1/24";
        this.hostIP = "192.168." + subnetID + ".1";
        this.seedIP = DockerCluster.getKthIP(hostIP, 0);
        try {
            this.build();
        } catch (Exception e) {
            logger.error("Cannot build cluster " + e);
        }
    }

    public void teardown() {
        if (exportComposeOnly) {
            return;
        }

        // Chmod so that we can read/write them on the host machine
        try {
            for (Docker docker : dockers) {
                docker.chmodDir();
            }
        } catch (Exception e) {
            logger.error("fail to chmod dir");
        }

        try {
            Process buildProcess = Utilities.exec(
                    new String[] { "docker", "compose", "down" }, workdir);
            buildProcess.waitFor();
            logger.info("teardown docker compose in " + workdir);
        } catch (IOException | InterruptedException e) {
            logger.error("failed to teardown docker", e);
        }

        if (!Config.getConf().keepDir) {
            try {
                Utilities.exec(new String[] { "rm", "-rf",
                        this.workdir
                                .getAbsolutePath() },
                        ".");
            } catch (IOException e) {
                e.printStackTrace();
            }
            logger.info("[teardown] deleting dir");
        }
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

    @Override
    public void formatComposeYaml() {
        Map<String, String> formatMap = new HashMap<>();

        StringBuilder sb = new StringBuilder();
        sb.append(extranodes[0].formatComposeYaml());
        for (Docker docker : dockers) {
            sb.append(docker.formatComposeYaml());
        }
        String dockersFormat = sb.toString();
        formatMap.put("dockers", dockersFormat);
        formatMap.put("subnet", subnet);
        formatMap.put("networkName", networkName);
        StringSubstitutor sub = new StringSubstitutor(formatMap);
        this.composeYaml = sub.replace(template);
    }

    @Override
    public Path getDataPath() {
        return Paths.get(workdir.toString(), "persistent");
    }

    // Fault Injection
    // If we crash a docker container, the cqlsh might also be killed.
    // So next time if we want to issue a command, we might need to send
    // the command to a different node.

    // Disconnect the network of a set of containers
    // Inside the nodes, they also cannot communicate with each other (Not
    // Partition)
    public boolean disconnectNetwork(Set<Integer> nodeIndexes) {
        // First check whether all indexes is valid
        int maxNodeIndex = Collections.max(nodeIndexes);
        int minNodeIndex = Collections.min(nodeIndexes);

        if (maxNodeIndex >= nodeNum || minNodeIndex < 0) {
            throw new RuntimeException(
                    "The nodeIndex is out of range. maxNodeIndex = "
                            + maxNodeIndex
                            + ", minNodeIndex = " + minNodeIndex
                            + ", nodeNum = " + nodeNum);
        }
        for (int nodeIndex : nodeIndexes) {
            try {
                if (!disconnectNetwork(dockers[nodeIndex]))
                    return false;
            } catch (IOException | InterruptedException e) {
                logger.error("Cannot disconnect network of container "
                        + dockers[nodeIndex].containerName + " exception: "
                        + e);
            }
        }
        return true;
    }

    // Disconnect one container from network
    private boolean disconnectNetwork(DockerMeta docker)
            throws IOException, InterruptedException {
        String[] disconnectNetworkCMD = new String[] {
                "docker", "network", "disconnect", "-f", networkID,
                docker.containerName
        };
        Process disconnProcess = Utilities.exec(disconnectNetworkCMD, workdir);
        int ret = disconnProcess.waitFor();

        return ret == 0;
    }

    @Override
    public void prepareUpgrade() throws Exception {
    }

    @Override
    public void finalizeUpgrade() {
        logger.debug("HBase upgrade finalized");
    }
}
