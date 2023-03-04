package org.zlab.upfuzz.hbase.DataDefinitionCommands;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;

public class IS_ENABLED extends HBaseCommand {
    public IS_ENABLED(HBaseState state) {
        Parameter tableName = chooseTable(state, this, null);
        this.params.add(tableName); // 0 tableName
    }

    @Override
    public String constructCommandString() {
        return "IS_ENABLED " + "'" + params.get(0) + "'";
    }

    @Override
    public void updateState(State state) {
    }
}
