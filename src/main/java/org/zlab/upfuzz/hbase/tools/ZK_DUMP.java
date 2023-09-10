package org.zlab.upfuzz.hbase.tools;

import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;

public class ZK_DUMP extends HBaseCommand {
    // read the status
    public ZK_DUMP(HBaseState state) {
        super(state);
    }

    @Override
    public String constructCommandString() {
        return "zk_dump";
    }

    @Override
    public void updateState(State state) {
    }
}
