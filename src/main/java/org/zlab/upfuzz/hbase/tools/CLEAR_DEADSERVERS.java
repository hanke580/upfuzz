package org.zlab.upfuzz.hbase.tools;

import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;

public class CLEAR_DEADSERVERS extends HBaseCommand {
    // read the status
    public CLEAR_DEADSERVERS(HBaseState state) {
        super(state);
    }

    @Override
    public String constructCommandString() {
        return "clear_deadservers";
    }

    @Override
    public void updateState(State state) {
    }
}
