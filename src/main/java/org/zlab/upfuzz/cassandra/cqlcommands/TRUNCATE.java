package org.zlab.upfuzz.cassandra.cqlcommands;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.cassandra.CassandraCommand;
import org.zlab.upfuzz.cassandra.CassandraState;

public class TRUNCATE extends CassandraCommand {

    public TRUNCATE(CassandraState state) {
        Parameter keyspaceName = chooseKeyspace(state, this,
                null);
        this.params.add(keyspaceName); // [0]

        Parameter TableName = chooseTable(state, this, null);
        this.params.add(TableName); // [1]
    }

    @Override
    public String constructCommandString() {
        return "TRUNCATE TABLE " +
                params.get(0).toString() + "." + params.get(1).toString()
                + ";";
    }

    @Override
    public void updateState(State state) {

    }
}
