package org.zlab.upfuzz.hbase.hbasecommands;

import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;

public class STATUS  extends HBaseCommand {
    public STATUS(HBaseState state) {
    }

    @Override
    public String constructCommandString() {
        return "STATUS";
    }

    @Override
    public void updateState(State state) {
    }
}
