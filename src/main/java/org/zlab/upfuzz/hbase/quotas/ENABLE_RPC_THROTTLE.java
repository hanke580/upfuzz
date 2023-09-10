package org.zlab.upfuzz.hbase.quotas;

import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseState;

public class ENABLE_RPC_THROTTLE
        extends org.zlab.upfuzz.hbase.HBaseCommand {
    public ENABLE_RPC_THROTTLE(HBaseState state) {
        super(state);
    }

    @Override
    public String constructCommandString() {
        return "enable_rpc_throttle";
    }

    @Override
    public void updateState(State state) {

    }
}
