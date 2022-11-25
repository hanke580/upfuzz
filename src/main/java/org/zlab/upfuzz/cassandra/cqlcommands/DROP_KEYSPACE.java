package org.zlab.upfuzz.cassandra.cqlcommands;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.cassandra.CassandraCommand;
import org.zlab.upfuzz.cassandra.CassandraState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;

public class DROP_KEYSPACE extends CassandraCommand {
    public DROP_KEYSPACE(State state) {
        super();

        assert state instanceof CassandraState;
        CassandraState cassandraState = (CassandraState) state;

        Parameter keyspaceName = chooseKeyspace(cassandraState, this, null);
        this.params.add(keyspaceName); // 0

        ParameterType.ConcreteType IF_EXISTType = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("IF EXISTS"), null // TODO: Make a
        // pure
        // CONSTANTType
        );
        Parameter IF_EXIST = IF_EXISTType
                .generateRandomParameter(cassandraState, this);
        params.add(IF_EXIST); // 1
    }

    @Override
    public String constructCommandString() {
        StringBuilder sb = new StringBuilder();
        sb.append("DROP KEYSPACE " + params.get(1));
        sb.append(" " + this.params.get(0) + ";");
        return sb.toString();
    }

    @Override
    public void updateState(State state) {
        ((CassandraState) state).keyspace2tables
                .remove(params.get(0).toString());
        ((CassandraState) state).keyspace2UDTs
                .remove(params.get(0).toString());
    }
}
