package org.zlab.upfuzz.hbase.configurations;

import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;

public class UPDATE_ALL_CONFIG extends HBaseCommand {
    public UPDATE_ALL_CONFIG(HBaseState state) {
        super(state);
    }

    @Override
    public String constructCommandString() {
        return "update_all_config";
    }

    @Override
    public void updateState(State state) {
    }
}
