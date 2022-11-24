package org.zlab.upfuzz.docker;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang3.RandomUtils;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.fuzzingengine.LogInfo;
import org.zlab.upfuzz.fuzzingengine.executor.Executor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.utils.Utilities;

public abstract class DockerCluster implements IDockerCluster {
    static Logger logger = LogManager.getLogger(DockerCluster.class);

    protected Docker[] dockers;
    public DockerMeta.DockerState[] dockerStates;

    public Network network;

    public String version;
    public String originalVersion;
    public String upgradedVersion;

    public int nodeNum;
    public String networkID;
    public Executor executor;
    public String executorID;
    public String system;
    public String subnet;
    public int agentPort;
    public int subnetID;
    public String networkName;
    public String composeYaml;
    public String hostIP;
    public File workdir;
    public Path configpath;

    public Set<String> targetSystemStates;

    // This function do the shifting
    // .1 runs the client
    // .2 runs the first node
    public static String getKthIP(String ip, int index) {
        String[] segments = ip.split("\\.");
        segments[3] = Integer.toString(index + 2);
        return String.join(".", segments);
    }

    public DockerCluster(Executor executor, String version,
            int nodeNum, Set<String> targetSystemStates) {
        // replace subnet
        // rename services

        // 192.168.24.[(0001~1111)|0000] / 28
        this.networkName = MessageFormat.format(
                "network_{0}_{1}_to_{2}_{3}", executor.systemID,
                Config.getConf().originalVersion,
                Config.getConf().upgradedVersion,
                UUID.randomUUID());
        this.subnetID = RandomUtils.nextInt(1, 256);
        this.subnet = "192.168." + subnetID + ".1/24";
        this.hostIP = "192.168." + subnetID + ".1";
        this.agentPort = executor.agentPort;
        this.executor = executor;
        this.executorID = executor.executorID;
        this.version = version;
        this.originalVersion = Config.getConf().originalVersion;
        this.upgradedVersion = Config.getConf().upgradedVersion;
        this.system = executor.systemID;
        this.nodeNum = nodeNum;
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        String executorTimestamp = formatter.format(System.currentTimeMillis());
        this.workdir = new File("fuzzing_storage/" + executor.systemID + "/" +
                originalVersion + "/" + upgradedVersion + "/" +
                executorTimestamp + "-" + executor.executorID);
        this.network = new Network();
        this.targetSystemStates = targetSystemStates;

        // Init docker states
        dockerStates = new DockerMeta.DockerState[nodeNum];
        for (int i = 0; i < nodeNum; i++) {
            this.dockerStates[i] = new DockerMeta.DockerState(
                    DockerMeta.DockerVersion.original, true);
        }
    }

    public int getFirstLiveNodeIdx() {
        int idx = -1;
        for (int i = 0; i < nodeNum; i++) {
            if (dockerStates[i].alive) {
                idx = i;
                break;
            }
        }
        return idx;
    }

    @Override
    public boolean fullStopUpgrade() throws Exception {
        logger.info("Cluster full-stop upgrading...");
        prepareUpgrade();
        for (int i = 0; i < dockers.length; i++) {
            dockers[i].flush();
            dockers[i].shutdown();
        }
        for (int i = 0; i < dockers.length; i++) {
            dockers[i].upgrade();
        }
        logger.info("Cluster upgraded");
        return true;
    }

    @Override
    public boolean rollingUpgrade() throws Exception {
        logger.info("Cluster upgrading...");
        prepareUpgrade();
        for (int i = 0; i < dockers.length; ++i) {
            dockers[i].flush();
            dockers[i].shutdown();
            dockers[i].upgrade();
        }
        logger.info("Cluster upgraded");
        return true;
    }

    @Override
    public boolean freshStartNewVersion() throws Exception {
        logger.info("Fresh start new version ...");
        for (int i = 0; i < dockers.length; i++) {
            dockers[i].shutdown();
        }
        for (int i = 0; i < dockers.length; i++) {
            dockers[i].clear();
        }
        // new version will start up from a clear state
        for (int i = 0; i < dockers.length; i++) {
            dockers[i].upgrade();
        }
        logger.info("Cluster upgraded");
        return true;
    }

