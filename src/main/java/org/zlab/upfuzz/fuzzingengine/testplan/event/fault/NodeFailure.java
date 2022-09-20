package org.zlab.upfuzz.fuzzingengine.testplan.event.fault;

import org.zlab.upfuzz.fuzzingengine.testplan.event.Fault;

public class NodeFailure extends Fault {
    public int nodeIndex;

    public NodeFailure(int nodeIndex) {
        this.nodeIndex = nodeIndex;
    }

}
