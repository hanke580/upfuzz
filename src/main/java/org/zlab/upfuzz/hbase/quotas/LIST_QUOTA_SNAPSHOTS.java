package org.zlab.upfuzz.hbase.quotas;

import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseState;

public class LIST_QUOTA_SNAPSHOTS
        extends org.zlab.upfuzz.hbase.HBaseCommand {
    public LIST_QUOTA_SNAPSHOTS(HBaseState state) {
        super(state);
    }

    @Override
    public String constructCommandString() {
        return "list_quota_snapshots";
    }

    @Override
    public void updateState(State state) {

    }
}
