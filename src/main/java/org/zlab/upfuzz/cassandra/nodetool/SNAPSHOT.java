package org.zlab.upfuzz.cassandra.nodetool;

import org.zlab.upfuzz.State;
import org.zlab.upfuzz.cassandra.CassandraCommand;
import org.zlab.upfuzz.cassandra.CassandraState;

public class SNAPSHOT extends CassandraCommand {

    public SNAPSHOT(CassandraState state) {
    }

    @Override
    public String constructCommandString() {
        return "snapshot";
    }

    @Override
    public void updateState(State state) {

    }
}
