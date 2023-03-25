package org.zlab.upfuzz.hbase.DataManipulationCommands;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;

public class GET extends HBaseCommand {
    public GET(HBaseState state) {
        Parameter tableName = chooseTable(state, this, null);
        this.params.add(tableName); // [0] table name

        Parameter rowKey = chooseRowKey(state, this, null);
        this.params.add(rowKey); // [1] row key
    }

    @Override
    public String constructCommandString() {
        return "GET " + "'" + params.get(0) + "', " + "'" + params.get(1) + "'";
    }

    @Override
    public void updateState(State state) {
    }
}
