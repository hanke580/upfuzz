package org.zlab.upfuzz.hbase.quotas;

import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseState;

public class LIST_QUOTAS
        extends org.zlab.upfuzz.hbase.HBaseCommand {
    public LIST_QUOTAS(HBaseState state) {
        super(state);
    }

    @Override
    public String constructCommandString() {
        return "list_quotas";
    }

    @Override
    public void updateState(State state) {

    }
}
