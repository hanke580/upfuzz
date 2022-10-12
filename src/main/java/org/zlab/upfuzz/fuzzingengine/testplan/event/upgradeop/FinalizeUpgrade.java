package org.zlab.upfuzz.fuzzingengine.testplan.event.upgradeop;

import org.zlab.upfuzz.fuzzingengine.testplan.event.Event;

public class FinalizeUpgrade extends Event {

    public FinalizeUpgrade() {
        super("FinalizeUpgrade");
    }

    @Override
    public String toString() {
        return "[FinalizeUpgrade] Finalize Upgrade Process";
    }

}
