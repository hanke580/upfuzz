package org.zlab.upfuzz.hbase.tools;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;

public class CLEAR_BLOCK_CACHE extends HBaseCommand {
    public CLEAR_BLOCK_CACHE(HBaseState state) {
        super(state);
        Parameter tableName = chooseTable(state, this, null);
        this.params.add(tableName); // 0 tableName
    }

    @Override
    public String constructCommandString() {
        String tableName = "'" + params.get(0).toString() + "'";
        return "clear_block_cache " + tableName;
    }

    @Override
    public void updateState(State state) {
    }
}
