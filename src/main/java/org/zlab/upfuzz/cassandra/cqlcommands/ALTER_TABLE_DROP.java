package org.zlab.upfuzz.cassandra.cqlcommands;

import org.zlab.upfuzz.*;
import org.zlab.upfuzz.cassandra.CassandraCommand;
import org.zlab.upfuzz.cassandra.CassandraState;
import org.zlab.upfuzz.cassandra.CassandraTable;
import org.zlab.upfuzz.utils.Pair;

/**
 * ALTER TABLE [keyspace_name.] table_name
 * [DROP column_list];
 */
public class ALTER_TABLE_DROP extends CassandraCommand {
    public ALTER_TABLE_DROP(State state, Object init0, Object init1,
            Object init2) {
        super();

        assert state instanceof CassandraState;
        CassandraState cassandraState = (CassandraState) state;

        Parameter keyspaceName = chooseKeyspace(cassandraState, this,
                init0);
        this.params.add(keyspaceName);

        Parameter TableName = chooseTable(cassandraState, this, init1);
        this.params.add(TableName);

        Predicate predicate = (s, c) -> {
            assert c instanceof ALTER_TABLE_DROP;
            CassandraTable cassandraTable = ((CassandraState) s).getTable(
                    c.params.get(0).toString(), c.params.get(1).toString());
            return cassandraTable.colName2Type
                    .size() != cassandraTable.primaryColName2Type.size();
        };

        ParameterType.ConcreteType dropColumnType = new ParameterType.NotInCollectionType(
                new ParameterType.InCollectionType(null,
                        (s, c) -> ((CassandraState) s).getTable(
                                c.params.get(0).toString(),
                                c.params.get(1).toString()).colName2Type,
                        null, predicate),
                (s, c) -> ((CassandraState) s).getTable(
                        c.params.get(0).toString(),
                        c.params.get(1).toString()).primaryColName2Type,
                null);
        Parameter dropColumn = dropColumnType
                .generateRandomParameter(cassandraState, this, init2);
        this.params.add(dropColumn);
    }

    public ALTER_TABLE_DROP(State state) {
        super();

        assert state instanceof CassandraState;
        CassandraState cassandraState = (CassandraState) state;

        Parameter keyspaceName = chooseKeyspace(cassandraState, this, null);
        this.params.add(keyspaceName);

        Parameter TableName = chooseTable(cassandraState, this, null);
        this.params.add(TableName);

        Predicate predicate = (s, c) -> {
            assert c instanceof ALTER_TABLE_DROP;
            CassandraTable cassandraTable = ((CassandraState) s).getTable(
                    c.params.get(0).toString(), c.params.get(1).toString());
            return cassandraTable.colName2Type
                    .size() != cassandraTable.primaryColName2Type.size();
        };
        /**
         * FIXME: About the Predicate. Two ways
         * Keep the retry times, if it retrys for many times, throw a warning about the constraints?
         * Retry a few times, if not success, it fails.
         */

        ParameterType.ConcreteType dropColumnType = new ParameterType.NotInCollectionType(
                new ParameterType.InCollectionType(null,
                        (s, c) -> ((CassandraState) s).getTable(
                                c.params.get(0).toString(),
                                c.params.get(1).toString()).colName2Type,
                        null, predicate),
                (s, c) -> ((CassandraState) s).getTable(
                        c.params.get(0).toString(),
                        c.params.get(1).toString()).primaryColName2Type,
                null);
        Parameter dropColumn = dropColumnType
                .generateRandomParameter(cassandraState, this);
        this.params.add(dropColumn);
    }

    @Override
    public String constructCommandString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ALTER TABLE");
        sb.append(" " + this.params.get(0) + "."
                + this.params.get(1).toString() + " ");
        sb.append("DROP");
        sb.append(
                " " + ((Pair) this.params.get(2).getValue()).left.toString()
                        + " ;");
        return sb.toString();
    }

    @Override
    public void updateState(State state) {
        CassandraTable table = ((CassandraState) state).getTable(
                this.params.get(0).toString(),
                this.params.get(1).toString());
        if (table == null) {
            throw new RuntimeException(
                    "Table not found: ks = " + this.params.get(0).toString()
                            + ", table = " + this.params.get(1).toString());
        }
        table.colName2Type.removeIf(value -> value.toString()
                .equals(this.params.get(2).toString()));
    }
}