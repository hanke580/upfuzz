package org.zlab.upfuzz.cassandra.cqlcommands;

import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.cassandra.CassandraCommand;
import org.zlab.upfuzz.cassandra.CassandraState;
import org.zlab.upfuzz.cassandra.CassandraTable;
import org.zlab.upfuzz.utils.Pair;

import java.util.*;

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
public class INSERT extends CassandraCommand {

    public INSERT(CassandraState state, Object init0, Object init1,
            Object init2,
            Object init3) {
        Parameter keyspaceName = chooseKeyspace(state, this,
                init0);
        this.params.add(keyspaceName); // [0]

        Parameter TableName = chooseTable(state, this, init1);
        this.params.add(TableName); // [1]

        ParameterType.ConcreteType columnsType = new ParameterType.SuperSetType(
                new ParameterType.SubsetType(null,
                        (s, c) -> ((CassandraState) s).getTable(
                                c.params.get(0).toString(),
                                c.params.get(1).toString()).colName2Type,
                        null),
                (s, c) -> ((CassandraState) s).getTable(
                        c.params.get(0).toString(),
                        c.params.get(1).toString()).primaryColName2Type,
                null);
        Parameter columns = columnsType
                .generateRandomParameter(state, this, init2);
        this.params.add(columns); // [2]

        ParameterType.ConcreteType insertValuesType = new ParameterType.Type2ValueType(
                null, (s, c) -> (Collection) c.params.get(2).getValue(), // columns
                p -> ((Pair) ((Parameter) p).value).right);
        Parameter insertValues = insertValuesType
                .generateRandomParameter(state, this, init3);
        this.params.add(insertValues); // [3]
    }

    public INSERT(CassandraState state) {
        Parameter keyspaceName = chooseKeyspace(state, this, null);
        this.params.add(keyspaceName);

        Parameter TableName = chooseTable(state, this, null);
        this.params.add(TableName);

        ParameterType.ConcreteType columnsType = new ParameterType.SuperSetType(
                new ParameterType.SubsetType(null,
                        (s, c) -> ((CassandraState) s).getTable(
                                c.params.get(0).toString(),
                                c.params.get(1).toString()).colName2Type,
                        null),
                (s, c) -> ((CassandraState) s).getTable(
                        c.params.get(0).toString(),
                        c.params.get(1).toString()).primaryColName2Type,
                null);
        Parameter columns = columnsType
                .generateRandomParameter(state, this);
        this.params.add(columns);

        ParameterType.ConcreteType insertValuesType = new ParameterType.Type2ValueType(
                null, (s, c) -> (Collection) c.params.get(2).getValue(), // columns
                p -> ((Pair) ((Parameter) p).value).right);
        Parameter insertValues = insertValuesType
                .generateRandomParameter(state, this);
        this.params.add(insertValues);
    }

    @Override
    public String constructCommandString() {
        Parameter keyspaceName = params.get(0);
        Parameter tableName = params.get(1);
        ParameterType.ConcreteType columnNameType = new ParameterType.StreamMapType(
                null, (s, c) -> (Collection) c.params.get(2).getValue(),
                p -> ((Pair) ((Parameter) p).getValue()).left);
        Parameter columnName = columnNameType.generateRandomParameter(null,
                this);
        Parameter insertValues = params.get(3);

        return "INSERT INTO " + keyspaceName.toString() + "."
                + tableName.toString() + " (" + columnName.toString()
                + ") VALUES (" + insertValues.toString() + ");";
    }

    @Override
    public void updateState(State state) {
    }

    @Override
    public Set<Command> generateRelatedReadCommand(State state) {
        if (this.params.size() != 4)
            return null;
        // You can only query with the primary key
        // First, get the primary keys, there must be primary keys for the
        // insertion

        CassandraState cassandraState = (CassandraState) state;
        String keyspaceName = this.params.get(0).toString();
        String tableName = this.params.get(1).toString();

        CassandraTable cassandraTable = cassandraState
                .getTable(keyspaceName, tableName);
        if (cassandraTable != null) {
            Set<Command> ret = new HashSet<>();

            // primaryNames
            List<Parameter> primaryColName2Type = cassandraTable.primaryColName2Type;
            List<String> primaryCols = new ArrayList<>();
            for (Parameter p : primaryColName2Type) {
                primaryCols.add(p.toString());
            }

            // columnsNames
            String[] columns = this.params.get(2).toString().split(",");
            List<String> columnsNames = new ArrayList<>();
            for (String column : columns) {
                columnsNames.add(column);
            }

            // insertValues
            List<Object> insertValues = (List<Object>) this.params.get(3)
                    .getValue();
            assert columnsNames.size() == insertValues.size();

            List<Object> primaryValues = new ArrayList<>();

            for (int i = 0; i < primaryCols.size(); i++) {
                // Index may not exist in the column???
                if (columnsNames.indexOf(primaryCols.get(i)) == -1) {
                    System.out.println(
                            "primaryCols not exist in the columns Names");
                    System.out.println("primaryCols[" + i + "]" + " = "
                            + primaryCols.get(i));
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("columnsNames = [");
                    for (String columnName : columnsNames) {
                        stringBuilder.append(columnName + " ");
                    }
                    stringBuilder.append("]");
                    System.out.println(stringBuilder);
                    System.out.println("column name parameter = "
                            + this.params.get(2).toString());

                    throw new RuntimeException();
                }
                primaryValues.add(((Parameter) insertValues
                        .get(columnsNames.indexOf(primaryCols.get(i))))
                                .getValue());
            }

            List<String> columns_SELECT = new ArrayList<>();
            // Randomly pick some, make it null here

            SELECT cmd = new SELECT(state, keyspaceName, tableName,
                    columns_SELECT, primaryCols, primaryValues);

            ret.add(cmd);
            return ret;
        }
        return null;
    }
}