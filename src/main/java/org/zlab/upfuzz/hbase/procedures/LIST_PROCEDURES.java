package org.zlab.upfuzz.hbase.procedures;

import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;

public class LIST_PROCEDURES extends HBaseCommand {
    public LIST_PROCEDURES(HBaseState state) {
        super(state);
    }

    @Override
    public String constructCommandString() {
        return "list_procedures";
    }

    @Override
    public void updateState(State state) {
    }
}
