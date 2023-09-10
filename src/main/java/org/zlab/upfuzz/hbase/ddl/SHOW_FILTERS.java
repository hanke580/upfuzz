package org.zlab.upfuzz.hbase.ddl;

import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;

public class SHOW_FILTERS extends HBaseCommand {
    public SHOW_FILTERS(HBaseState state) {
        super(state);
    }

    @Override
    public String constructCommandString() {
        return "show_filters";
    }

    @Override
    public void updateState(State state) {
    }
}
