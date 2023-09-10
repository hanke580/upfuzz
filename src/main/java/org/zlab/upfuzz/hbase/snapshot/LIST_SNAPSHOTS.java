package org.zlab.upfuzz.hbase.snapshot;

import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;

public class LIST_SNAPSHOTS extends HBaseCommand {
    // read the status
    public LIST_SNAPSHOTS(HBaseState state) {
        super(state);
    }

    @Override
    public String constructCommandString() {
        return "list_snapshots";
    }

    @Override
    public void updateState(State state) {
    }
}
