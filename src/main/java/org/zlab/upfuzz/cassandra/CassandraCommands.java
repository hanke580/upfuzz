package org.zlab.upfuzz.cassandra;

import org.zlab.upfuzz.*;
import org.zlab.upfuzz.utils.PAIRType;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.utils.Pair;
import org.zlab.upfuzz.utils.STRINGType;

import java.util.*;

/**
 * TODO:
 *   1. nested commands & scope // we could do it in a simple way without scope first...
 *   2. mutate() & isValid() // we implemented generateRandomParameter() for each type
 *   3. user defined type // we need to implement a UnionType, each instance of a UnionType could be a user defined type
 *   4. mutate methods.
 *   - Shouldn't be operated by user.
 *   - When we call the command.mutate, (Conduct param level mutation)
 *   - It should pick one parameter defined in this command, and call its mutation method.
 *   - Then for the rest command, it should run check() method to do the minor modification or not.
 */
public class CassandraCommands {
    /**
     * CREATE (TABLE | COLUMNFAMILY) <tablename>
     * ('<column-definition>' , '<column-definition>')
     * (WITH <option> AND <option>)
     *
     * E.g.,
     *
     * CREATE TABLE emp(
     *    emp_id int PRIMARY KEY,
     *    emp_name text,
     *    emp_city text,
     *    emp_sal varint,
     *    emp_phone varint
     *    );
     */

    public static final List<Map.Entry<Class<? extends Command>, Integer>> commandClassList = new ArrayList<>();

//    public static final List<Class<? extends Command>> commandClassList = new ArrayList<>();
    // Prioritized commands, have a higher possibility to be generated in the first several
    // commands, but share the same possibility with the rest for the following commands.
public static final List<Map.Entry<Class<? extends Command>, Integer>> createCommandClassList = new ArrayList<>();


    static {
        commandClassList.add(new AbstractMap.SimpleImmutableEntry<>(CREATETABLE.class, 6));
        commandClassList.add(new AbstractMap.SimpleImmutableEntry<>(INSERT.class, 8));
        // commandClassList.add(new AbstractMap.SimpleImmutableEntry<>(ALTER_TABLE_DROP.class, 2));

        createCommandClassList.add(new AbstractMap.SimpleImmutableEntry<>(CREATETABLE.class, 2));
    }

    public static class CREATETABLE extends Command {

        // a parameter should correspond to one variable in the text format of this command.
        // mutating a parameter could depend on and the state updated by all nested internal commands and other parameters.
        // Note: Thus, we need to be careful to not have cyclic dependency among parameters.

        // final Command ...; // Nested commands need to be constructed first.

        public CREATETABLE(String tableName, List<Pair<String, String>> columns, List<String> PK, boolean ifNE){
            // TODO construct a command and replace its value!
            this(new CassandraState());
            // params.get(0).setValue((Object)tableName);
            // params.get(1).setValue((Object)columns);
        }

        public CREATETABLE(State state) {
            super();

            assert state instanceof CassandraState;
            CassandraState cassandraState = (CassandraState) state;

            ParameterType.ConcreteType tableNameType = new ParameterType.NotInCollectionType(
                new ParameterType.NotEmpty(
                        STRINGType.instance
                ),
                (s, c) -> {return ((CassandraState) s).tables.keySet(); },
                null
            );

            Parameter tableName = tableNameType.generateRandomParameter(cassandraState, this);
            params.add(tableName);

            ParameterType.ConcreteType columnsType = // LIST<PAIR<String,TYPEType>>
                new ParameterType.NotEmpty(
                    ParameterType.ConcreteGenericType.constructConcreteGenericType(
                        CassandraTypes.MapLikeListType.instance,
                        ParameterType.ConcreteGenericType.constructConcreteGenericType(PAIRType.instance,
                            new ParameterType.NotEmpty(
                                    STRINGType.instance
                            ),
                            CassandraTypes.TYPEType.instance))
            );

            Parameter columns = columnsType.generateRandomParameter(cassandraState, this);
            params.add(columns);
            /**
             * Bool variable check whether the previous columns has any member that's already specified as
             * Primary Key
             * - True
             *      - Shouldn't generate the third param
             * - False
             *      - Should generate
             *
             * Impl this check as a type
             * - Take a previous parameter as input
             *      - genRanParam()
             *            - whether generate() according to whether 'columns' have already 'Primary Key'
             *
             */
            ParameterType.ConcreteType primaryColumnsType =
                    new ParameterType.NotEmpty(
                            new ParameterType.SubsetType(
                                    columnsType,
                                    (s, c) -> (Collection<Parameter>) c.params.get(1).getValue(),
                                    null
                            )
                    );

            Parameter primaryColumns = primaryColumnsType.generateRandomParameter(cassandraState, this);
            params.add(primaryColumns);

            ParameterType.ConcreteType IF_NOT_EXISTType = new ParameterType.OptionalType(
                    new CONSTANTSTRINGType("IF NOT EXISTS"), null   // TODO: Make a pure CONSTANTType
            );
            Parameter IF_NOT_EXIST = IF_NOT_EXISTType.generateRandomParameter(cassandraState, this);
            params.add(IF_NOT_EXIST);

            updateExecutableCommandString();
        }

