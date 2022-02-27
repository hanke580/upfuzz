package org.zlab.upfuzz;

import org.zlab.upfuzz.cassandra.CassandraState;

public interface Command {
    public String constructCommandString();
    void updateState(CassandraState state);
}
