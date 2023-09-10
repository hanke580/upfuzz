package org.zlab.upfuzz.hbase.tools;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;

public class COMPACTION_STATE extends HBaseCommand {
    public COMPACTION_STATE(HBaseState state) {
        super(state);
        Parameter tableName = chooseTable(state, this, null);
        this.params.add(tableName); // 0 tableName
    }

    @Override
    public String constructCommandString() {
        String tableName = "'" + params.get(0).toString() + "'";
        return "compaction_state " + tableName;
    }

    @Override
    public void updateState(State state) {
    }
}
