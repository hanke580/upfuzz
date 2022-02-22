package org.zlab.upfuzz.cassandra;

import org.zlab.upfuzz.*;
import org.zlab.upfuzz.utils.PAIRType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * TODO:
 *   1. nested commands & scope // we could do it in a simple way without scope first...
 *   2. mutate() & isValid() // we implemented generateValue() for each type
 *   3. user defined type // we need to implement a UnionType, each instance of a UnionType could be a user defined type
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
    public static class CREATETABLE implements Command {

        // parameters used in this command
        // they are generated following user-specified constraints

        // a parameter should correspond to one variable in the text format of this command.
        // mutating a parameter could depend on and the state updated by all nested internal commands and other parameters.
        // Note: Thus, we need to be careful to not have cyclic dependency among parameters.

        final Parameter tableName; // TEXTType

        final Parameter columns; // LIST<PAIR<TEXTType,TYPE>>

        final Parameter primaryColumns;

        final Parameter IF_NOT_EXIST;

        // final Command ...; // Nested commands need to be constructed first.

        public CREATETABLE(CassandraState state) {

            ParameterType.ConcreteType tableNameType = CassandraTypes.TEXTType.instance; // TEXTType
            tableName = tableNameType.generateRandomParameter(state, this);

            ParameterType.ConcreteType columnsType = // LIST<PAIR<TEXT,TYPE>>
                ParameterType.ConcreteGenericType.constructConcreteGenericType(
                    CassandraTypes.LISTType.instance,
                    ParameterType.ConcreteGenericType.constructConcreteGenericType(PAIRType.instance,
                        CassandraTypes.TEXTType.instance, CassandraTypes.TYPEType.instance));
            columns = columnsType.generateRandomParameter(state, this);

            primaryColumns = null;
            IF_NOT_EXIST = null;
        }

        @Override
        public String constructCommandString() {
            return null;
        }

        @Override
        public void updateState() {

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
    public static class INSERT implements Command {
        @Override
        public String constructCommandString() {
            return null;
        }

        @Override
        public void updateState() {

        }
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
