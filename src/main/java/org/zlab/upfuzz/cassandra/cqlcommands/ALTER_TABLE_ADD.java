package org.zlab.upfuzz.cassandra.cqlcommands;

import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.cassandra.CassandraCommands;
import org.zlab.upfuzz.cassandra.CassandraState;
import org.zlab.upfuzz.cassandra.CassandraTypes;
import org.zlab.upfuzz.utils.PAIRType;
import org.zlab.upfuzz.utils.Pair;
import org.zlab.upfuzz.utils.STRINGType;

/**
 * ALTER TABLE [keyspace_name.] table_name
 * [DROP column_list];
 */
public class ALTER_TABLE_ADD extends CassandraCommands {

    public ALTER_TABLE_ADD(State state) {
        super();

        assert state instanceof CassandraState;
        CassandraState cassandraState = (CassandraState) state;

        Parameter keyspaceName = chooseKeyspace(cassandraState, this, null);
        this.params.add(keyspaceName);

        Parameter TableName = chooseTable(cassandraState, this, null);
        this.params.add(TableName);

        /**
         * Add a column
         * - Must not be in the original column list
         * - Pair type <String, TYPEType>
         */

        ParameterType.ConcreteType addColumnNameType = new ParameterType.NotInCollectionType(
                new ParameterType.NotEmpty(STRINGType.instance),
                (s, c) -> ((CassandraState) s).getTable(
                        c.params.get(0).toString(),
                        c.params.get(1).toString()).colName2Type,
                p -> ((Pair) ((Parameter) p).getValue()).left);
        Parameter addColumnName = addColumnNameType
                .generateRandomParameter(cassandraState, this);
        this.params.add(addColumnName);

        ParameterType.ConcreteType addColumnTypeType = CassandraTypes.TYPEType.instance;
        Parameter addColumnType = addColumnTypeType
                .generateRandomParameter(cassandraState, this);
        this.params.add(addColumnType);

        updateExecutableCommandString();
    }

    @Override
    public String constructCommandString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ALTER TABLE");
        sb.append(" " + this.params.get(0) + "."
                + this.params.get(1).toString() + " ");
        sb.append("ADD");
        sb.append(" " + this.params.get(2).toString() + " "
                + this.params.get(3).toString() + " ;");
        return sb.toString();
    }

    @Override
    public void updateState(State state) {

        ParameterType.ConcreteType columnType = ParameterType.ConcreteGenericType
                .constructConcreteGenericType(PAIRType.instance,
                        new ParameterType.NotEmpty(STRINGType.instance),
                        CassandraTypes.TYPEType.instance);

        Parameter p = new Parameter(columnType,
                new Pair<>(params.get(2), params.get(3)));
        ((CassandraState) state).getTable(this.params.get(0).toString(),
                this.params.get(1).toString()) // Get the table to modify
                        .colName2Type.add(p);
    }
}