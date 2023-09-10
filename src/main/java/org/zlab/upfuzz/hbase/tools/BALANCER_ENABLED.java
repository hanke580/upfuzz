package org.zlab.upfuzz.hbase.tools;

import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;

public class BALANCER_ENABLED extends HBaseCommand {
    // read
    // The balancer_enabled command in HBase is used to check whether the
    // automatic balancing of regions across RegionServers is currently enabled
    // or not.
    public BALANCER_ENABLED(HBaseState state) {
        super(state);
    }

    @Override
    public String constructCommandString() {
        return "balancer_enabled";
    }

    @Override
    public void updateState(State state) {
    }
}
