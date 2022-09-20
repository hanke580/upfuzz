package org.zlab.upfuzz.docker;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang3.RandomUtils;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.fuzzingengine.executor.Executor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.utils.Utilities;

public abstract class DockerCluster implements IDockerCluster {
    static Logger logger = LogManager.getLogger(DockerCluster.class);

    protected Docker[] dockers;

    public Network network;

    public String version;
    public String originalVersion;
    public String upgradedVersion;

    public int nodeNum;
    public String networkID;
    public Executor executor;
    public String executorID;
    public String system;
    public String type;
    public String subnet;
    public int agentPort;
    public int subnetID;
    public String networkName;
    public String composeYaml;
    public String hostIP;
    public File workdir;

    static public String getKthIP(String ip, int index) {
        String[] segments = ip.split("\\.");
        segments[3] = Integer.toString(index + 2);
        return String.join(".", segments);
    }

    public DockerCluster(Executor executor, String version,
            int nodeNum) {
        // replace subnet
        // rename services

        // 192.168.24.[(0001~1111)|0000] / 28
        //
        this.networkName = MessageFormat.format(
                "network_{0}_{1}_to_{2}_{3}", executor.systemID,
                Config.getConf().originalVersion,
                Config.getConf().upgradedVersion,
                UUID.randomUUID());
        this.subnetID = RandomUtils.nextInt(1, 256);
        this.subnet = "192.168." + Integer.toString(subnetID) + ".1/24";
        this.hostIP = "192.168." + Integer.toString(subnetID) + ".1";
        this.agentPort = executor.agentPort;
        this.executor = executor;
        this.executorID = executor.executorID;
        this.version = version;
        this.type = "original";
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
    }

    // partition two nodes
    // Always provide the node index to clients
    public boolean linkFailure(int nodeIndex1, int nodeIndex2) {
        if (!checkIndex(nodeIndex1) || !checkIndex(nodeIndex2))
            return false;
        if (nodeIndex1 == nodeIndex2)
            return false;
        return network.biPartition(dockers[nodeIndex1], dockers[nodeIndex2]);
    }

    public boolean isolateNode(int nodeIndex) {
        if (!checkIndex(nodeIndex))
            return false;
        Set<Docker> peers = new HashSet<>();
        for (int i = 0; i < nodeNum; i++) {
            if (i != nodeIndex)
                peers.add(dockers[i]);
        }
        return network.isolateNode(dockers[nodeIndex], peers);
    }

    public boolean partition(Set<Integer> nodeSet1, Set<Integer> nodeSet2) {
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

        return network.partitionTwoSets(dockerSet1, dockerSet2);
    }

    public boolean killContainer(int nodeIndex) {
        if (!checkIndex(nodeIndex))
            return false;

        boolean ret = true;
        try {
            String[] killContainerCMD = new String[] {
                    "docker", "kill", dockers[nodeIndex].containerName
            };
            Process killContainerProcess = Utilities.exec(killContainerCMD,
                    workdir);
            ret = killContainerProcess.waitFor() == 0;
        } catch (IOException | InterruptedException e) {
            logger.error("Cannot delete container index "
                    + dockers[nodeIndex].containerName, e);
            return false;
        }
        return ret;
    }

    public boolean checkIndex(int nodeIndex) {
        if (nodeIndex >= nodeNum || nodeIndex < 0) {
            return false;
        }
        return true;
    }

}
