package org.zlab.upfuzz.cassandra;

import org.zlab.upfuzz.*;
import org.zlab.upfuzz.ParameterType.FrontSubsetType;
import org.zlab.upfuzz.utils.*;

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
    public static final boolean DEBUG = false;

    public static final List<Map.Entry<Class<? extends Command>, Integer>> commandClassList = new ArrayList<>();

    /**
     * public static final List<Class<? extends Command>> commandClassList = new ArrayList<>();
     * Prioritized commands, have a higher possibility to be generated in the first several
     * commands, but share the same possibility with the rest for the following commands.
     */
    public static final List<Map.Entry<Class<? extends Command>, Integer>> createCommandClassList = new ArrayList<>();
    public static final List<Map.Entry<Class<? extends Command>, Integer>> readCommandClassList = new ArrayList<>();

    public static final String[] reservedKeywords = {
            "ADD","AGGREGATE","ALL","ALLOW","ALTER","AND",
            "ANY","APPLY","AS","ASC","ASCII","AUTHORIZE",
            "BATCH","BEGIN","BIGINT","BLOB","BOOLEAN","BY",
            "CLUSTERING","COLUMNFAMILY","COMPACT","CONSISTENCY","COUNT","COUNTER",
            "CREATE","CUSTOM","DECIMAL","DELETE","DESC","DISTINCT",
            "DOUBLE","DROP","EACH_QUORUM","ENTRIES","EXISTS","FILTERING",
            "FLOAT","FROM","FROZEN","FULL","GRANT","IF",
            "IN","INDEX","INET","INFINITY","INSERT","INT",
            "INTO","KEY","KEYSPACE","KEYSPACES","LEVEL","LIMIT",
            "LIST","LOCAL_ONE","LOCAL_QUORUM","MAP","MATERIALIZED","MODIFY",
            "NAN","NORECURSIVE","NOSUPERUSER","NOT","OF","ON",
            "ONE","ORDER","PARTITION","PASSWORD","PER","PERMISSION",
            "PERMISSIONS","PRIMARY","QUORUM","RENAME","REVOKE","SCHEMA",
            "SELECT","SET","STATIC","STORAGE","SUPERUSER","TABLE",
            "TEXT","TIME","TIMESTAMP","TIMEUUID","THREE","TO",
            "TOKEN","TRUNCATE","TTL","TUPLE","TWO","TYPE",
            "UNLOGGED","UPDATE","USE","USER","USERS","USING",
            "UUID","VALUES","VARCHAR","VARINT","VIEW","WHERE",
            "WITH","WRITETIM"
    };

    static {
        commandClassList.add(new AbstractMap.SimpleImmutableEntry<>(CREAT_KEYSPACE.class, 1));
        commandClassList.add(new AbstractMap.SimpleImmutableEntry<>(CREATE_TABLE.class, 1));
        commandClassList.add(new AbstractMap.SimpleImmutableEntry<>(INSERT.class, 10));
        commandClassList.add(new AbstractMap.SimpleImmutableEntry<>(DELETE.class, 6));
        // commandClassList.add(new AbstractMap.SimpleImmutableEntry<>(SELECT.class, 8));
        commandClassList.add(new AbstractMap.SimpleImmutableEntry<>(ALTER_TABLE_DROP.class, 8));
        commandClassList.add(new AbstractMap.SimpleImmutableEntry<>(CREAT_INDEX.class, 4));

        createCommandClassList.add(new AbstractMap.SimpleImmutableEntry<>(CREAT_KEYSPACE.class, 2));
        createCommandClassList.add(new AbstractMap.SimpleImmutableEntry<>(CREATE_TABLE.class, 3));

        readCommandClassList.add(new AbstractMap.SimpleImmutableEntry<>(SELECT.class, 10));
    }

    /**
     * CREATE KEYSPACE [IF NOT EXISTS] keyspace_name
     *    WITH REPLICATION = {
     *       'class' : 'SimpleStrategy', 'replication_factor' : N }
     *      | 'class' : 'NetworkTopologyStrategy',
     *        'dc1_name' : N [, ...]
     *    }
     *    [AND DURABLE_WRITES =  true|false] ;
     */
    public static class CREAT_KEYSPACE extends Command {

        public CREAT_KEYSPACE(State state, Object init0, Object init1, Object init2) {
            super();

            ParameterType.ConcreteType keyspaceNameType = new ParameterType.NotInCollectionType(
                    new ParameterType.NotEmpty(
                            STRINGType.instance
                    ),
                    (s, c) -> ((CassandraState) s).getKeyspaces(),
                    null
            );
            Parameter keyspaceName = keyspaceNameType.generateRandomParameter(state, this, init0);
            this.params.add(keyspaceName); // [0]

            ParameterType.ConcreteType replicationFactorType = new INTType(1, 4);
            Parameter replicationFactor = replicationFactorType.generateRandomParameter(state, this, init1);
            this.params.add(replicationFactor); // [1]

            ParameterType.ConcreteType IF_NOT_EXISTType = new ParameterType.OptionalType(
                    new CONSTANTSTRINGType("IF NOT EXISTS"), null   // TODO: Make a pure CONSTANTType
            );
            Parameter IF_NOT_EXIST = IF_NOT_EXISTType.generateRandomParameter(state, this, init2);
            params.add(IF_NOT_EXIST); // [2]

            updateExecutableCommandString();

        }

        public CREAT_KEYSPACE(State state) {
            super();

            ParameterType.ConcreteType keyspaceNameType = new ParameterType.NotInCollectionType(
                    new ParameterType.NotEmpty(
                            STRINGType.instance
                    ),
                    (s, c) -> ((CassandraState) s).getKeyspaces(),
                    null
            );
            Parameter keyspaceName = keyspaceNameType.generateRandomParameter(state, this);
            this.params.add(keyspaceName); // [0]

            ParameterType.ConcreteType replicationFactorType = new INTType(1, 4);
            Parameter replicationFactor = replicationFactorType.generateRandomParameter(state, this);
            this.params.add(replicationFactor); // [1]

            ParameterType.ConcreteType IF_NOT_EXISTType = new ParameterType.OptionalType(
                    new CONSTANTSTRINGType("IF NOT EXISTS"), null
                    // TODO: Make a pure CONSTANTType
            );
            Parameter IF_NOT_EXIST = IF_NOT_EXISTType.generateRandomParameter(state, this);
            params.add(IF_NOT_EXIST); // [2]

            updateExecutableCommandString();
        }

        @Override
        public String constructCommandString() {
            StringBuilder sb = new StringBuilder();
            sb.append("CREATE KEYSPACE" + " " + this.params.get(2).toString() + " " + this.params.get(0).toString() + " ");
            sb.append("WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' :" + " ");
            sb.append(this.params.get(1).toString() + " " + "};");
            return sb.toString();
        }

        @Override
        public void updateState(State state) {
            ((CassandraState) state).addKeyspace(this.params.get(0).toString());
        }

        public void changeKeyspaceName() {
            this.params.get(0).regenerate(null, this);
        }
    }

    public static class CREATE_TABLE extends Command {
        /**
         * a parameter should correspond to one variable in the text format of this command.
         * mutating a parameter could depend on and the state updated by all nested internal commands and other parameters.
         * Note: Thus, we need to be careful to not have cyclic dependency among parameters.
         */

        // final Command ...; // Nested commands need to be constructed first.

        public CREATE_TABLE(State state, Object init0, Object init1, Object init2, Object init3, Object init4) {
            super();

            assert state instanceof CassandraState;
            CassandraState cassandraState = (CassandraState) state;

            Parameter keyspaceName = chooseKeyspace(state, this, init0);
            params.add(keyspaceName); // [0]

            ParameterType.ConcreteType tableNameType = new ParameterType.NotInCollectionType(
                    new ParameterType.NotEmpty(
                            STRINGType.instance
                    ),
                    (s, c) -> ((CassandraState) s).keyspace2tables.get(this.params.get(0).toString()).keySet(),
                    null
            );

            Parameter tableName = tableNameType.generateRandomParameter(cassandraState, this, init1);
            params.add(tableName); // [1]

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

            Parameter columns = columnsType.generateRandomParameter(cassandraState, this, init2);
            params.add(columns); // [2]

            ParameterType.ConcreteType primaryColumnsType =
                    new ParameterType.NotEmpty(
                            new ParameterType.SubsetType(
                                    columnsType,
                                    (s, c) -> (Collection<Parameter>) c.params.get(2).getValue(),
                                    null
                            )
                    );

            Parameter primaryColumns = primaryColumnsType.generateRandomParameter(cassandraState, this, init3);
            params.add(primaryColumns); // [3]

            ParameterType.ConcreteType IF_NOT_EXISTType = new ParameterType.OptionalType(
                    new CONSTANTSTRINGType("IF NOT EXISTS"), null   // TODO: Make a pure CONSTANTType
            );
            Parameter IF_NOT_EXIST = IF_NOT_EXISTType.generateRandomParameter(cassandraState, this, init4);
            params.add(IF_NOT_EXIST); // [4]

            updateExecutableCommandString();
        }

        public CREATE_TABLE(State state) {
            super();

            assert state instanceof CassandraState;
            CassandraState cassandraState = (CassandraState) state;

            Parameter keyspaceName = chooseKeyspace(state, this, null);
            params.add(keyspaceName); // [0]

            ParameterType.ConcreteType tableNameType = new ParameterType.NotInCollectionType(
                new ParameterType.NotEmpty(
                        STRINGType.instance
                ),
                (s, c) -> ((CassandraState) s).keyspace2tables.get(this.params.get(0).toString()).keySet(),
                null
            );

            Parameter tableName = tableNameType.generateRandomParameter(cassandraState, this);
            params.add(tableName); // [1]

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
            params.add(columns); // [2]

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
                                    (s, c) -> (Collection<Parameter>) c.params.get(2).getValue(),
                                    null
                            )
                    );

            Parameter primaryColumns = primaryColumnsType.generateRandomParameter(cassandraState, this);
            params.add(primaryColumns); // [3]

            ParameterType.ConcreteType IF_NOT_EXISTType = new ParameterType.OptionalType(
                    new CONSTANTSTRINGType("IF NOT EXISTS"), null   // TODO: Make a pure CONSTANTType
            );
            Parameter IF_NOT_EXIST = IF_NOT_EXISTType.generateRandomParameter(cassandraState, this);
            params.add(IF_NOT_EXIST); // [4]

            updateExecutableCommandString();
        }

        @Override
        public String constructCommandString() {
            // TODO: Need a helper function, add space between all strings
            Parameter keyspaceName = params.get(0);
            Parameter tableName = params.get(1);
            Parameter columns = params.get(2); // LIST<PAIR<TEXTType,TYPE>>
            Parameter primaryColumns = params.get(3);
            Parameter IF_NOT_EXIST = params.get(4);

            ParameterType.ConcreteType primaryColumnsNameType = new ParameterType.StreamMapType(
                    null,
                    (s, c) -> (Collection) c.params.get(3).getValue(),
                    p -> ((Pair<Parameter, Parameter>) ((Parameter) p).value).left
            );
            Parameter primaryColumnsName = primaryColumnsNameType.generateRandomParameter(null, this);

            String ret = "CREATE TABLE " + IF_NOT_EXIST.toString() + " " +
                    keyspaceName.toString() + "." +
                    tableName.toString() + " (" +
                    columns.toString() + ",\n PRIMARY KEY (" +
                    primaryColumnsName.toString() + " )" +
                    ");";

            return ret;
        }

        @Override
        public void updateState(State state) {
            Parameter keyspaceName = params.get(0);
            Parameter tableName = params.get(1);
            Parameter columns = params.get(2); // LIST<PAIR<TEXTType,TYPE>>
            Parameter primaryColumns = params.get(3);

            CassandraTable table = new CassandraTable(tableName, columns, primaryColumns);
            ((CassandraState) state).addTable(keyspaceName.toString(), tableName.toString(), table);
        }
    }

    public static class CREAT_INDEX extends Command {

        public CREAT_INDEX(State state) {
            super();

            assert state instanceof CassandraState;
            CassandraState cassandraState = (CassandraState) state;

            Parameter keyspaceName = chooseKeyspace(cassandraState, this, null);
            this.params.add(keyspaceName); // P0

            Parameter TableName = chooseTable(cassandraState, this, null);
            this.params.add(TableName);    // P1

            ParameterType.ConcreteType indexNameType = new ParameterType.NotInCollectionType(
                    new ParameterType.NotEmpty(
                            STRINGType.instance
                    ),
                    (s, c) -> ((CassandraState) s).getTable(c.params.get(0).toString(), c.params.get(1).toString()).indexes,
                    null
            );
            Parameter indexName = indexNameType.generateRandomParameter(state, this);
            this.params.add(indexName);  // P2

            ParameterType.ConcreteType indexColumnType =
                    new ParameterType.InCollectionType(
                            null,
                            (s, c) -> ((CassandraState) s).getTable(c.params.get(0).toString(), c.params.get(1).toString()).colName2Type,
                            null,
                            null
                    );
            Parameter indexColumn = indexColumnType.generateRandomParameter(cassandraState, this);
            this.params.add(indexColumn); // P3

            ParameterType.ConcreteType IF_NOT_EXISTType = new ParameterType.OptionalType(
                    new CONSTANTSTRINGType("IF NOT EXISTS"), null   // TODO: Make a pure CONSTANTType
            );
            Parameter IF_NOT_EXIST = IF_NOT_EXISTType.generateRandomParameter(state, this);
            params.add(IF_NOT_EXIST); // P4

            updateExecutableCommandString();
        }

        @Override
        public String constructCommandString() {
            StringBuilder sb = new StringBuilder();
            sb.append("CREATE INDEX");
            sb.append(" " + this.params.get(4) + " " + this.params.get(2) + " ON");
            sb.append(" " + this.params.get(0) + "." + this.params.get(1).toString() + " ");
            sb.append("( " + ((Pair) this.params.get(3).getValue()).left.toString() + ");");
            return sb.toString();
        }

        @Override
        public void updateState(State state) {
            ((CassandraState) state)
                    .getTable(this.params.get(0).toString(), this.params.get(1).toString())
                    .indexes
                    .add(this.params.get(2).toString());
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
    public static class CREATE_TYPE extends Command {

        public CREATE_TYPE(State state) {
            super();

            assert state instanceof CassandraState;
            CassandraState cassandraState = (CassandraState) state;

            Parameter keyspaceName = chooseKeyspace(cassandraState, this, null);
            this.params.add(keyspaceName);  // 0

            ParameterType.ConcreteType typeNameType = new ParameterType.NotInCollectionType(
                    new ParameterType.NotEmpty(
                            STRINGType.instance
                    ),
                    (s, c) -> ((CassandraState) s).keyspace2UDTs.get(c.params.get(0).toString()),
                    null
            );
            Parameter typeName = typeNameType.generateRandomParameter(cassandraState, this);
            params.add(typeName);           // 1

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
            params.add(columns); // 2

            updateExecutableCommandString();
        }

        @Override
        public String constructCommandString() {

            Parameter keyspaceName = params.get(0);
            Parameter typeName = params.get(1);
            Parameter columns = params.get(2);

            StringBuilder sb = new StringBuilder();
            sb.append("CREATE TYPE " + keyspaceName.toString() + "." + typeName + " (" +  columns.toString() + ");");
            return sb.toString();
        }

        @Override
        public void updateState(State state) {
            ((CassandraState) state).keyspace2UDTs.get(params.get(0).toString()).add(params.get(1).toString());
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

        public INSERT(State state, Object init0, Object init1, Object init2, Object init3) {
            super();

            assert state instanceof CassandraState;
            CassandraState cassandraState = (CassandraState) state;

            Parameter keyspaceName = chooseKeyspace(cassandraState, this, init0);
            this.params.add(keyspaceName); // [0]

            Parameter TableName = chooseTable(cassandraState, this, init1);
            this.params.add(TableName); // [1]

            ParameterType.ConcreteType columnsType = new ParameterType.SuperSetType(
                    new ParameterType.SubsetType(
                            null,
                            (s, c) -> ((CassandraState) s).getTable(
                                    c.params.get(0).toString(),
                                    c.params.get(1).toString()
                            ).colName2Type,
                            null
                    ),
                    (s, c) -> ((CassandraState) s).getTable(
                            c.params.get(0).toString(),
                            c.params.get(1).toString()
                    ).primaryColName2Type,
                    null
            );
            Parameter columns = columnsType.generateRandomParameter(cassandraState, this, init2);
            this.params.add(columns); // [2]

            ParameterType.ConcreteType insertValuesType = new ParameterType.Type2ValueType(
                    null,
                    (s, c) -> (Collection) c.params.get(2).getValue(), // columns
                    p -> ((Pair) ((Parameter) p).value).right
            );
            Parameter insertValues = insertValuesType.generateRandomParameter(cassandraState, this, init3);
            this.params.add(insertValues); // [3]

            updateExecutableCommandString();
        }

        public INSERT(State state) {
            super();

            assert state instanceof CassandraState;
            CassandraState cassandraState = (CassandraState) state;

            Parameter keyspaceName = chooseKeyspace(cassandraState, this, null);
            this.params.add(keyspaceName);

            Parameter TableName = chooseTable(cassandraState, this, null);
            this.params.add(TableName);

            ParameterType.ConcreteType columnsType = new ParameterType.SuperSetType(
                    new ParameterType.SubsetType(
                            null,
                            (s, c) -> ((CassandraState) s).getTable(
                                    c.params.get(0).toString(),
                                    c.params.get(1).toString()
                            ).colName2Type,
                            null
                    ),
                    (s, c) -> ((CassandraState) s).getTable(
                            c.params.get(0).toString(),
                            c.params.get(1).toString()
                    ).primaryColName2Type,
                    null
            );
            Parameter columns = columnsType.generateRandomParameter(cassandraState, this);
            this.params.add(columns);

            ParameterType.ConcreteType insertValuesType = new ParameterType.Type2ValueType(
                    null,
                    (s, c) -> (Collection) c.params.get(2).getValue(), // columns
                    p -> ((Pair) ((Parameter) p).value).right
            );
            Parameter insertValues = insertValuesType.generateRandomParameter(cassandraState, this);
            this.params.add(insertValues);

            updateExecutableCommandString();
        }

        @Override
        public String constructCommandString() {

            Parameter keyspaceName = params.get(0);
            Parameter tableName = params.get(1);
            ParameterType.ConcreteType columnNameType = new ParameterType.StreamMapType(
                    null,
                    (s, c) -> (Collection) c.params.get(2).getValue(),
                    p -> ((Pair) ((Parameter) p).getValue()).left
            );
            Parameter columnName = columnNameType.generateRandomParameter(null, this);
            Parameter insertValues = params.get(3);

            StringBuilder sb = new StringBuilder();
            sb.append("INSERT INTO " + keyspaceName.toString() + "." + tableName.toString() + " (" +  columnName.toString() + ") VALUES (" + insertValues.toString()  + ");");
            return sb.toString();
        }

        @Override
        public void updateState(State state) { }

        @Override
        public Set<Command> generateRelatedReadCommand(State state) {
            if (this.params.size() != 4) return null;
            // You can only query with the primary key
            // First, get the primary keys, there must be primary keys for the insertion

            CassandraState cassandraState = (CassandraState) state;
            String keyspaceName = this.params.get(0).toString();
            String tableName = this.params.get(1).toString();

            CassandraTable cassandraTable = cassandraState.getTable(keyspaceName, tableName);
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
                List<Object> insertValues = (List<Object>) this.params.get(3).getValue();
                assert columnsNames.size() == insertValues.size();

                List<Object> primaryValues = new ArrayList<>();

                for (int i = 0; i < primaryCols.size(); i++) {
                    // Index may not be exist in the column???
                    if (columnsNames.indexOf(primaryCols.get(i)) == -1) {
                        System.out.println("primaryCols not exist in the columns Names");
                        System.out.println("primaryCols[" + i + "]" + " = " + primaryCols.get(i));
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("columnsNames = [");
                        for (String columnName : columnsNames) {
                            stringBuilder.append(columnName + " ");
                        }
                        stringBuilder.append("]");
                        System.out.println(stringBuilder);
                        throw new RuntimeException();
                    }
                    primaryValues.add( ((Parameter) insertValues.get(columnsNames.indexOf(primaryCols.get(i)))).getValue() );
                }

                List<String> columns_SELECT = new ArrayList<>();
                // Randomly pick some, make it null here

                SELECT cmd = new SELECT(state, keyspaceName, tableName, columns_SELECT, primaryCols, primaryValues);

                ret.add(cmd);
                return ret;
            }
            return null;
        }
    }

    /**
     * ALTER TABLE [keyspace_name.] table_name
     * [DROP column_list];
     */
    public static class ALTER_TABLE_DROP extends Command {
        public ALTER_TABLE_DROP(State state, Object init0, Object init1, Object init2) {
            super();

            assert state instanceof CassandraState;
            CassandraState cassandraState = (CassandraState) state;

            Parameter keyspaceName = chooseKeyspace(cassandraState, this, init0);
            this.params.add(keyspaceName);

            Parameter TableName = chooseTable(cassandraState, this, init1);
            this.params.add(TableName);

            Predicate predicate = (s, c) -> {
                assert c instanceof ALTER_TABLE_DROP;
                CassandraTable cassandraTable = ((CassandraState) s).getTable(c.params.get(0).toString(), c.params.get(1).toString());
                return cassandraTable.colName2Type.size() != cassandraTable.primaryColName2Type.size();
            };

            ParameterType.ConcreteType dropColumnType = new ParameterType.NotInCollectionType(
                    new ParameterType.InCollectionType(
                            null,
                            (s, c) -> ((CassandraState) s).getTable(c.params.get(0).toString(), c.params.get(1).toString()).colName2Type,
//                            p -> ((Pair) ((Parameter) p).value).left
                            null,
                            predicate
                    ),
                    (s, c) -> ((CassandraState) s).getTable(c.params.get(0).toString(), c.params.get(1).toString()).primaryColName2Type,
//                    p -> ((Pair) ((Parameter) p).value).left
                    null
            );
            Parameter dropColumn = dropColumnType.generateRandomParameter(cassandraState, this, init2);
            this.params.add(dropColumn);

            updateExecutableCommandString();
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
                CassandraTable cassandraTable = ((CassandraState) s).getTable(c.params.get(0).toString(), c.params.get(1).toString());
                return cassandraTable.colName2Type.size() != cassandraTable.primaryColName2Type.size();
            };
            /**
             * FIXME: About the Predicate. Two ways
             * Keep the retry times, if it retrys for many times, throw a warning about the constraints?
             * Retry a few times, if not success, it fails.
             */

            ParameterType.ConcreteType dropColumnType = new ParameterType.NotInCollectionType(
                    new ParameterType.InCollectionType(
                            null,
                            (s, c) -> ((CassandraState) s).getTable(c.params.get(0).toString(), c.params.get(1).toString()).colName2Type,
                            null,
                            predicate
                    ),
                    (s, c) -> ((CassandraState) s).getTable(c.params.get(0).toString(), c.params.get(1).toString()).primaryColName2Type,
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
            sb.append(" " + this.params.get(0) + "." + this.params.get(1).toString() + " ");
            sb.append("DROP");
            sb.append(" " + ((Pair) this.params.get(2).getValue()).left.toString() + " ;");
            return sb.toString();
        }

        @Override
        public void updateState(State state) {
            ((CassandraState) state).getTable(this.params.get(0).toString(), this.params.get(1).toString()) // Get the table to modify
                    .colName2Type.removeIf(value -> value.toString().equals(this.params.get(2).toString()));
        }
    }

    /**
     * ALTER TABLE [keyspace_name.] table_name
     * [DROP column_list];
     */
    public static class ALTER_TABLE_ADD extends Command {

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
                    new ParameterType.NotEmpty(
                            STRINGType.instance
                    ),
                    (s, c) -> ((CassandraState) s).getTable(c.params.get(0).toString(), c.params.get(1).toString()).colName2Type,
                    p -> ((Pair) ((Parameter) p).getValue()).left
            );
            Parameter addColumnName = addColumnNameType.generateRandomParameter(cassandraState, this);
            this.params.add(addColumnName);

            ParameterType.ConcreteType addColumnTypeType = CassandraTypes.TYPEType.instance;
            Parameter addColumnType = addColumnTypeType.generateRandomParameter(cassandraState, this);
            this.params.add(addColumnType);

            updateExecutableCommandString();
        }

        @Override
        public String constructCommandString() {
            StringBuilder sb = new StringBuilder();
            sb.append("ALTER TABLE");
            sb.append(" " + this.params.get(0) + "." + this.params.get(1).toString() + " ");
            sb.append("ADD");
            sb.append(" " + this.params.get(2).toString() + " " + this.params.get(3).toString() + " ;");
            return sb.toString();
        }

        @Override
        public void updateState(State state) {

            ParameterType.ConcreteType columnType =
                    ParameterType.ConcreteGenericType.constructConcreteGenericType(
                            PAIRType.instance,
                            new ParameterType.NotEmpty(
                                    STRINGType.instance
                            ),
                            CassandraTypes.TYPEType.instance);

            Parameter p = new Parameter(columnType, new Pair<>(params.get(2), params.get(3)));
            ((CassandraState) state).getTable(this.params.get(0).toString(), this.params.get(1).toString()) // Get the table to modify
                    .colName2Type.add(p);
        }
    }

    /**
     * ALTER  KEYSPACE keyspace_name
     *    WITH REPLICATION = {
     *       'class' : 'SimpleStrategy', 'replication_factor' : N
     *      | 'class' : 'NetworkTopologyStrategy', 'dc1_name' : N [, ...]
     *    }
     *    [AND DURABLE_WRITES =  true|false] ;
     */
    public static class ALTER_KEYSPACE extends Command {
        /**
         * a parameter should correspond to one variable in the text format of this command.
         * mutating a parameter could depend on and the state updated by all nested internal commands and other parameters.
         * Note: Thus, we need to be careful to not have cyclic dependency among parameters.
         */

        // final Command ...; // Nested commands need to be constructed first.

        public ALTER_KEYSPACE(State state) {
            super();

            assert state instanceof CassandraState;
            CassandraState cassandraState = (CassandraState) state;

            Parameter keyspaceName = chooseKeyspace(state, this, null);
            params.add(keyspaceName); // [0]


            ParameterType.ConcreteType replicationFactorType = new INTType(1, 4);
            Parameter replicationFactor = replicationFactorType.generateRandomParameter(state, this);
            this.params.add(replicationFactor); // [1]

            updateExecutableCommandString();
        }

        @Override
        public String constructCommandString() {
            StringBuilder sb = new StringBuilder();
            sb.append("ALTER KEYSPACE" + " " + this.params.get(0).toString() + " ");
            sb.append("WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' :" + " ");
            sb.append(this.params.get(1).toString() + " " + "};");

            return sb.toString();
        }

        @Override
        public void updateState(State state) { }
    }

    public static class DROP_INDEX extends Command {
        public DROP_INDEX(State state) {
            super();

            assert state instanceof CassandraState;
            CassandraState cassandraState = (CassandraState) state;

            Parameter keyspaceName = chooseKeyspace(cassandraState, this, null);
            this.params.add(keyspaceName); // 0

            Parameter TableName = chooseTable(cassandraState, this, null);
            this.params.add(TableName);    // 1

            ParameterType.ConcreteType indexNameType = new ParameterType.InCollectionType(
                    CONSTANTSTRINGType.instance,
                    (s, c) -> ((CassandraState) s).getTable(c.params.get(0).toString(), c.params.get(1).toString()).indexes,
                    null
            );
            Parameter indexName = indexNameType.generateRandomParameter(state, this);
            this.params.add(indexName); // 2

            ParameterType.ConcreteType IF_EXISTType = new ParameterType.OptionalType(
                    new CONSTANTSTRINGType("IF EXISTS"), null   // TODO: Make a pure CONSTANTType
            );
            Parameter IF_EXIST = IF_EXISTType.generateRandomParameter(cassandraState, this);
            params.add(IF_EXIST); // 3

            updateExecutableCommandString();
        }

        @Override
        public String constructCommandString() {
            StringBuilder sb = new StringBuilder();
            sb.append("DROP INDEX ").append(params.get(3)).append(" " + this.params.get(0) + "." + this.params.get(2).toString() + ";");
            return sb.toString();
        }

        @Override
        public void updateState(State state) {
            ((CassandraState) state)
                    .getTable(params.get(0).toString(), params.get(1).toString())
                    .indexes
                    .remove(params.get(2).toString());
        }
    }

    public static class DROP_KEYSPACE extends Command {
        public DROP_KEYSPACE(State state) {
            super();

            assert state instanceof CassandraState;
            CassandraState cassandraState = (CassandraState) state;

            Parameter keyspaceName = chooseKeyspace(cassandraState, this, null);
            this.params.add(keyspaceName); // 0

            ParameterType.ConcreteType IF_EXISTType = new ParameterType.OptionalType(
                    new CONSTANTSTRINGType("IF EXISTS"), null   // TODO: Make a pure CONSTANTType
            );
            Parameter IF_EXIST = IF_EXISTType.generateRandomParameter(cassandraState, this);
            params.add(IF_EXIST); // 1

            updateExecutableCommandString();
        }

        @Override
        public String constructCommandString() {
            StringBuilder sb = new StringBuilder();
            sb.append("DROP KEYSPACE " + params.get(1));
            sb.append(" " + this.params.get(0) + ";");
            return sb.toString();
        }

        @Override
        public void updateState(State state) {
            ((CassandraState) state)
                    .keyspace2tables.remove(params.get(0).toString());
            ((CassandraState) state)
                    .keyspace2UDTs.remove(params.get(0).toString());
        }
    }

    public static class DROP_TABLE extends Command {
        public DROP_TABLE(State state) {
            super();

            assert state instanceof CassandraState;
            CassandraState cassandraState = (CassandraState) state;

            Parameter keyspaceName = chooseKeyspace(cassandraState, this, null);
            this.params.add(keyspaceName); // 0

            Parameter TableName = chooseTable(cassandraState, this, null);
            this.params.add(TableName);    // 1

            ParameterType.ConcreteType IF_EXISTType = new ParameterType.OptionalType(
                    new CONSTANTSTRINGType("IF EXISTS"), null   // TODO: Make a pure CONSTANTType
            );
            Parameter IF_EXIST = IF_EXISTType.generateRandomParameter(cassandraState, this);
            params.add(IF_EXIST); // 2

            updateExecutableCommandString();
        }

        @Override
        public String constructCommandString() {
            StringBuilder sb = new StringBuilder();
            sb.append("DROP TABLE " + params.get(2));
            sb.append(" " + this.params.get(0) + "." + this.params.get(1).toString() + ";");
            return sb.toString();
        }

        @Override
        public void updateState(State state) {
            ((CassandraState) state)
                    .keyspace2tables.get(params.get(0).toString())
                    .remove(params.get(1).toString());
        }
    }

    public static class DROP_TYPE extends Command {
        public DROP_TYPE(State state) {
            super();

            assert state instanceof CassandraState;
            CassandraState cassandraState = (CassandraState) state;

            Parameter keyspaceName = chooseKeyspace(cassandraState, this, null);
            this.params.add(keyspaceName); // 0

            ParameterType.ConcreteType typeNameType = new ParameterType.InCollectionType(
                    STRINGType.instance,
                    (s, c) -> ((CassandraState) s).keyspace2UDTs.get(this.params.get(0).toString()),
                    null
            );

            Parameter typeName = typeNameType.generateRandomParameter(cassandraState, this);
            params.add(typeName); // 1


            ParameterType.ConcreteType IF_EXISTType = new ParameterType.OptionalType(
                    new CONSTANTSTRINGType("IF EXISTS"), null   // TODO: Make a pure CONSTANTType
            );
            Parameter IF_EXIST = IF_EXISTType.generateRandomParameter(cassandraState, this);
            params.add(IF_EXIST); // 2

            updateExecutableCommandString();
        }

        @Override
        public String constructCommandString() {
            StringBuilder sb = new StringBuilder();
            sb.append("DROP TYPE " + params.get(2));
//            sb.append(" " + this.params.get(0).toString() + "." + ";");
            sb.append(" " + this.params.get(0).toString() + "." + this.params.get(1).toString() + ";");
            return sb.toString();
        }

        @Override
        public void updateState(State state) {
            ((CassandraState) state).keyspace2UDTs.get(this.params.get(0).toString()).remove(this.params.get(1).toString());
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
            Parameter keyspaceName = chooseKeyspace(state, this, null);
            this.params.add(keyspaceName); // Param0

            Parameter TableName = chooseTable(state, this, null);
            this.params.add(TableName); // Param1

            // Pick the subset of the primary columns, and make sure it's on the right order
            // First Several Type
            /**
             * Subset of primary columns
             */

            ParameterType.ConcreteType whereColumnsType =
                    new ParameterType.NotEmpty(
                            new FrontSubsetType(
                                    null,
                                    (s, c) -> ((CassandraState) s)
                                            .getTable(
                                                    c.params.get(0).toString(),
                                                    c.params.get(1).toString())
                                            .primaryColName2Type,
                                    null
                            )
                    );
            Parameter whereColumns = whereColumnsType.generateRandomParameter(state, this);
            this.params.add(whereColumns); // Param2

            ParameterType.ConcreteType whereValuesType = new ParameterType.Type2ValueType(
                    null,
                    (s, c) -> (Collection) c.params.get(2).getValue(),
                    p -> ((Pair) ((Parameter) p).value).right
            );
            Parameter insertValues = whereValuesType.generateRandomParameter(state, this);
            this.params.add(insertValues); // Param3

            updateExecutableCommandString();
        }

        @Override
        public String constructCommandString() {
            StringBuilder sb = new StringBuilder();
            sb.append("DELETE" + " " + "FROM" + " ");
            sb.append(params.get(0) + "." + params.get(1).toString());
            sb.append(" " + "WHERE" + " ");


            ParameterType.ConcreteType whereColumnsType = new ParameterType.StreamMapType(
                    null,
                    (s, c) -> (Collection) c.params.get(2).getValue(),
                    p -> ((Pair<Parameter, Parameter>) ((Parameter) p).value).left
            );

            List<Parameter> whereColumns = (List<Parameter>) whereColumnsType.generateRandomParameter(null, this).getValue();
            List<Parameter> whereValues = (List<Parameter>) this.params.get(3).getValue();

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
     * SELECT * | select_expression | DISTINCT partition
     * FROM [keyspace_name.] table_name
     * [WHERE partition_value
     *    [AND clustering_filters
     *    [AND static_filters]]]
     * [ORDER BY PK_column_name ASC|DESC]
     * [LIMIT N]
     * [ALLOW FILTERING]
     */
    public static class SELECT extends Command {

        public SELECT (State state, Object init0, Object init1, Object init2, Object init3, Object init4) {
            super();

            assert state instanceof CassandraState;
            CassandraState cassandraState = (CassandraState) state;

            Parameter keyspaceName = chooseKeyspace(cassandraState, this, init0);
            this.params.add(keyspaceName); // [0]

            Parameter TableName = chooseTable(cassandraState, this, init1);
            this.params.add(TableName); // [1]

            ParameterType.ConcreteType selectColumnsType =
                    new ParameterType.SubsetType<>(
                            null,
                            (s, c) -> ((CassandraState) s)
                                    .getTable(
                                            c.params.get(0).toString(),
                                            c.params.get(1).toString())
                                    .colName2Type,
                            p -> ((Pair<Parameter, Parameter>) (((Parameter) p).getValue())).left
                    );
            Parameter selectColumns = selectColumnsType.generateRandomParameter(state, this, init2);
            this.params.add(selectColumns); // Param2

            ParameterType.ConcreteType whereColumnsType =
                    new FrontSubsetType(
                            null,
                            (s, c) -> ((CassandraState) s)
                                    .getTable(
                                            c.params.get(0).toString(),
                                            c.params.get(1).toString())
                                    .primaryColName2Type,
                            null
                    );
            Parameter whereColumns = whereColumnsType.generateRandomParameter(state, this, init3);
            this.params.add(whereColumns); // Param 3

            ParameterType.ConcreteType whereValuesType = new ParameterType.Type2ValueType(
                    null,
                    (s, c) -> (Collection) c.params.get(3).getValue(),
                    p -> ((Pair) ((Parameter) p).value).right
            );
            Parameter insertValues = whereValuesType.generateRandomParameter(state, this, init4);
            this.params.add(insertValues); // Param4

            updateExecutableCommandString();
        }

        public SELECT (State state) {

            Parameter keyspaceName = chooseKeyspace(state, this, null);
            this.params.add(keyspaceName); // Param0

            Parameter TableName = chooseTable(state, this, null);
            this.params.add(TableName); // Param1

            // Pick the subset of the primary columns, and make sure it's on the right order
            // First Several Type
            /**
             * Subset of primary columns
             */
            ParameterType.ConcreteType selectColumnsType =
                    new ParameterType.SubsetType<>(
                            null,
                            (s, c) -> ((CassandraState) s)
                                    .getTable(
                                            c.params.get(0).toString(),
                                            c.params.get(1).toString())
                                    .colName2Type,
                            p -> ((Pair<Parameter, Parameter>) (((Parameter) p).getValue())).left
                    );
            Parameter selectColumns = selectColumnsType.generateRandomParameter(state, this);
            this.params.add(selectColumns); // Param2

            ParameterType.ConcreteType whereColumnsType =
                        new FrontSubsetType(
                                null,
                                (s, c) -> ((CassandraState) s)
                                        .getTable(
                                                c.params.get(0).toString(),
                                                c.params.get(1).toString())
                                        .primaryColName2Type,
                                null
                        );
            Parameter whereColumns = whereColumnsType.generateRandomParameter(state, this);
            this.params.add(whereColumns); // Param 3

            ParameterType.ConcreteType whereValuesType = new ParameterType.Type2ValueType(
                    null,
                    (s, c) -> (Collection) c.params.get(3).getValue(),
                    p -> ((Pair) ((Parameter) p).value).right
            );
            Parameter insertValues = whereValuesType.generateRandomParameter(state, this);
            this.params.add(insertValues); // Param4

            updateExecutableCommandString();
        }

        @Override
        public String constructCommandString() {
            StringBuilder sb = new StringBuilder();
            sb.append("SELECT ");
            if (params.get(2).isEmpty(null, this)) {
                sb.append("* ");
            } else {
                List<Parameter> selectColumns = (List<Parameter>) params.get(2).getValue();
                for (int i = 0; i < selectColumns.size(); i++) {
                    sb.append(selectColumns.get(i).toString());
                    if (i < selectColumns.size() - 1) {
                        sb.append(", ");
                    }
                }
            }

            sb.append(" FROM " + params.get(0) + "." + params.get(1));
            if (((List)params.get(3).getValue()).size() > 0) {
                sb.append(" " + "WHERE" + " ");
                ParameterType.ConcreteType whereColumnsType = new ParameterType.StreamMapType(
                        null,
                        (s, c) -> (Collection) c.params.get(3).getValue(),
                        p -> ((Pair<Parameter, Parameter>) ((Parameter) p).value).left
                );

                List<Parameter> whereColumns = (List<Parameter>) whereColumnsType.generateRandomParameter(null, this).getValue();
                List<Parameter> whereValues = (List<Parameter>) this.params.get(4).getValue();

                assert whereColumns.size() == whereValues.size();

                for (int i = 0; i < whereColumns.size(); i++) {
                    sb.append(whereColumns.get(i).toString() + " = " + whereValues.get(i).toString());
                    if (i < whereColumns.size() - 1) {
                        sb.append(" AND ");
                    }
                }
            }
            sb.append(";");
            return sb.toString();
        }

        @Override
        public void updateState(State state) { }
    }

    public static class USE extends Command {
        public USE(State state) {
            super();

            assert state instanceof CassandraState;
            CassandraState cassandraState = (CassandraState) state;

            Parameter keyspaceName = chooseKeyspace(cassandraState, this, null);
            this.params.add(keyspaceName); // 0

            updateExecutableCommandString();
        }

        @Override
        public String constructCommandString() {
            StringBuilder sb = new StringBuilder();
            sb.append("USE ").append(params.get(0)).append(";");
            return sb.toString();
        }

        @Override
        public void updateState(State state) { }
    }

    /**
     * This helper function will randomly pick keyspace and return its
     * tablename as parameter.
     */
    public static Parameter chooseKeyspace(State state, Command command, Object init) {

        ParameterType.ConcreteType keyspaceNameType = new ParameterType.InCollectionType(
                CONSTANTSTRINGType.instance,
                (s, c) -> ((CassandraState) s).keyspace2tables.keySet(),
                null
        );
        return keyspaceNameType.generateRandomParameter(state, command, init);
    }

    /**
     * This helper function will randomly pick one table and return its
     * table name as parameter.
     */
    public static Parameter chooseTable(State state, Command command, Object init) {

        ParameterType.ConcreteType TableNameType = new ParameterType.InCollectionType(
                CONSTANTSTRINGType.instance,
                (s, c) -> ((CassandraState) s).keyspace2tables.get(c.params.get(0).toString()).keySet(),
                null
        );
        return TableNameType.generateRandomParameter(state, command, init);
    }

}
