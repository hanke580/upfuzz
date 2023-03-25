package org.zlab.upfuzz.hbase.hbasecommands;

import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;

public class TABLE_HELP extends HBaseCommand {
    public TABLE_HELP(HBaseState state) {
    }

    @Override
    public String constructCommandString() {
        return "table_help";
    }

    @Override
    public void updateState(State state) {
    }
}
