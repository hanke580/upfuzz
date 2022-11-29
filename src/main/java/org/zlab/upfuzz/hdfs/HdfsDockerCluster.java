package org.zlab.upfuzz.hdfs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.text.StringSubstitutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.docker.DockerCluster;
import org.zlab.upfuzz.docker.IDocker;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.utils.Utilities;

public class HdfsDockerCluster extends DockerCluster {
    static Logger logger = LogManager.getLogger(HdfsDockerCluster.class);

    String namenodeIP;

    static final String includes = "org.apache.hadoop.hdfs.*";
    static final String excludes = "org.apache.cassandra.*";

    public static String[] includeJacocoHandlers = {
            "org.apache.hadoop.hdfs.server.namenode.NameNode",
            "org.apache.hadoop.hdfs.server.namenode.SecondaryNameNode",
            "org.apache.hadoop.hdfs.server.datanode.DataNode"
    };

    HdfsDockerCluster(HdfsExecutor executor, String version,
            int nodeNum, Set<String> targetSystemStates, Path configPath) {
        super(executor, version, nodeNum, targetSystemStates);

        this.dockers = new HdfsDocker[nodeNum];
        this.namenodeIP = DockerCluster.getKthIP(hostIP, 0); // 2 means the
        this.configpath = configPath;
    }

    public boolean build() throws Exception {
        for (int i = 0; i < dockers.length; ++i) {
            dockers[i] = new HdfsDocker(this, i);
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
        this.namenodeIP = DockerCluster.getKthIP(hostIP, 0);
        try {
            this.build();
        } catch (Exception e) {
            logger.error("Cannot build cluster " + e);
        }
    }

    @Override
    public void prepareUpgrade() {
        int idx = getFirstLiveNodeIdx();
        if (idx == -1) {
            logger.error("cannot upgrade, all nodes are down");
            throw new RuntimeException(
                    "all nodes are down, cannot prepare upgrade");
        }
        String oriHDFS = "/" + system + "/" + originalVersion + "/"
                + "bin/hdfs";
        String[] enterSafemode = new String[] { oriHDFS, "dfsadmin",
                "-safemode", "enter" };
        String[] prepareFSImage = new String[] { oriHDFS, "dfsadmin",
                "-rollingUpgrade", "prepare" };
        String[] leaveSafemode = new String[] { oriHDFS, "dfsadmin",
                "-safemode", "leave" };
        int ret;
        ret = dockers[idx].runProcessInContainer(enterSafemode,
                dockers[idx].env);
        logger.debug("enter safe mode ret = " + ret);
        ret = dockers[idx].runProcessInContainer(prepareFSImage,
                dockers[idx].env);
        logger.debug("prepare image ret = " + ret);
        ret = dockers[idx].runProcessInContainer(leaveSafemode,
                dockers[idx].env);
        logger.debug("leave safemode ret = " + ret);
    }

    @Override
    public void finalizeUpgrade() {
        String upHadoopHDFSPath = "/" + system + "/" + upgradedVersion + "/"
                + "bin/hdfs";
        String[] finalizeUpgradeCmd = new String[] {
                upHadoopHDFSPath, "dfsadmin", "-rollingUpgrade",
                "finalize"
        };
        dockers[0].runProcessInContainer(finalizeUpgradeCmd);
        logger.debug("hdfs upgrade finalized");
    }

    public void stopSNN() {
        String orihadoopDaemonPath = "/" + system + "/" + originalVersion + "/"
                + "sbin/hadoop-daemon.sh";
        String[] stopNode = new String[] { orihadoopDaemonPath, "stop",
                "secondarynamenode" };
        int ret = dockers[1].runProcessInContainer(stopNode, dockers[1].env);
        logger.debug("secondarynamenode stop: " + ret);
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
            buildProcess.waitFor();
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
}
