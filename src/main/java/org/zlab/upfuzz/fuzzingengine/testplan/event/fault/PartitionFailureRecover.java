package org.zlab.upfuzz.fuzzingengine.testplan.event.fault;

import java.util.Set;

public class PartitionFailureRecover extends FaultRecover {
    public Set<Integer> nodeSet1;
    public Set<Integer> nodeSet2;

    public PartitionFailureRecover(Set<Integer> nodeSet1,
            Set<Integer> nodeSet2) {
        super("PartitionFailureRecover");
        this.nodeSet1 = nodeSet1;
        this.nodeSet2 = nodeSet2;
    }
}
