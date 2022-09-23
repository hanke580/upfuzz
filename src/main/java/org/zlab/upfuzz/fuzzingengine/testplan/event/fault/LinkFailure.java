package org.zlab.upfuzz.fuzzingengine.testplan.event.fault;

public class LinkFailure extends Fault {
    public int nodeIndex1;
    public int nodeIndex2;

    public LinkFailure(int nodeIndex1, int nodeIndex2) {
        super("LinkFailure");
        this.nodeIndex1 = nodeIndex1;
        this.nodeIndex2 = nodeIndex2;
    }

    @Override
    public FaultRecover generateRecover() {
        return new LinkFailureRecover(nodeIndex1, nodeIndex2);
    }

    @Override
    public String toString() {
        return String.format("[Fault] LinkFailure: Node[%d], Node[%d]",
                nodeIndex1, nodeIndex2);
    }
}
