package org.zlab.upfuzz.hbase.DataManipulationCommands;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;

public class DELETEALL extends HBaseCommand {

    public DELETEALL(HBaseState state) {
        Parameter TableName = chooseTable(state, this, null);
        this.params.add(TableName); // Param0

        Parameter rowKey = chooseRowKey(state, this, null);
        this.params.add(rowKey); // Param0
    }

    @Override
    public String constructCommandString() {
        return "deleteall " + "'" + params.get(0) + "'"
                + ", " + "'" + params.get(1) + "'";
    }

    @Override
    public void updateState(State state) {
        ((HBaseState) state).deleteRowKey(params.get(0).toString(),
                params.get(1).toString());
    }
}
