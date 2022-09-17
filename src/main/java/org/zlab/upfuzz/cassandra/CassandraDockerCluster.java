package org.zlab.upfuzz.cassandra;

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
import org.zlab.upfuzz.docker.DockerCluster;
import org.zlab.upfuzz.docker.DockerMeta;
import org.zlab.upfuzz.docker.IDocker;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.utils.Utilities;

public class CassandraDockerCluster extends DockerCluster {
    static Logger logger = LogManager.getLogger(CassandraDockerCluster.class);

    // CassandraDocker[] dockers;
    String seedIP;

    static final String includes = "org.apache.cassandra.*";
    static final String excludes = "org.apache.cassandra.metrics.*:org.apache.cassandra.net.*:org.apache.cassandra.io.sstable.format.SSTableReader.*:org.apache.cassandra.service.*";

    CassandraDockerCluster(CassandraExecutor executor, String version,
            int nodeNum) {
        super(executor, version, nodeNum);

        this.dockers = new CassandraDocker[nodeNum];
        this.seedIP = DockerCluster.getKthIP(hostIP, 0);
    }

    public boolean build() throws Exception {
        for (int i = 0; i < dockers.length; ++i) {
            dockers[i] = new CassandraDocker(this, i);
            dockers[i].build();
        }
        return true;
    }

    public int start() {
        // String dockerTimestamp =
        // formatter.format(System.currentTimeMillis());

        File composeFile = new File(workdir, "docker-compose.yaml");
        if (!workdir.exists()) {
            workdir.mkdirs();
        }

        Process buildProcess = null;
        int retry = 3, ret = -1;

        for (int i = 0; i < retry; ++i) {
            try {
                BufferedWriter writer = new BufferedWriter(
                        new FileWriter(composeFile));

                formatComposeYaml();
                composeFile.createNewFile();
                // logger.info("\n\n compose yaml \n" + composeYaml + "\n\n");
                writer.write(composeYaml);
                writer.close();

                buildProcess = Utilities.exec(
                        new String[] { "docker-compose", "up", "-d" }, workdir);
                ret = buildProcess.waitFor();
                if (ret == 0) {
                    logger.info("docker-compose up " + workdir);
                    break;
                } else {
                    Utilities.exec(
                            new String[] { "docker", "network", "prune" },
                            workdir);
                    refreshNetwork();
                    String errorMessage = Utilities.readProcess(buildProcess);
                    logger.warn("docker-compose up\n" + errorMessage);
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                throw new RuntimeException(e.toString());
            }
        }
        if (ret != 0) {
            String errorMessage = Utilities.readProcess(buildProcess);
            logger.error("docker-compose up\n" + errorMessage);
            // System.exit(ret);
        }

        try {
            // Get network full name here, so that later we can disconnect and
            // reconnect
            Process getNameProcess = Utilities.exec(
                    new String[] { "/bin/sh", "-c",
                            "docker network ls | grep " + networkName },
                    workdir);
            getNameProcess.waitFor();

            BufferedReader stdInput = new BufferedReader(
                    new InputStreamReader(getNameProcess.getInputStream()));

            BufferedReader stdError = new BufferedReader(
                    new InputStreamReader(getNameProcess.getErrorStream()));

            List<String> results = new ArrayList<>();
            String s;
            while ((s = stdInput.readLine()) != null) {
                results.add(s);
            }
            if (results.size() != 1) {
                logger.error(
                        "There should be one matching network, but there is "
                                + results.size() + " matching");
                this.networkID = null;
            } else {
                this.networkID = results.get(0).split(" ")[0];
            }

            System.out.println("network ID = " + this.networkID);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < dockers.length; ++i) {
            try {
                dockers[i].start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    private void refreshNetwork() throws IOException {
        this.subnetID = RandomUtils.nextInt(1, 256);
        this.subnet = "192.168." + Integer.toString(subnetID) + ".1/24";
        this.hostIP = "192.168." + Integer.toString(subnetID) + ".1";
        this.seedIP = DockerCluster.getKthIP(hostIP, 0);
        try {
            this.build();
        } catch (Exception e) {
            logger.error("Cannot build cluster " + e);
        }
    }

    public void upgrade() throws Exception {
        logger.info("Cluster upgrading...");
        type = "upgraded";
        for (int i = 0; i < dockers.length; ++i) {
            dockers[i].upgrade();
        }
        logger.info("Cluster upgraded");
    }

    public void teardown() {

        // Chmod so that we can read/write them on the host machine
        try {
            for (int i = 0; i < dockers.length; ++i) {
                dockers[i].chmodDir();
            }
        } catch (Exception e) {
            logger.error("fail to chmod dir");
        }

        try {
            Process buildProcess = Utilities.exec(
                    new String[] { "docker-compose", "down" }, workdir);
            int ret = buildProcess.waitFor();
            // try {
            // if (workdir.delete()) {
            // logger.info("folder deleted successfully");
            // } else {
            // logger.info("cannot delete");
            // }
            // } catch (Exception e) {
            // e.printStackTrace();
            // System.exit(1);
            // }
            logger.info("teardown docker-compose in " + workdir);
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

        return ret == 0 ? true : false;
    }

}
