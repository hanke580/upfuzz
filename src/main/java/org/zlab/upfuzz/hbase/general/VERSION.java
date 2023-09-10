package org.zlab.upfuzz.hbase.general;

import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;

public class VERSION extends HBaseCommand {
    public VERSION(HBaseState state) {
        super(state);
    }

    @Override
    public String constructCommandString() {
        return "version";
    }

    @Override
    public void updateState(State state) {
    }
}
