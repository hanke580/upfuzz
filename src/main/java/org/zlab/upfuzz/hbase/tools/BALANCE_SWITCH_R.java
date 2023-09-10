package org.zlab.upfuzz.hbase.tools;

import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;

public class BALANCE_SWITCH_R extends HBaseCommand {
    // read the status
    public BALANCE_SWITCH_R(HBaseState state) {
        super(state);
    }

    @Override
    public String constructCommandString() {
        return "balance_switch";
    }

    @Override
    public void updateState(State state) {
    }
}
