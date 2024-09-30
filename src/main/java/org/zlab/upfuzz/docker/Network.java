package org.zlab.upfuzz.docker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Set;

public class Network {
    static Logger logger = LogManager.getLogger(Network.class);

    public boolean partitionTwoSets(Set<Docker> nodeSet1,
            Set<Docker> nodeSet2) {
        for (Docker node1 : nodeSet1) {
            for (Docker node2 : nodeSet2) {
                if (!biPartition(node1, node2)) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean partitionTwoSetsRecover(Set<Docker> nodeSet1,
            Set<Docker> nodeSet2) {
        for (Docker node1 : nodeSet1) {
            for (Docker node2 : nodeSet2) {
                if (!biPartitionRecover(node1, node2)) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean isolateNode(Docker local, Set<Docker> peers) {
        for (Docker node : peers) {
            if (node.containerName != local.containerName) {
                if (!biPartition(node, local))
                    return false;
            }
        }
        return true;
    }

    public boolean isolateNodeRecover(Docker local, Set<Docker> peers) {
        for (Docker node : peers) {
            if (node.containerName != local.containerName) {
                if (!biPartitionRecover(node, local))
                    return false;
            }
        }
        return true;
    }

    public boolean biPartition(Docker node1, Docker node2) {
        boolean ret1 = true, ret2 = true;
        if (node2 != null) {
            ret1 = uniPartition(node1, node2);
            ret2 = uniPartition(node2, node1);
        }
        if (!ret1) {
            logger.error(String.format(
                    "[biPartition] Cannot uniPartition docker[%s] to docker[%s]",
                    node1.serviceName, node2.serviceName));
        }
        if (!ret2) {
            logger.error(String.format(
                    "[biPartition] Cannot uniPartition docker[%s] to docker[%s]",
                    node2.serviceName, node1.serviceName));
        }
        return ret1 && ret2;
    }

    public boolean biPartitionRecover(Docker node1, Docker node2) {
        boolean ret1 = true, ret2 = true;
        if (node2 != null) {
            ret1 = uniPartitionRecover(node1, node2);
            ret2 = uniPartitionRecover(node2, node1);
        }
        return ret1 && ret2;
    }

    public boolean uniPartition(Docker local, Docker remote) {
        // Make node1 cannot receive any packets from node2
        // Execute in node1
        try {
            local.runInContainerWithPrivilege(new String[] {
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

    public boolean uniPartitionRecover(Docker local, Docker remote) {
        try {
            local.runInContainerWithPrivilege(new String[] {
                    "iptables", "-D", "INPUT", "-s", remote.networkIP, "-j",
                    "DROP", "-w"
            });
        } catch (IOException e) {
            logger.error(
                    "cannot recover a partition from " + local.containerName
                            + " to " + remote.containerName);
            return false;
        }
        return true;
    }

}
