package org.zlab.upfuzz.cassandra.nodetool;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.cassandra.CassandraCommand;
import org.zlab.upfuzz.cassandra.CassandraState;

public class RECOMPRESS_SSTABLES extends CassandraCommand {

    public RECOMPRESS_SSTABLES(CassandraState state) {
        Parameter keyspaceName = chooseKeyspace(state, this, null);
        this.params.add(keyspaceName); // [0]
    }

    @Override
    public String constructCommandString() {
        return "recompress_sstables " + params.get(0).toString();
    }

    @Override
    public void updateState(State state) {

    }
}
