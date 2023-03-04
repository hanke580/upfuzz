package org.zlab.upfuzz.hbase.DataDefinitionCommands;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;

public class DROP extends HBaseCommand {
    public DROP(HBaseState state) {
        Parameter tableName = chooseTable(state, this, null);
        this.params.add(tableName); // 0 tableName
    }

    @Override
    public String constructCommandString() {
        return "DROP " + "'" + params.get(0) + "'";
    }

    @Override
    public void updateState(State state) {
        ((HBaseState) state).table2families
                .remove(params.get(0).toString());
    }
}
