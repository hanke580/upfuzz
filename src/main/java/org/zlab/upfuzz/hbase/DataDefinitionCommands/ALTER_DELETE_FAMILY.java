package org.zlab.upfuzz.hbase.DataDefinitionCommands;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;

public class ALTER_DELETE_FAMILY extends HBaseCommand {

    public ALTER_DELETE_FAMILY(HBaseState state) {
        Parameter tableName = chooseTable(state, this, null);
        this.params.add(tableName); // [0] table name

        Parameter columnFamilyName = chooseColumnFamily(state, this, null);
        this.params.add(columnFamilyName); // [1] column family name
    }

    @Override
    public String constructCommandString() {
        Parameter tableName = params.get(0);
        Parameter columnFamilyName = params.get(1);

        return "ALTER "
                + "'" + tableName.toString() + "', 'delete' => "
                + "'" + columnFamilyName.toString() + "'";
    }

    @Override
    public void updateState(State state) {
        Parameter tableName = params.get(0);
        Parameter columnFamilyName = params.get(1);
        ((HBaseState) state).deleteColumnFamily(
                tableName.toString(),
                columnFamilyName.toString());
    }
}
