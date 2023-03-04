package org.zlab.upfuzz.hbase.hbasecommands;

import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;

public class VERSION  extends HBaseCommand {
    public VERSION(HBaseState state) {
    }

    @Override
    public String constructCommandString() {
        return "VERSION";
    }

    @Override
    public void updateState(State state) {
    }
}
