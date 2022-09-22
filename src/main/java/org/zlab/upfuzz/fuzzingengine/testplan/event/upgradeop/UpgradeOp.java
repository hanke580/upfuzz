package org.zlab.upfuzz.fuzzingengine.testplan.event.upgradeop;

import org.zlab.upfuzz.fuzzingengine.testplan.event.Event;

public class UpgradeOp implements Event {
    public int nodeIndex;

    public UpgradeOp(int nodeIndex) {
        this.nodeIndex = nodeIndex;
    }
}
