package org.zlab.upfuzz.cassandra.cqlcommands;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.cassandra.CassandraCommand;
import org.zlab.upfuzz.cassandra.CassandraState;
import org.zlab.upfuzz.cassandra.CassandraTypes;
import org.zlab.upfuzz.utils.PAIRType;
import org.zlab.upfuzz.utils.STRINGType;
import org.zlab.upfuzz.utils.Utilities;

/**
 * INSERT INTO [keyspace_name.] table_name (column_list)
 * VALUES (column_values)
 * [IF NOT EXISTS]
 * [USING TTL seconds | TIMESTAMP epoch_in_microseconds]
 *
 * E.g.,
 * INSERT INTO cycling.cyclist_name (id, lastname, firstname)
 *    VALUES (c4b65263-fe58-4846-83e8-f0e1c13d518f, 'RATTO', 'Rissella')
 * IF NOT EXISTS;
 */
public class CREATE_TYPE extends CassandraCommand {

    public CREATE_TYPE(State state) {
        super();

        assert state instanceof CassandraState;
        CassandraState cassandraState = (CassandraState) state;

        Parameter keyspaceName = chooseKeyspace(cassandraState, this, null);
        this.params.add(keyspaceName); // 0

        ParameterType.ConcreteType typeNameType = new ParameterType.NotInCollectionType(
                new ParameterType.NotEmpty(new STRINGType(10)),
                (s, c) -> Utilities
                        .strings2Parameters(((CassandraState) s).keyspace2UDTs
                                .get(c.params.get(0).toString())),
                null);
        Parameter typeName = typeNameType
                .generateRandomParameter(cassandraState, this);
        params.add(typeName); // 1

        ParameterType.ConcreteType columnsType = // LIST<PAIR<String,TYPEType>>
                new ParameterType.NotEmpty(ParameterType.ConcreteGenericType
                        .constructConcreteGenericType(
                                CassandraTypes.MapLikeListType.instance,
                                ParameterType.ConcreteGenericType
                                        .constructConcreteGenericType(
                                                PAIRType.instance,
                                                new ParameterType.NotEmpty(
                                                        new STRINGType(10)),
                                                CassandraTypes.TYPEType.instance)));

        Parameter columns = columnsType
                .generateRandomParameter(cassandraState, this);
        params.add(columns); // 2
    }

    @Override
    public String constructCommandString() {

        Parameter keyspaceName = params.get(0);
        Parameter typeName = params.get(1);
        Parameter columns = params.get(2);

        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TYPE " + keyspaceName.toString() + "." + typeName
                + " (" + columns.toString() + ");");
        return sb.toString();
    }

    @Override
    public void updateState(State state) {
        ((CassandraState) state).keyspace2UDTs.get(params.get(0).toString())
                .add(params.get(1).toString());
    }
}