package org.zlab.upfuzz.cassandra.cqlcommands;

import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.cassandra.CassandraCommands;
import org.zlab.upfuzz.cassandra.CassandraState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;

public class DROP_INDEX extends CassandraCommands {
    public DROP_INDEX(State state) {
        super();

        assert state instanceof CassandraState;
        CassandraState cassandraState = (CassandraState) state;

        Parameter keyspaceName = chooseKeyspace(cassandraState, this, null);
        this.params.add(keyspaceName); // 0

        Parameter TableName = chooseTable(cassandraState, this, null);
        this.params.add(TableName); // 1

        ParameterType.ConcreteType indexNameType = new ParameterType.InCollectionType(
                CONSTANTSTRINGType.instance,
                (s, c) -> ((CassandraState) s).getTable(
                        c.params.get(0).toString(),
                        c.params.get(1).toString()).indexes,
                null);
        Parameter indexName = indexNameType.generateRandomParameter(state,
                this);
        this.params.add(indexName); // 2

        ParameterType.ConcreteType IF_EXISTType = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("IF EXISTS"), null // TODO: Make a
        // pure
        // CONSTANTType
        );
        Parameter IF_EXIST = IF_EXISTType
                .generateRandomParameter(cassandraState, this);
        params.add(IF_EXIST); // 3

        updateExecutableCommandString();
    }

    @Override
    public String constructCommandString() {
        StringBuilder sb = new StringBuilder();
        sb.append("DROP INDEX ").append(params.get(3))
                .append(" " + this.params.get(0) + "."
                        + this.params.get(2).toString() + ";");
        return sb.toString();
    }

    @Override
    public void updateState(State state) {
        ((CassandraState) state).getTable(params.get(0).toString(),
                params.get(1).toString()).indexes
                        .remove(params.get(2).toString());
    }
}