    /**
     * collecting system states from each node
     */
    public Map<Integer, Map<String, String>> readSystemState() {
        // nodeId -> {class.state -> value}
        Map<Integer, Map<String, String>> states = new HashMap<>();
        for (int i = 0; i < nodeNum; i++) {
            states.put(i, dockers[i].readSystemState());
        }
        return states;
    }

    /**
     * collecting system states from each node
     */
    public Map<Integer, LogInfo> readLogInfo() {
        // nodeId -> {class.state -> value}
        Map<Integer, LogInfo> states = new HashMap<>();
        for (int i = 0; i < nodeNum; i++) {
            states.put(i, dockers[i].readLogInfo());
        }
        return states;
    }

    // Some preparation before upgrading nodes
    // - prepare FSImage in HDFS
    // - flush memTable in Cassandra
    public abstract void prepareUpgrade() throws Exception;

    @Override
    public void upgrade(int nodeIndex) throws Exception {
        // upgrade a specific node
        logger.info(String.format("Upgrade Node[%d]", nodeIndex));
        dockers[nodeIndex].flush();
        dockers[nodeIndex].shutdown();
        dockers[nodeIndex].upgrade();
        dockerStates[nodeIndex].dockerVersion = DockerMeta.DockerVersion.upgraded;
        logger.info(String.format("Node[%d] is upgraded", nodeIndex));
    }

    @Override
    public void downgrade(int nodeIndex) throws Exception {
        // upgrade a specific node
        logger.info(String.format("Downgrade Node[%d]", nodeIndex));
        dockers[nodeIndex].shutdown();
        dockers[nodeIndex].downgrade();
        dockerStates[nodeIndex].dockerVersion = DockerMeta.DockerVersion.original;
        logger.info(String.format("Node[%d] is downgraded", nodeIndex));
    }

    // Link failure on two nodes
    // Always provide the node index to clients
    public boolean linkFailure(int nodeIndex1, int nodeIndex2) {
        if (!checkIndex(nodeIndex1) || !checkIndex(nodeIndex2))
            return false;
        if (nodeIndex1 == nodeIndex2)
            return false;
        // If one of the node is down, what should we do?
        // - We shouldn't inject faults here right? The
        // - container is completely down
        if (!dockerStates[nodeIndex1].alive
                || !dockerStates[nodeIndex2].alive) {
            return false;
        }
        logger.info("[LinkFailure] node" + nodeIndex1 + ", node" + nodeIndex2);
        return network.biPartition(dockers[nodeIndex1], dockers[nodeIndex2]);
    }

    public boolean linkFailureRecover(int nodeIndex1, int nodeIndex2) {
        if (!checkIndex(nodeIndex1) || !checkIndex(nodeIndex2))
            return false;
        if (nodeIndex1 == nodeIndex2)
            return false;
        return network.biPartitionRecover(dockers[nodeIndex1],
                dockers[nodeIndex2]);
    }

    public boolean isolateNode(int nodeIndex) {
        if (!checkIndex(nodeIndex))
            return false;
        if (!dockerStates[nodeIndex].alive)
            return false;
        Set<Docker> peers = new HashSet<>();
        for (int i = 0; i < nodeNum; i++) {
            if (i != nodeIndex)
                peers.add(dockers[i]);
        }
        return network.isolateNode(dockers[nodeIndex], peers);
    }

    public boolean isolateNodeRecover(int nodeIndex) {
        if (!checkIndex(nodeIndex))
            return false;
        Set<Docker> peers = new HashSet<>();
        for (int i = 0; i < nodeNum; i++) {
            if (i != nodeIndex)
                peers.add(dockers[i]);
        }
        return network.isolateNodeRecover(dockers[nodeIndex], peers);
    }

