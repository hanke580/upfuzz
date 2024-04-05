package org.zlab.upfuzz.fuzzingengine.server.testanalyzer;

import org.zlab.upfuzz.fuzzingengine.server.testtracker.BaseNode;
import org.zlab.upfuzz.fuzzingengine.server.testtracker.TestTrackerUpgradeNode;

import java.util.ArrayList;
import java.util.List;

public class TestNode {

    private List<TestNode> children = new ArrayList<>();

    public BaseNode baseNode;

    public TestNode(BaseNode baseNode) {
        this.baseNode = baseNode;
    }

    public List<TestNode> getChildren() {
        return children;
    }

    public void addChild(TestNode child) {
        children.add(child);
    }

}
