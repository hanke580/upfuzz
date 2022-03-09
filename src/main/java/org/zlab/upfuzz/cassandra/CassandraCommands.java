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

    public static final List<Class<? extends Command>> commandClassList = new ArrayList<>();

    static {
        commandClassList.add(CREATETABLE.class);
    }

    /**
     * TODO: Optional Parameters, need to be impl in a more general way.
     */
    public static class CREATETABLE extends Command {

        // a parameter should correspond to one variable in the text format of this command.
        // mutating a parameter could depend on and the state updated by all nested internal commands and other parameters.
        // Note: Thus, we need to be careful to not have cyclic dependency among parameters.

        // final Command ...; // Nested commands need to be constructed first.

        public CREATETABLE(State state) {
            super();

            assert state instanceof CassandraState;
            CassandraState cassandraState = (CassandraState) state;

            ParameterType.ConcreteType tableNameType = new ParameterType.NotInCollectionType(
                    new ParameterType.NotEmpty(
                            STRINGType.instance
                    ),
                    (Collection) cassandraState.tables,
                    p -> ((CassandraTable) p).name
            );

            Parameter tableName = tableNameType.generateRandomParameter(cassandraState, this);
            params.add(tableName);

            ParameterType.ConcreteType columnsType = // LIST<PAIR<String,TYPEType>>
                ParameterType.ConcreteGenericType.constructConcreteGenericType(
                    CassandraTypes.MapLikeListType.instance,
                        ParameterType.ConcreteGenericType.constructConcreteGenericType(PAIRType.instance,
                            STRINGType.instance, CassandraTypes.TYPEType.instance));
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
                    new ParameterType.SubsetType(
                            columnsType,
                            (Collection) columns.value,
                            p -> ((Pair<Parameter, Parameter>) ((Parameter) p).value).left
                    );
            /**
             * List type for the columns
             * - genRanParam()
             *      - check the current command(this) parameters' second parameter (Columns)
             *      - get subset from it.
             */

            Parameter primaryColumns = primaryColumnsType.generateRandomParameter(cassandraState, this);
            params.add(primaryColumns);

            ParameterType.ConcreteType IF_NOT_EXISTType = new ParameterType.OptionalType(
                    new CONSTANTSTRINGType("IF NOT EXIST"), null   // TODO: Make a pure CONSTANTType
            );
            Parameter IF_NOT_EXIST = IF_NOT_EXISTType.generateRandomParameter(cassandraState, this);
            params.add(IF_NOT_EXIST);
        }

        @Override
        public String constructCommandString() {
            // TODO: Need a helper function, add space between all strings
            Parameter tableName = params.get(0);
            Parameter columns = params.get(1); // LIST<PAIR<TEXTType,TYPE>>
            Parameter primaryColumns = params.get(2);
            Parameter IF_NOT_EXIST = params.get(3);

            assert tableName.toString().isEmpty() == false;

            String ret = "CREATE TABLE " + IF_NOT_EXIST.toString() + " " + tableName.toString() + "(" +
                    columns.toString() + " WITH PRIMARY KEY (" +
                    primaryColumns.toString() + " )" +
                    ");";
            /**
             * primaryColumns also contains keywords (Optional)
             * - " WITH PRIMARY KEY (" + primaryColumns.toString() + " )"
             * Check the current command, column definitions (Whether it's followed by 'Primary key'
             * - Across multiple Param
             */
            return ret;
        }

        @Override
        public void updateState(State state) {
            Parameter tableName = params.get(0);
            Parameter columns = params.get(1); // LIST<PAIR<TEXTType,TYPE>>
            Parameter primaryColumns = params.get(2);

            CassandraTable table = new CassandraTable(tableName, columns, primaryColumns);
            ((CassandraState) state).addTable(table);
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

            ParameterType.ConcreteType TableNameType = new ParameterType.PickOneFromSetType(
                    CONSTANTSTRINGType.instance,
                    cassandraState.tables,
                    p -> ((CassandraTable) p).name
            );
            Parameter TableName = TableNameType.generateRandomParameter(cassandraState, this);
            this.params.add(TableName);

            /**
             * TODO: Transfer TYPEType to exact values
             */
        }

        @Override
        public String constructCommandString() {
            return this.params.get(0).toString();
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

}
