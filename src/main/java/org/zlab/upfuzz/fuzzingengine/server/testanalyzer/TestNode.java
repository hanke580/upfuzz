package org.zlab.upfuzz.fuzzingengine.server.testanalyzer;

import org.zlab.upfuzz.fuzzingengine.server.testtracker.TestTrackerNode;

import java.util.ArrayList;
import java.util.List;

public class TestNode extends TestTrackerNode {

    private List<TestNode> children = new ArrayList<>();

    public TestNode(int nodeId, int pNodeId, List<String> writeCommands,
            List<String> readCommands, int configId) {
        super(nodeId, pNodeId, writeCommands, readCommands, configId);
    }

    public TestNode(TestTrackerNode testTrackerNode) {
        super(testTrackerNode.nodeId, testTrackerNode.pNodeId,
                testTrackerNode.writeCommands, testTrackerNode.readCommands,
                testTrackerNode.configId);
    }

    public List<TestNode> getChildren() {
        return children;
    }

    public void addChild(TestNode child) {
        children.add(child);
    }

}
