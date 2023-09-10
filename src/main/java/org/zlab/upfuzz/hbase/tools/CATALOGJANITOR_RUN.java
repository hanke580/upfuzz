package org.zlab.upfuzz.hbase.tools;

import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;

public class CATALOGJANITOR_RUN extends HBaseCommand {
    // read the status
    public CATALOGJANITOR_RUN(HBaseState state) {
        super(state);
    }

    @Override
    public String constructCommandString() {
        return "catalogjanitor_run";
    }

    @Override
    public void updateState(State state) {
    }
}
