package org.zlab.upfuzz.cassandra;

import org.zlab.upfuzz.*;
import org.zlab.upfuzz.utils.PAIRType;
import org.zlab.upfuzz.utils.FIXSTRINGType;
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
            params = new ArrayList<>();

            assert state instanceof CassandraState;
            CassandraState cassandraState = (CassandraState) state;

            /**
             * There should be a not in collection constraints
             * TODO: Add a not empty constraints here.
             */
            ParameterType.ConcreteType tableNameType = new ParameterType.NotInCollectionType(
                    STRINGType.instance,
                    (Collection) cassandraState.tables,
                    p -> ((CassandraTable) p).name
            );

            Parameter tableName = tableNameType.generateRandomParameter(cassandraState, this);
            params.add(tableName);

            ParameterType.ConcreteType columnsType = // LIST<PAIR<TEXT,TYPE>>
                ParameterType.ConcreteGenericType.constructConcreteGenericType(
                    CassandraTypes.MapLikeListType.instance,
                        ParameterType.ConcreteGenericType.constructConcreteGenericType(PAIRType.instance,
                            STRINGType.instance, CassandraTypes.TYPEType.instance));
            Parameter columns = columnsType.generateRandomParameter(cassandraState, this);
            params.add(columns);

            ParameterType.ConcreteType primaryColumnsType =
                    new ParameterType.SubsetType(
                            columnsType,
                            (Collection) columns.value,
                            p -> ((Pair) p).left
                    );

            Parameter primaryColumns = primaryColumnsType.generateRandomParameter(cassandraState, this);
            params.add(primaryColumns);

            ParameterType.ConcreteType IF_NOT_EXISTType = new FIXSTRINGType("IF NOT EXIST");
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
                    columns.toString() + "\n WITH PRIMARY KEY (" +
                    primaryColumns.toString() + " )" +
                    ");";
            return ret;
        }

        @Override
        public void updateState(State state) {
            Parameter tableName = params.get(0);
            Parameter columns = params.get(1); // LIST<PAIR<TEXTType,TYPE>>
            Parameter primaryColumns = params.get(2);
            Parameter IF_NOT_EXIST = params.get(3);

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
//    public static class INSERT implements Command {
//        @Override
//        public String constructCommandString() {
//            return null;
//        }
//
//        @Override
//        public void updateState() {
//
//        }
//    }

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
