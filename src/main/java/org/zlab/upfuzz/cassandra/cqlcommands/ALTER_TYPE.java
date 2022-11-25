package org.zlab.upfuzz.cassandra.cqlcommands;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.cassandra.CassandraCommand;
import org.zlab.upfuzz.cassandra.CassandraState;
import org.zlab.upfuzz.cassandra.CassandraTypes;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.utils.STRINGType;
import org.zlab.upfuzz.utils.Utilities;

public class ALTER_TYPE extends CassandraCommand {

    public ALTER_TYPE(CassandraState cassandraState) {

        Parameter keyspaceName = chooseKeyspace(cassandraState, this, null);
        params.add(keyspaceName);

        // select a type
        ParameterType.ConcreteType udtType = new ParameterType.InCollectionType(
                CONSTANTSTRINGType.instance,
                (s, c) -> Utilities.strings2Parameters(
                        ((CassandraState) s).keyspace2UDTs
                                .get(params.get(0).toString())),
                null);
        Parameter udt = udtType.generateRandomParameter(cassandraState, this,
                null);
        params.add(udt);

        ParameterType.ConcreteType colNameType = new ParameterType.NotEmpty(
                new STRINGType(10));
        Parameter colName = colNameType.generateRandomParameter(cassandraState,
                this);
        params.add(colName);

        // alter type of it
        ParameterType.ConcreteType newTypeType = CassandraTypes.TYPEType.instance;
        Parameter newType = newTypeType
                .generateRandomParameter(cassandraState, this);
        this.params.add(newType);
    }

    @Override
    public String constructCommandString() {
        return "ALTER TYPE " +
                params.get(0).toString() + "." + params.get(1).toString()
                + " ALTER " + params.get(2).toString() + " TYPE " +
                params.get(3) + ";";
    }

    @Override
    public void updateState(State state) {

    }
}