    public boolean partition(Set<Integer> nodeSet1, Set<Integer> nodeSet2) {
        if (Collections.disjoint(nodeSet1, nodeSet2)) {
            // There shouldn't be common nodes
            return false;
        }
        for (int nodeIndex : nodeSet1) {
            if (!checkIndex(nodeIndex) || !dockerStates[nodeIndex].alive)
                return false;
        }
        for (int nodeIndex : nodeSet2) {
            if (!checkIndex(nodeIndex) || !dockerStates[nodeIndex].alive)
                return false;
        }

        Set<Docker> dockerSet1 = nodeSet1.stream()
                .map(nodeIndex -> dockers[nodeIndex])
                .collect(Collectors.toSet());
        Set<Docker> dockerSet2 = nodeSet2.stream()
                .map(nodeIndex -> dockers[nodeIndex])
                .collect(Collectors.toSet());

        return network.partitionTwoSets(dockerSet1, dockerSet2);
    }

    public boolean partitionRecover(Set<Integer> nodeSet1,
            Set<Integer> nodeSet2) {
        if (Collections.disjoint(nodeSet1, nodeSet2)) {
            // There shouldn't be common nodes
            return false;
        }
        for (int nodeIndex : nodeSet1) {
            if (!checkIndex(nodeIndex))
                return false;
        }
        for (int nodeIndex : nodeSet2) {
            if (!checkIndex((nodeIndex)))
                return false;
        }

        Set<Docker> dockerSet1 = nodeSet1.stream()
                .map(nodeIndex -> dockers[nodeIndex])
                .collect(Collectors.toSet());
        Set<Docker> dockerSet2 = nodeSet2.stream()
                .map(nodeIndex -> dockers[nodeIndex])
                .collect(Collectors.toSet());

        return network.partitionTwoSetsRecover(dockerSet1, dockerSet2);
    }

    public boolean killContainer(int nodeIndex) {
        if (!checkIndex(nodeIndex))
            return false;

        try {
            String[] killContainerCMD = new String[] {
                    "docker-compose", "stop", dockers[nodeIndex].serviceName
            };
            logger.debug("workdir = " + workdir);
            Process killContainerProcess = Utilities.exec(killContainerCMD,
                    workdir);
            killContainerProcess.waitFor();
        } catch (IOException | InterruptedException e) {
            logger.error("Cannot delete container index "
                    + dockers[nodeIndex].containerName, e);
            return false;
        }

        dockerStates[nodeIndex].alive = false;
        return true;
    }

    public boolean restartContainer(int nodeIndex) {
        if (!checkIndex(nodeIndex))
            return false;

        try {
            String[] restartContainerCMD = new String[] {
                    "docker-compose", "restart", dockers[nodeIndex].serviceName
            };
            logger.debug("workdir = " + workdir);
            Process restartContainerProcess = Utilities.exec(
                    restartContainerCMD,
                    workdir);
            restartContainerProcess.waitFor();

            // recreate the shell connection
            dockers[nodeIndex].start();
        } catch (Exception e) {
            logger.error("Cannot restart container index "
                    + dockers[nodeIndex].containerName, e);
            return false;
        }

        dockerStates[nodeIndex].alive = true;
        return true;
    }

    // Is it currently in the new version or the old version?
    // I crash it, but it should still remain the original state
    // So it should restart from where it crashed
    public boolean killContainerRecover(int nodeIndex) {
        if (!checkIndex(nodeIndex))
            return false;

        // docker-compose recover SERVICE_NAME
        try {
            String[] containerRecoverCMD = new String[] {
                    "docker-compose", "restart", dockers[nodeIndex].serviceName
            };
            Process containerRecoverProcess = Utilities.exec(
                    containerRecoverCMD,
                    workdir);
            containerRecoverProcess.waitFor();

            // recreate the cqlsh connection
            logger.info(String.format("[HKLOG] Wait for node%d to restart",
                    nodeIndex));
            dockers[nodeIndex].start();

            logger.info(
                    String.format("Node%d recover successfully!", nodeIndex));
        } catch (Exception e) {
            logger.error("Cannot recover container index "
                    + dockers[nodeIndex].containerName, e);
            return false;
        }
        dockerStates[nodeIndex].alive = true;
        return true;
    }

    public boolean checkIndex(int nodeIndex) {
        return nodeIndex < nodeNum && nodeIndex >= 0;
    }

    public abstract void finalizeUpgrade();

}
