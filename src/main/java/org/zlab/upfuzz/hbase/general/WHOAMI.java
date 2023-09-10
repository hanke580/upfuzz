package org.zlab.upfuzz.hbase.general;

import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;

public class WHOAMI extends HBaseCommand {
    public WHOAMI(HBaseState state) {
        super(state);
    }

    @Override
    public String constructCommandString() {
        return "whoami";
    }

    @Override
    public void updateState(State state) {
    }
}
