package org.zlab.upfuzz.cassandra.cqlcommands;

import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.cassandra.CassandraCommands;
import org.zlab.upfuzz.cassandra.CassandraState;

public class USE extends CassandraCommands {
    public USE(State state) {
        super();

        assert state instanceof CassandraState;
        CassandraState cassandraState = (CassandraState) state;

        Parameter keyspaceName = chooseKeyspace(cassandraState, this, null);
        this.params.add(keyspaceName); // 0

        updateExecutableCommandString();
    }

    @Override
    public String constructCommandString() {
        StringBuilder sb = new StringBuilder();
        sb.append("USE ").append(params.get(0)).append(";");
        return sb.toString();
    }

    @Override
    public void updateState(State state) {
    }
}
