package org.zlab.upfuzz.fuzzingengine.testplan.event.fault;

public class NodeFailure extends Fault {
    public int nodeIndex;

    public NodeFailure(int nodeIndex) {
        this.nodeIndex = nodeIndex;
    }

    @Override
    public FaultRecover generateRecover() {
        return null;
    }
}
