package org.zlab.upfuzz.fuzzingengine.testplan.event.fault;

import java.util.Set;

public class PartitionFailure extends Fault {
    public Set<Integer> nodeSet1;
    public Set<Integer> nodeSet2;

    public PartitionFailure(Set<Integer> nodeSet1, Set<Integer> nodeSet2) {
        super("PartitionFailure");
        this.nodeSet1 = nodeSet1;
        this.nodeSet2 = nodeSet2;
    }

    @Override
    public FaultRecover generateRecover() {
        return new PartitionFailureRecover(nodeSet1, nodeSet2);
    }
}
