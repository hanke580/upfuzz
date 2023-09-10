package org.zlab.upfuzz.hbase;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;

public class HbaseSimpleCommandHelper extends HBaseCommand {
    String command;

    public HbaseSimpleCommandHelper(HBaseState state, String command) {
        super(state);
        // choose an existing table
        this.command = command;
    }

    @Override
    public String constructCommandString() {
        return command;
    }

    @Override
    public void updateState(State state) {

    }
}
