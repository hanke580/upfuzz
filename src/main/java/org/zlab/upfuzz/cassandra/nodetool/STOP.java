package org.zlab.upfuzz.cassandra.nodetool;

import org.zlab.upfuzz.State;
import org.zlab.upfuzz.cassandra.CassandraCommand;
import org.zlab.upfuzz.cassandra.CassandraState;

public class STOP extends CassandraCommand {

    public STOP(CassandraState state) {
    }

    @Override
    public String constructCommandString() {
        return "stop";
    }

    @Override
    public void updateState(State state) {

    }
}
