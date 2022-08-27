package org.zlab.upfuzz.cassandra.cqlcommands;

import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.cassandra.CassandraCommands;
import org.zlab.upfuzz.cassandra.CassandraState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.utils.Pair;
import org.zlab.upfuzz.utils.STRINGType;
import org.zlab.upfuzz.utils.Utilities;

public class CREATE_INDEX extends CassandraCommands {

    public CREATE_INDEX(State state) {
        super();

        assert state instanceof CassandraState;
        CassandraState cassandraState = (CassandraState) state;

        Parameter keyspaceName = chooseKeyspace(cassandraState, this, null);
        this.params.add(keyspaceName); // P0

        Parameter TableName = chooseTable(cassandraState, this, null);
        this.params.add(TableName); // P1

        ParameterType.ConcreteType indexNameType = new ParameterType.NotInCollectionType(
                new ParameterType.NotEmpty(STRINGType.instance),
                (s, c) -> Utilities
                        .strings2Parameters(((CassandraState) s).getTable(
                                c.params.get(0).toString(),
                                c.params.get(1).toString()).indexes),
                null);
        Parameter indexName = indexNameType.generateRandomParameter(state,
                this);
        this.params.add(indexName); // P2

        ParameterType.ConcreteType indexColumnType = new ParameterType.InCollectionType(
                null,
                (s, c) -> ((CassandraState) s).getTable(
                        c.params.get(0).toString(),
                        c.params.get(1).toString()).colName2Type,
                null, null);
        Parameter indexColumn = indexColumnType
                .generateRandomParameter(cassandraState, this);
        this.params.add(indexColumn); // P3

        ParameterType.ConcreteType IF_NOT_EXISTType = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("IF NOT EXISTS"), null // TODO: Make
        // a pure
        // CONSTANTType
        );
        Parameter IF_NOT_EXIST = IF_NOT_EXISTType
                .generateRandomParameter(state, this);
        params.add(IF_NOT_EXIST); // P4

        updateExecutableCommandString();
    }

    @Override
    public String constructCommandString() {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE INDEX");
        sb.append(" " + this.params.get(4) + " " + this.params.get(2)
                + " ON");
        sb.append(" " + this.params.get(0) + "."
                + this.params.get(1).toString() + " ");
        sb.append("( "
                + ((Pair) this.params.get(3).getValue()).left.toString()
                + ");");
        return sb.toString();
    }

    @Override
    public void updateState(State state) {
        ((CassandraState) state).getTable(this.params.get(0).toString(),
                this.params.get(1).toString()).indexes
                        .add(this.params.get(2).toString());
    }
}
