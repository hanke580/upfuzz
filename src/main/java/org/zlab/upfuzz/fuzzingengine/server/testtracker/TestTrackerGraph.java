package org.zlab.upfuzz.fuzzingengine.server.testtracker;

import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.fuzzingengine.server.Seed;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lightweight tracking for test node during the testing
 */
public class TestTrackerGraph implements Serializable {
    private Map<Integer, TestTrackerNode> nodeMap = new HashMap<>();
    private List<TestTrackerNode> rootNodes = new ArrayList<>();

    public TestTrackerGraph() {
        // create a dir for node storage
        File graphDir = Paths.get(Config.getConf().testGraphDirPath).toFile();
        if (!graphDir.exists()) {
            graphDir.mkdirs();
        }
    }

    public static Path getSubDirPath(int nodeId) {
        int subDirId = nodeId / 10000;
        Path subTestDir = Paths.get(Config.getConf().testGraphDirPath)
                .resolve(subDirId + "");
        return subTestDir;
    }

    private void addNode(TestTrackerNode node) {
        if (node.pNodeId == -1)
            rootNodes.add(node);
        nodeMap.put(node.nodeId, node);
    }

    public void addNode(int parentNodeID, Seed seed) {
        // parentNodeID == -1 means this is the root node (random generated, or
        // from the provided corpus)
        TestTrackerNode node = new TestTrackerNode(seed.testID, parentNodeID,
                seed.originalCommandSequence.getCommandStringList(),
                seed.validationCommandSequence.getCommandStringList(),
                seed.configIdx);
        addNode(node);
    }

    public void updateNodeCoverage(int nodeId, boolean newCoverage) {
        // Runtime tracking, it removes the node from memory
        // long startTime = System.nanoTime();
        TestTrackerNode node = nodeMap.get(nodeId);
        node.newCoverage = newCoverage;
        // serialize this node to disk, remove it from map
        assert nodeMap.containsKey(nodeId);

        if (node.pNodeId == -1)
            rootNodes.remove(node);
        TestTrackerNode nodeToSerialize = nodeMap.remove(nodeId);

        Path graphDir = Paths.get(Config.getConf().testGraphDirPath);

        try {

            // create a folder according to its testId
            int subDirId = nodeId / 10000;
            File subTestDir = graphDir.resolve(subDirId + "").toFile();
            if (!subTestDir.exists()) {
                subTestDir.mkdirs();
            }
            TestTrackerNode
                    .serializeNodeToDisk(
                            subTestDir.toPath()
                                    .resolve(nodeId + ".ser"),
                            nodeToSerialize);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // Takes around 3.3 ms for each node
        // long endTime = System.nanoTime();
        // // Calculate the duration in milliseconds
        // double durationInMilliseconds = (endTime - startTime) / 1_000_000.0;
        // System.out.println("Function execution time: " +
        // durationInMilliseconds + " ms");
    }

    public TestTrackerNode getNode(int nodeId) {
        return nodeMap.get(nodeId);
    }
}
