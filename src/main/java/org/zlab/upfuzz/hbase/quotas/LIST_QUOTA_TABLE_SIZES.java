package org.zlab.upfuzz.hbase.quotas;

import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseState;

public class LIST_QUOTA_TABLE_SIZES
        extends org.zlab.upfuzz.hbase.HBaseCommand {
    public LIST_QUOTA_TABLE_SIZES(HBaseState state) {
        super(state);
    }

    @Override
    public String constructCommandString() {
        return "list_quota_table_sizes";
    }

    @Override
    public void updateState(State state) {

    }
}
