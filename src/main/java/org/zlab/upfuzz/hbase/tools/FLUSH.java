package org.zlab.upfuzz.hbase.tools;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;

public class FLUSH extends HBaseCommand {
    public FLUSH(HBaseState state) {
        super(state);
        Parameter tableName = chooseTable(state, this, null);
        this.params.add(tableName); // [0] table name

        Parameter columnFamilyName = chooseOptionalColumnFamily(state, this);
        this.params.add(columnFamilyName); // [1] column family name
    }

    @Override
    public String constructCommandString() {
        String tableName = params.get(0).toString();
        String columnFamilyName = params.get(1).toString().isEmpty() ? ""
                : ", '" + params.get(1).toString() + "'";
        return "flush " + "'" + tableName + "'" + columnFamilyName;
    }

    @Override
    public void updateState(State state) {
    }
}
