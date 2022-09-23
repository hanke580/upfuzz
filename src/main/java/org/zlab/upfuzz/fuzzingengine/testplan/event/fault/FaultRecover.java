package org.zlab.upfuzz.fuzzingengine.testplan.event.fault;

import org.zlab.upfuzz.fuzzingengine.testplan.event.Event;

public abstract class FaultRecover extends Event {
    public FaultRecover(String type) {
        super(type);
    }
}
