package org.zlab.upfuzz.hbase.ddl;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;

public class DROP extends HBaseCommand {
    public DROP(HBaseState state) {
        super(state);
        Parameter tableName = chooseTable(state, this, null);
        this.params.add(tableName); // 0 tableName
    }

    @Override
    public String constructCommandString() {
        return "drop " + "'" + params.get(0) + "'";
    }

    @Override
    public void updateState(State state) {
        ((HBaseState) state).deleteTable(params.get(0).toString());
    }
}
