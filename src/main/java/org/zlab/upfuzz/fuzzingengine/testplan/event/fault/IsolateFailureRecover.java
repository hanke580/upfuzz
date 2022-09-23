package org.zlab.upfuzz.fuzzingengine.testplan.event.fault;

public class IsolateFailureRecover extends FaultRecover {
    int nodeIndex;

    public IsolateFailureRecover(int nodeIndex) {
        super("IsolateFailureRecover");
        this.nodeIndex = nodeIndex;
    }

    @Override
    public String toString() {
        return String.format(
                "[FaultRecover] Recover from Isolate Failure: Node[%d]",
                nodeIndex);
    }
}
