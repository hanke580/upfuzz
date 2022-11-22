package org.zlab.upfuzz.fuzzingengine.testplan.event.downgradeop;

import org.zlab.upfuzz.fuzzingengine.testplan.event.Event;

public class DowngradeOp extends Event {
    public int nodeIndex;

    public DowngradeOp(int nodeIndex) {
        super("DowngradeOp");
        this.nodeIndex = nodeIndex;
    }

    @Override
    public String toString() {
        return String.format("[DowngradeOp] Downgrade Node[%d]", nodeIndex);
    }
}
