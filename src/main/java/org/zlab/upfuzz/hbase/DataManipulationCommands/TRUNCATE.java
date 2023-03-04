package org.zlab.upfuzz.hbase.DataManipulationCommands;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;

public class TRUNCATE extends HBaseCommand {
    public TRUNCATE(HBaseState state) {
        Parameter tableName = chooseTable(state, this, null);
        this.params.add(tableName); // 0 tableName
    }

    @Override
    public String constructCommandString() {
        return "TRUNCATE " + "'" + params.get(0) + "'";
    }

    @Override
    public void updateState(State state) {
    }
}
