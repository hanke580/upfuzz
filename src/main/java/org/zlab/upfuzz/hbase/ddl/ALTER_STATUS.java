package org.zlab.upfuzz.hbase.ddl;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;

public class ALTER_STATUS extends HBaseCommand {
    // Inquire the current status of alter command on a table: might introduce
    // FP

    public ALTER_STATUS(HBaseState state) {
        super(state);
        Parameter tableName = chooseTable(state, this, null);
        this.params.add(tableName); // 0 tableName
    }

    @Override
    public String constructCommandString() {
        return "alter_status " + "'" + params.get(0) + "'";
    }

    @Override
    public void updateState(State state) {
    }
}
