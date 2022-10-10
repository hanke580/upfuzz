package org.zlab.upfuzz.fuzzingengine.testplan.event.upgradeop;

import org.zlab.upfuzz.fuzzingengine.testplan.event.Event;

public class PrepareUpgrade extends Event {

    public PrepareUpgrade() {
        super("PrepareUpgrade");
    }

    @Override
    public String toString() {
        return "[PrepareUpgrade] flush/create image before starting upgrade";
    }
}
