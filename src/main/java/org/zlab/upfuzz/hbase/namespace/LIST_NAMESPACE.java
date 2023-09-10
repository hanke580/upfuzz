package org.zlab.upfuzz.hbase.namespace;

import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;

public class LIST_NAMESPACE extends HBaseCommand {
    public LIST_NAMESPACE(HBaseState state) {
        super(state);
    }

    @Override
    public String constructCommandString() {
        return "list_namespace";
    }

    @Override
    public void updateState(State state) {
    }
}
