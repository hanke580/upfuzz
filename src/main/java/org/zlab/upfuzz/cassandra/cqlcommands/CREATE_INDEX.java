package org.zlab.upfuzz.cassandra.cqlcommands;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.cassandra.CassandraCommand;
import org.zlab.upfuzz.cassandra.CassandraState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.utils.Pair;
import org.zlab.upfuzz.utils.STRINGType;
import org.zlab.upfuzz.utils.Utilities;

public class CREATE_INDEX extends CassandraCommand {

    public CREATE_INDEX(CassandraState state) {

        Parameter keyspaceName = chooseKeyspace(state, this, null);
        this.params.add(keyspaceName); // P0

        Parameter TableName = chooseTable(state, this, null);
        this.params.add(TableName); // P1

        ParameterType.ConcreteType indexNameType = new ParameterType.NotInCollectionType(
                new ParameterType.NotEmpty(new STRINGType(10)),
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
                .generateRandomParameter(state, this);
        this.params.add(indexColumn); // P3

        ParameterType.ConcreteType IF_NOT_EXISTType = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("IF NOT EXISTS"), null // TODO: Make
        // a pure
        // CONSTANTType
        );
        Parameter IF_NOT_EXIST = IF_NOT_EXISTType
                .generateRandomParameter(state, this);
        params.add(IF_NOT_EXIST); // P4
    }

    @Override
    public String constructCommandString() {
        return "CREATE INDEX" +
                " " + this.params.get(4) + " " + this.params.get(2)
                + " ON" +
                " " + this.params.get(0) + "."
                + this.params.get(1).toString() + " " +
                "( "
                + ((Pair) this.params.get(3).getValue()).left.toString()
                + ");";
    }

    @Override
    public void updateState(State state) {
        ((CassandraState) state).getTable(this.params.get(0).toString(),
                this.params.get(1).toString()).indexes
                        .add(this.params.get(2).toString());
    }
}
