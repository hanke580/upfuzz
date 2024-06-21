package org.zlab.upfuzz.cassandra.cqlcommands;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.cassandra.CassandraCommand;
import org.zlab.upfuzz.cassandra.CassandraState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.utils.INTType;
import org.zlab.upfuzz.utils.UUIDType;

public class CREATE_KEYSPACE extends CassandraCommand {

    public CREATE_KEYSPACE(State state, Object init0, Object init1,
            Object init2) {
        super();

        ParameterType.ConcreteType keyspaceNameType = new ParameterType.NotInCollectionType(
                new ParameterType.NotEmpty(UUIDType.instance),
                (s, c) -> ((CassandraState) s).getKeyspaces(), null);
        Parameter keyspaceName = keyspaceNameType
                .generateRandomParameter(state, this, init0);
        this.params.add(keyspaceName); // [0]

        ParameterType.ConcreteType replicationFactorType = new INTType(1,
                4);
        Parameter replicationFactor = replicationFactorType
                .generateRandomParameter(state, this, init1);
        this.params.add(replicationFactor); // [1]

        ParameterType.ConcreteType IF_NOT_EXISTType = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("IF NOT EXISTS"), null // TODO: Make
        // a pure
        // CONSTANTType
        );
        Parameter IF_NOT_EXIST = IF_NOT_EXISTType
                .generateRandomParameter(state, this, init2);
        params.add(IF_NOT_EXIST); // [2]
    }

    public CREATE_KEYSPACE(State state) {
        super();

        ParameterType.ConcreteType keyspaceNameType = new ParameterType.LessLikelyMutateType(
                new ParameterType.NotInCollectionType(
                        new ParameterType.NotEmpty(UUIDType.instance),
                        (s, c) -> ((CassandraState) s).getKeyspaces(), null),
                0.1);
        Parameter keyspaceName = keyspaceNameType
                .generateRandomParameter(state, this);
        this.params.add(keyspaceName); // [0]

        ParameterType.ConcreteType replicationFactorType = new INTType(1,
                4);
        Parameter replicationFactor = replicationFactorType
                .generateRandomParameter(state, this);
        this.params.add(replicationFactor); // [1]

        ParameterType.ConcreteType IF_NOT_EXISTType = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("IF NOT EXISTS"), null
        // TODO: Make a pure CONSTANTType
        );
        Parameter IF_NOT_EXIST = IF_NOT_EXISTType
                .generateRandomParameter(state, this);
        params.add(IF_NOT_EXIST); // [2]
    }

    @Override
    public String constructCommandString() {
        return "CREATE KEYSPACE" + " " + this.params.get(2).toString()
                + " " + this.params.get(0).toString() + " " +
                "WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' :"
                + " " +
                this.params.get(1).toString() + " " + "};";
    }

    @Override
    public void updateState(State state) {
        ((CassandraState) state).addKeyspace(this.params.get(0).toString());
    }

    @Override
    public void separate(State state) {
        this.params.get(0).regenerate(null, this);
    }
}
