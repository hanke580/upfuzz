package org.zlab.upfuzz.fuzzingengine.testplan.event.fault;

public class NodeFailureRecover extends FaultRecover {
    public int nodeIndex;

    public NodeFailureRecover(int nodeIndex) {
        this.nodeIndex = nodeIndex;
    }

}
