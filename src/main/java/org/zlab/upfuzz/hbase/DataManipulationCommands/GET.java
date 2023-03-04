package org.zlab.upfuzz.hbase.DataManipulationCommands;

import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;

public class GET extends HBaseCommand {
    public GET(HBaseState state){}

    @Override
    public String constructCommandString() {
        return "GET " + "'" + params.get(0) + "'";
    }

    @Override
    public void updateState(State state) {
    }
}
