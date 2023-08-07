package org.zlab.upfuzz.hbase.DataDefinitionCommands;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;

public class LIST extends HBaseCommand {
    public LIST(HBaseState state) {
    }

    @Override
    public String constructCommandString() {
        return "list";
    }

    @Override
    public void updateState(State state) {
    }
}
