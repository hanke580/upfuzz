package org.zlab.upfuzz.fuzzingengine.testplan.event.fault;

import org.zlab.upfuzz.fuzzingengine.testplan.event.Fault;

import java.util.Set;

public class PartitionFailure extends Fault {
    public Set<Integer> nodeSet1;
    public Set<Integer> nodeSet2;

}
