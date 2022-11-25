package org.zlab.upfuzz.cassandra.cqlcommands;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.cassandra.CassandraCommand;
import org.zlab.upfuzz.cassandra.CassandraState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.utils.STRINGType;
import org.zlab.upfuzz.utils.Utilities;

public class DROP_TYPE extends CassandraCommand {
    public DROP_TYPE(State state) {
        super();

        assert state instanceof CassandraState;
        CassandraState cassandraState = (CassandraState) state;

        Parameter keyspaceName = chooseKeyspace(cassandraState, this, null);
        this.params.add(keyspaceName); // 0

        ParameterType.ConcreteType typeNameType = new ParameterType.InCollectionType(
                new STRINGType(10),
                (s, c) -> Utilities
                        .strings2Parameters(((CassandraState) s).keyspace2UDTs
                                .get(this.params.get(0).toString())),
                null);

        Parameter typeName = typeNameType
                .generateRandomParameter(cassandraState, this);
        params.add(typeName); // 1

        ParameterType.ConcreteType IF_EXISTType = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("IF EXISTS"), null // TODO: Make a
        // pure
        // CONSTANTType
        );
        Parameter IF_EXIST = IF_EXISTType
                .generateRandomParameter(cassandraState, this);
        params.add(IF_EXIST); // 2
    }

    @Override
    public String constructCommandString() {
        StringBuilder sb;
        sb = new StringBuilder();
        sb.append("DROP TYPE ").append(params.get(2));
        // sb.append(" " + this.params.get(0).toString() + "." + ";");
        sb.append(" " + this.params.get(0).toString() + "."
                + this.params.get(1).toString() + ";");
        return sb.toString();
    }

    @Override
    public void updateState(State state) {
        ((CassandraState) state).keyspace2UDTs
                .get(this.params.get(0).toString())
                .remove(this.params.get(1).toString());
    }
}
