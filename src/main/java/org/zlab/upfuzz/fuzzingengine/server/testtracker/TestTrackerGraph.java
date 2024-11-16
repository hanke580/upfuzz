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
    private static final Path graphDirPath = Paths
            .get(Config.getConf().testGraphDirPath);

    private final Map<Integer, BaseNode> nodeMap = new HashMap<>();
    private final List<BaseNode> rootNodes = new ArrayList<>();

    public TestTrackerGraph() {
        // create a dir for node storage
        if (!graphDirPath.toFile().exists()) {
            graphDirPath.toFile().mkdirs();
        }
    }

    private void addNode(BaseNode node) {
        if (node.pNodeId == -1)
            rootNodes.add(node);
        nodeMap.put(node.nodeId, node);
    }

    public void addNode(int parentNodeID, Seed seed) {
        // parentNodeID == -1 means this is the root node (random generated, or
        // from the provided corpus)
        if (Config.getConf().useVersionDelta) {
            BaseNode node;
            if (Config.getConf().versionDeltaApproach == 1) {
                node = new TestTrackerMatchableVersionDeltaNode(
                        seed.testID, parentNodeID,
                        seed.originalCommandSequence.getCommandStringList(),
                        seed.validationCommandSequence.getCommandStringList(),
                        seed.configIdx);
            } else {
                node = new TestTrackerVersionDeltaNode(
                        seed.testID, parentNodeID,
                        seed.originalCommandSequence.getCommandStringList(),
                        seed.validationCommandSequence.getCommandStringList(),
                        seed.configIdx);
            }
            addNode(node);
        } else {
            BaseNode node = new TestTrackerUpgradeNode(
                    seed.testID, parentNodeID,
                    seed.originalCommandSequence.getCommandStringList(),
                    seed.validationCommandSequence.getCommandStringList(),
                    seed.configIdx);
            addNode(node);
        }
    }

    public void updateNodeCoverage(int nodeId,
            boolean newOriBC,
            boolean newUpgradeBC, boolean newFC) {
        updateNodeCoverage(nodeId, newOriBC,
                newUpgradeBC, newFC, false);
    }

    // Non-version delta testing mode
    public void updateNodeCoverage(int nodeId,
            boolean newOriBC,
            boolean newUpgradeBC, boolean newFC,
            boolean newModFC) {
        // Runtime tracking, it removes the node from memory
        // long startTime = System.nanoTime();
        assert !Config.getConf().useVersionDelta;

        BaseNode baseNode = nodeMap.get(nodeId);
        assert baseNode instanceof TestTrackerUpgradeNode;
        TestTrackerUpgradeNode node = (TestTrackerUpgradeNode) baseNode;
        node.updateCoverage(newOriBC,
                newUpgradeBC, newFC, newModFC);

        // serialize this node to disk, remove it from map
        assert nodeMap.containsKey(nodeId);

        if (node.pNodeId == -1)
            rootNodes.remove(node);
        BaseNode nodeToSerialize = nodeMap.remove(nodeId);

        try {
            // create a folder according to its testId
            int subDirId = nodeId / 10000;
            File subTestDir = graphDirPath.resolve(subDirId + "").toFile();
            if (!subTestDir.exists()) {
                subTestDir.mkdirs();
            }
            BaseNode
                    .serializeNodeToDisk(
                            subTestDir.toPath()
                                    .resolve(nodeId + ".ser"),
                            nodeToSerialize);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // --------- Version delta testing mode (1-group) ---------

    // Update coverage (single group)
    public void updateNodeCoverage(int nodeId,
            boolean newOriBC,
            boolean newUpgradeBC,
            boolean newUpBC,
            boolean newDowngradeBC,
            boolean newOriFC,
            boolean newUpFC,
            boolean newOriMatchableFC,
            boolean newUpMatchableFC) {
        // Runtime tracking, it removes the node from memory
        assert Config.getConf().useVersionDelta
                && Config.getConf().versionDeltaApproach == 1;

        BaseNode baseNode = nodeMap.get(nodeId);
        assert baseNode instanceof TestTrackerMatchableVersionDeltaNode;
        TestTrackerMatchableVersionDeltaNode node = (TestTrackerMatchableVersionDeltaNode) baseNode;
        node.updateCoverage(newOriBC, newUpgradeBC, newUpBC,
                newDowngradeBC, newOriFC, newUpFC, newOriMatchableFC,
                newUpMatchableFC);

        // serialize this node to disk, remove it from map
        assert nodeMap.containsKey(nodeId);

        if (node.pNodeId == -1)
            rootNodes.remove(node);
        BaseNode nodeToSerialize = nodeMap.remove(nodeId);

        Path subDir = createSubDir(nodeId);

        try {
            BaseNode
                    .serializeNodeToDisk(
                            subDir.resolve(nodeId + ".ser"),
                            nodeToSerialize);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // --------- Version delta testing mode (2-group) ---------

    // Update coverage group1
    public void updateNodeCoverageGroup1(int nodeId,
            boolean newOriBC,
            boolean newUpBC,
            boolean newOriFC,
            boolean newUpFC) {
        // Runtime tracking, it removes the node from memory
        if (!Config.getConf().useVersionDelta)
            throw new RuntimeException(
                    "This function is only for version delta testing");

        BaseNode baseNode = nodeMap.get(nodeId);
        assert baseNode instanceof TestTrackerVersionDeltaNode;
        TestTrackerVersionDeltaNode node = (TestTrackerVersionDeltaNode) baseNode;
        node.updateCoverageGroup1(newOriBC, newUpBC, newOriFC, newUpFC);

        // serialize this node to disk, remove it from map
        assert nodeMap.containsKey(nodeId);

        if (node.pNodeId == -1)
            rootNodes.remove(node);
        BaseNode nodeToSerialize = nodeMap.remove(nodeId);

        Path subDirPath = createSubDir(nodeId);
        try {
            BaseNode.serializeNodeToDisk(
                    subDirPath.resolve(nodeId + ".ser"), nodeToSerialize);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Version delta testing mode: Group2 update
    public void updateNodeCoverageGroup2(int nodeId,
            boolean newUpgradeBC,
            boolean newDowngradeBC) {
        // Runtime tracking, it removes the node from memory
        if (!Config.getConf().useVersionDelta)
            throw new RuntimeException(
                    "This function is only for version delta testing");

        // load baseNode from disk...
        int subDirId = nodeId / 10000;
        Path baseNodePath = Paths.get(Config.getConf().testGraphDirPath)
                .resolve(String.valueOf(subDirId)).resolve((nodeId + ".ser"));
        try {
            BaseNode baseNode = BaseNode
                    .deserializeNodeFromDisk(baseNodePath.toFile());
            TestTrackerVersionDeltaNode node = (TestTrackerVersionDeltaNode) baseNode;
            node.updateCoverageGroup2(newUpgradeBC,
                    newDowngradeBC);
            // Double check to make sure node not exist anymore
            if (node.pNodeId == -1)
                rootNodes.remove(node);
            nodeMap.remove(nodeId);
            BaseNode.serializeNodeToDisk(baseNodePath, node);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public BaseNode getNode(int nodeId) {
        return nodeMap.get(nodeId);
    }

    public Path createSubDir(int nodeId) {
        int subDirId = nodeId / 10000;
        Path subTestDirPath = graphDirPath.resolve(subDirId + "");
        if (!subTestDirPath.toFile().exists()) {
            subTestDirPath.toFile().mkdirs();
        }
        return subTestDirPath;
    }

    public static Path getSubDirPath(int nodeId) {
        int subDirId = nodeId / 10000;
        Path subTestDir = Paths.get(Config.getConf().testGraphDirPath)
                .resolve(subDirId + "");
        return subTestDir;
    }

}
