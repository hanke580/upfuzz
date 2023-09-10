package org.zlab.upfuzz.hbase.quotas;

import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseState;

public class ENABLE_EXCEED_THROTTLE_QUOTA
        extends org.zlab.upfuzz.hbase.HBaseCommand {
    public ENABLE_EXCEED_THROTTLE_QUOTA(HBaseState state) {
        super(state);
    }

    @Override
    public String constructCommandString() {
        return "enable_exceed_throttle_quota";
    }

    @Override
    public void updateState(State state) {

    }
}
