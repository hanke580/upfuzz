package org.zlab.upfuzz.fuzzingengine.testplan.event.fault;

import java.util.AbstractMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class FaultPool {

    public enum FaultType {
        IsolateFailure, LinkFailure, NodeFailure, RestartFailure
    }

    public static final List<Map.Entry<Class<? extends Fault>, Integer>> faultClassList = new LinkedList<>();

    static {
        faultClassList.add(new AbstractMap.SimpleImmutableEntry<>(
                IsolateFailure.class, 1));
        faultClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(LinkFailure.class, 1));
        faultClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(NodeFailure.class, 1));
        faultClassList.add(new AbstractMap.SimpleImmutableEntry<>(
                PartitionFailure.class, 1));
        faultClassList.add(new AbstractMap.SimpleImmutableEntry<>(
                RestartFailure.class, 1));
    }
}
