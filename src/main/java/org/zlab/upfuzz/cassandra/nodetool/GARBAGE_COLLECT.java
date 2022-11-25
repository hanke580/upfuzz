package org.zlab.upfuzz.cassandra.nodetool;

import org.zlab.upfuzz.State;
import org.zlab.upfuzz.cassandra.CassandraCommand;
import org.zlab.upfuzz.cassandra.CassandraState;

public class GARBAGE_COLLECT extends CassandraCommand {

    public GARBAGE_COLLECT(CassandraState state) {
    }

    @Override
    public String constructCommandString() {
        return "garbagecollect";
    }

    @Override
    public void updateState(State state) {

    }
}
