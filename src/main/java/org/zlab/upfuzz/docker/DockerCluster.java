package org.zlab.upfuzz.docker;

import java.io.File;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.UUID;

import org.apache.commons.lang3.RandomUtils;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.fuzzingengine.executor.Executor;

public abstract class DockerCluster implements IDockerCluster {

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
    }

}
