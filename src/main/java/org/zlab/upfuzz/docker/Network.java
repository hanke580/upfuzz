package org.zlab.upfuzz.docker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class Network {

    static Logger logger = LogManager.getLogger(Network.class);

    public boolean partitionTwoSets(DockerMeta[] nodeSet1,
            DockerMeta[] nodeSet2) {
        for (DockerMeta node1 : nodeSet1) {
            for (DockerMeta node2 : nodeSet2) {
                if (!partition(node1, node2)) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean partition(DockerMeta local, DockerMeta remote) {
        // Normal network failure between two nodes
        return biPartition(local, remote);
    }

    public boolean biPartition(DockerMeta node1, DockerMeta node2) {
        boolean ret1 = true, ret2 = true;
        if (node2 != null) {
            ret1 = uniPartition(node1, node2);
            ret2 = uniPartition(node2, node1);
        }
        return ret1 && ret2;
    }

    private boolean uniPartition(DockerMeta local, DockerMeta remote) {
        // Make node1 cannot receive any packets from node2
        // Execute in node1
        try {
            local.runInContainer(new String[] {
                    "iptables", "-A", "INPUT", "-s", remote.networkIP, "-j",
                    "DROP", "-w"
            });
        } catch (IOException e) {
            logger.error("cannot create a partition from " + local.containerName
                    + " to " + remote.containerName);
            return false;
        }
        return true;
    }
}