        @Override
        public String constructCommandString() {
            // TODO: Need a helper function, add space between all strings
            Parameter tableName = params.get(0);
            Parameter columns = params.get(1); // LIST<PAIR<TEXTType,TYPE>>
            Parameter primaryColumns = params.get(2);
            Parameter IF_NOT_EXIST = params.get(3);

            ParameterType.ConcreteType primaryColumnsNameType = new ParameterType.StreamMapType(
                    null,
                    (s, c) -> (Collection) c.params.get(2).getValue(),
                    p -> ((Pair<Parameter, Parameter>) ((Parameter) p).value).left
            );
            Parameter primaryColumnsName = primaryColumnsNameType.generateRandomParameter(null, this);

            String ret = "CREATE TABLE " + IF_NOT_EXIST.toString() + " " + tableName.toString() + "(" +
                    columns.toString() + ",\n PRIMARY KEY (" +
                    primaryColumnsName.toString() + " )" +
                    ");";

            return ret;
        }

        @Override
        public void updateState(State state) {
            Parameter tableName = params.get(0);
            Parameter columns = params.get(1); // LIST<PAIR<TEXTType,TYPE>>
            Parameter primaryColumns = params.get(2);

            CassandraTable table = new CassandraTable(tableName, columns, primaryColumns);
            ((CassandraState) state).addTable(tableName.toString(), table);
        }
    }

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

    public static class INSERT extends Command {

        public INSERT(State state) {
            super();

            assert state instanceof CassandraState;
            CassandraState cassandraState = (CassandraState) state;

            Parameter TableName = chooseOneTable(cassandraState, this);
            this.params.add(TableName);

            ParameterType.ConcreteType columnsType = new ParameterType.SuperSetType(
                    new ParameterType.SubsetType(
                            null,
                            (s, c) -> ((CassandraState) s).tables.get(c.params.get(0).toString()).colName2Type,
                            null
                    ),
                    (s, c) -> ((CassandraState) s).tables.get(c.params.get(0).toString()).primaryColName2Type,
                    null
            );
            Parameter columns = columnsType.generateRandomParameter(cassandraState, this);
            this.params.add(columns);

            ParameterType.ConcreteType insertValuesType = new ParameterType.Type2ValueType(
                    null,
                    (s, c) -> (Collection) c.params.get(1).getValue(),
                    p -> ((Pair) ((Parameter) p).value).right
            );
            Parameter insertValues = insertValuesType.generateRandomParameter(cassandraState, this);
            this.params.add(insertValues);

            updateExecutableCommandString();
        }

        @Override
        public String constructCommandString() {

            Parameter tableName = params.get(0);
            ParameterType.ConcreteType columnNameType = new ParameterType.StreamMapType(
                    null,
                    (s, c) -> (Collection) c.params.get(1).getValue(),
                    p -> ((Pair) ((Parameter) p).getValue()).left
            );
            Parameter columnName = columnNameType.generateRandomParameter(null, this);
            Parameter insertValues = params.get(2);

            StringBuilder sb = new StringBuilder();
            sb.append("INSERT INTO " + tableName.toString() + " (" +  columnName.toString() + ") VALUES (" + insertValues.toString()  + ");");
            return sb.toString();
        }

        @Override
        public void updateState(State state) { }
    }

