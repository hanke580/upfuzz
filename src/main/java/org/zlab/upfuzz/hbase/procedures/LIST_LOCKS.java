package org.zlab.upfuzz.hbase.procedures;

import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;

public class LIST_LOCKS extends HBaseCommand {
    public LIST_LOCKS(HBaseState state) {
        super(state);
    }

    @Override
    public String constructCommandString() {
        return "list_locks";
    }

    @Override
    public void updateState(State state) {
    }
}
