package org.zlab.upfuzz.hbase.tools;

import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;

public class CATALOGJANITOR_ENABLED extends HBaseCommand {
    // read the status
    public CATALOGJANITOR_ENABLED(HBaseState state) {
        super(state);
    }

    @Override
    public String constructCommandString() {
        return "catalogjanitor_enabled";
    }

    @Override
    public void updateState(State state) {
    }
}
