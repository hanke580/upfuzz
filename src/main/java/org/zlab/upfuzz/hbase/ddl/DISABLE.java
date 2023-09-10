package org.zlab.upfuzz.hbase.ddl;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;

public class DISABLE extends HBaseCommand {
    public DISABLE(HBaseState state) {
        super(state);
        Parameter tableName = chooseTable(state, this, null);
        this.params.add(tableName); // 0 tableName
    }

    @Override
    public String constructCommandString() {
        return "disable " + "'" + params.get(0) + "'";
    }

    @Override
    public void updateState(State state) {
        ((HBaseState) state).disableTable(params.get(0).toString());
    }
}
