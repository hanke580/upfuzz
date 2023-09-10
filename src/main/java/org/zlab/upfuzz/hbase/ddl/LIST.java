package org.zlab.upfuzz.hbase.ddl;

import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;

public class LIST extends HBaseCommand {
    public LIST(HBaseState state) {
        super(state);
    }

    @Override
    public String constructCommandString() {
        return "list";
    }

    @Override
    public void updateState(State state) {
    }
}