    /**
     * ALTER TABLE [keyspace_name.] table_name
     * [DROP column_list];
     */
    public static class ALTER_TABLE_DROP extends Command {
        public ALTER_TABLE_DROP(State state) {
            super();

            assert state instanceof CassandraState;
            CassandraState cassandraState = (CassandraState) state;

            Parameter TableName = chooseOneTable(cassandraState, this);
            this.params.add(TableName);

            Predicate predicate = (s, c) -> {
                assert c instanceof ALTER_TABLE_DROP;
                CassandraTable cassandraTable = ((CassandraState) s).tables.get(c.params.get(0).toString());
                return cassandraTable.colName2Type.size() != cassandraTable.primaryColName2Type.size();
            };

            ParameterType.ConcreteType dropColumnType = new ParameterType.NotInCollectionType<>(
                    new ParameterType.InCollectionType(
                            null,
                            (s, c) -> ((CassandraState) s).tables.get(c.params.get(0).toString()).colName2Type,
//                            p -> ((Pair) ((Parameter) p).value).left
                            null,
                            predicate
                    ),
                    (s, c) -> ((CassandraState) s).tables.get(c.params.get(0).toString()).primaryColName2Type,
//                    p -> ((Pair) ((Parameter) p).value).left
                    null
            );
            Parameter dropColumn = dropColumnType.generateRandomParameter(cassandraState, this);
            this.params.add(dropColumn);

            updateExecutableCommandString();
        }

        @Override
        public String constructCommandString() {
            StringBuilder sb = new StringBuilder();
            sb.append("ALTER TABLE");
            sb.append(" " + this.params.get(0).toString() + " ");
            sb.append("DROP");
            sb.append(" " + ((Pair) this.params.get(1).getValue()).left.toString() + " ;");
            return sb.toString();
        }

        @Override
        public void updateState(State state) {
            ((CassandraState) state).tables.get(this.params.get(0).toString()).colName2Type.removeIf(value -> value.toString().equals(this.params.get(1).toString()));
        }
    }

    /**
     * DELETE firstname, lastname
     *   FROM cycling.cyclist_name
     *   USING TIMESTAMP 1318452291034
     *   WHERE lastname = 'VOS';
     */
    public static class DELETE extends Command {

        public DELETE(State state) {
            /**
             * Delete the whole column for now.
             */
            Parameter TableName = chooseOneTable(state, this);
            this.params.add(TableName); // Param0

            // Pick the subset of the primary columns, and make sure it's on the right order
            // First Several Type
            /**
             * Subset of primary columns
             */

            ParameterType.ConcreteType whereColumnsType =
                    new ParameterType.NotEmpty(
                            new ParameterType.SubsetType(
                                    null,
                                    (s, c) -> ((CassandraState) s).tables.get(c.params.get(0).toString()).primaryColName2Type,
                                    null
                            )
                    );
            Parameter whereColumns = whereColumnsType.generateRandomParameter(state, this);
            this.params.add(whereColumns); // Param1

            ParameterType.ConcreteType whereValuesType = new ParameterType.Type2ValueType(
                    null,
                    (s, c) -> (Collection) c.params.get(1).getValue(),
                    p -> ((Pair) ((Parameter) p).value).right
            );
            Parameter insertValues = whereValuesType.generateRandomParameter(state, this);
            this.params.add(insertValues); // Param2

            updateExecutableCommandString();
        }

        @Override
        public String constructCommandString() {
            StringBuilder sb = new StringBuilder();
            sb.append("DELETE" + " " + "FROM" + " ");
            sb.append(params.get(0).toString());
            sb.append(" " + "WHERE" + " ");


            ParameterType.ConcreteType whereColumnsType = new ParameterType.StreamMapType(
                    null,
                    (s, c) -> (Collection) c.params.get(1).getValue(),
                    p -> ((Pair<Parameter, Parameter>) ((Parameter) p).value).left
            );

            List<Parameter> whereColumns = (List<Parameter>) whereColumnsType.generateRandomParameter(null, this).getValue();

            List<Parameter> whereValues = (List<Parameter>) this.params.get(2).getValue();


            assert whereValues.size() == whereValues.size();

            for (int i = 0; i < whereColumns.size(); i++) {
                sb.append(whereColumns.get(i).toString() + " = " + whereValues.get(i).toString());
                if (i < whereColumns.size() - 1) {
                    sb.append(" AND ");
                }
            }
            sb.append(";");
            return sb.toString();
        }

        @Override
        public void updateState(State state) { }
    }


    /**
     * CREATE TYPE cycling.basic_info (
     *   birthday timestamp,
     *   nationality text,
     *   weight text,
     *   height text
     * );
     *
     * // UnionType {1,2,3,4}
     */

    public static Parameter chooseOneTable(State state, Command command) {
        /**
         * This helper function will randomly pick one table and return its
         * tablename as parameter.
         */
        ParameterType.ConcreteType TableNameType = new ParameterType.InCollectionType(
                CONSTANTSTRINGType.instance,
                (s, c) -> ((CassandraState) s).tables.keySet(),
                null
        );
        Parameter TableName = TableNameType.generateRandomParameter(state, command);
        return TableName;
    }

}
