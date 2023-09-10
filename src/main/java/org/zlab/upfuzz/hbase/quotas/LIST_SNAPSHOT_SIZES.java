package org.zlab.upfuzz.hbase.quotas;

import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseState;

public class LIST_SNAPSHOT_SIZES
        extends org.zlab.upfuzz.hbase.HBaseCommand {
    public LIST_SNAPSHOT_SIZES(HBaseState state) {
        super(state);
    }

    @Override
    public String constructCommandString() {
        return "list_snapshot_sizes";
    }

    @Override
    public void updateState(State state) {

    }
}
