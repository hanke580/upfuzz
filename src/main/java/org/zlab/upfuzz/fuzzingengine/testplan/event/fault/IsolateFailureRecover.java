package org.zlab.upfuzz.fuzzingengine.testplan.event.fault;

public class IsolateFailureRecover extends FaultRecover {
    int nodeIndex;

    public IsolateFailureRecover(int nodeIndex) {
        this.nodeIndex = nodeIndex;
    }
}
