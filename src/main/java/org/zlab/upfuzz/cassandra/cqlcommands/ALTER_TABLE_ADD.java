package org.zlab.upfuzz.cassandra.cqlcommands;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.cassandra.CassandraCommand;
import org.zlab.upfuzz.cassandra.CassandraState;
import org.zlab.upfuzz.cassandra.CassandraTypes;
import org.zlab.upfuzz.utils.PAIRType;
import org.zlab.upfuzz.utils.Pair;
import org.zlab.upfuzz.utils.STRINGType;

/**
 * ALTER TABLE [keyspace_name.] table_name
 * [DROP column_list];
 */
public class ALTER_TABLE_ADD extends CassandraCommand {

    public ALTER_TABLE_ADD(State state, Object init0, Object init1,
            Object init2, Object init3) {
        super();

        assert state instanceof CassandraState;
        CassandraState cassandraState = (CassandraState) state;

        Parameter keyspaceName = chooseKeyspace(cassandraState, this, init0);
        this.params.add(keyspaceName);

        Parameter TableName = chooseTable(cassandraState, this, init1);
        this.params.add(TableName);

        /**
         * Add a column
         * - Must not be in the original column list
         * - Pair type <String, TYPEType>
         */

        ParameterType.ConcreteType addColumnNameType = new ParameterType.NotInCollectionType(
                new ParameterType.NotEmpty(new STRINGType(10)),
                (s, c) -> ((CassandraState) s).getTable(
                        c.params.get(0).toString(),
                        c.params.get(1).toString()).colName2Type,
                p -> ((Pair) ((Parameter) p).getValue()).left);
        Parameter addColumnName = addColumnNameType
                .generateRandomParameter(cassandraState, this, init2);
        this.params.add(addColumnName);

        ParameterType.ConcreteType addColumnTypeType = CassandraTypes.TYPEType.instance;
        Parameter addColumnType = addColumnTypeType
                .generateRandomParameter(cassandraState, this, init3);
        this.params.add(addColumnType);
    }

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
                new ParameterType.NotEmpty(new STRINGType(10)),
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
    }

    @Override
    public String constructCommandString() {
        return "ALTER TABLE" +
                " " + this.params.get(0) + "."
                + this.params.get(1).toString() + " " +
                "ADD" +
                " " + this.params.get(2).toString() + " "
                + this.params.get(3).toString() + " ;";
    }

    @Override
    public void updateState(State state) {

        ParameterType.ConcreteType columnType = ParameterType.ConcreteGenericType
                .constructConcreteGenericType(PAIRType.instance,
                        new ParameterType.NotEmpty(new STRINGType(10)),
                        CassandraTypes.TYPEType.instance);

        // The mechanism is incorrect here
        // the newly added column should have the same type like the other
        // columns
        // One way is to clone a type here or create a same type
        // But not urgent for now

        Parameter p = new Parameter(columnType,
                new Pair<>(params.get(2), params.get(3)));
        ((CassandraState) state).getTable(this.params.get(0).toString(),
                this.params.get(1).toString()) // Get the table to modify
                        .colName2Type.add(p);
    }
}
