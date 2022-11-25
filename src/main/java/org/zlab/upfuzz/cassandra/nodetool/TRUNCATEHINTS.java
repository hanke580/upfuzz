package org.zlab.upfuzz.cassandra.nodetool;

import org.zlab.upfuzz.State;
import org.zlab.upfuzz.cassandra.CassandraCommand;
import org.zlab.upfuzz.cassandra.CassandraState;

public class TRUNCATEHINTS extends CassandraCommand {

    public TRUNCATEHINTS(CassandraState state) {
    }

    @Override
    public String constructCommandString() {
        return "truncatehints";
    }

    @Override
    public void updateState(State state) {

    }
}
