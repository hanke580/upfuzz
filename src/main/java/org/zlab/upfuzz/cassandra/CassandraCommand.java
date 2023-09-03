package org.zlab.upfuzz.cassandra;

import org.zlab.upfuzz.*;
import org.zlab.upfuzz.utils.*;

/**
 * TODO:
 *   1. nested commands & scope // we could do it in a simple way without scope
 * first...
 *   2. mutate() & isValid() // we implemented generateRandomParameter() for
 * each type
 *   3. user defined type // we need to implement a UnionType, each instance of
 * a UnionType could be a user defined type
 *   4. mutate methods.
 *   - Shouldn't be operated by user.
 *   - When we call the command.mutate, (Conduct param level mutation)
 *   - It should pick one parameter defined in this command, and call its
 * mutation method.
 *   - Then for the rest command, it should run check() method to do the minor
 * modification or not.
 */
public abstract class CassandraCommand extends Command {
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

    // For testing purpose, the fuzzer can new this object again
    public static CassandraCommandPool cassandraCommandPool = new CassandraCommandPool();

    public static final String[] reservedKeywords = {
            "ADD", "AGGREGATE", "ALL", "ALLOW",
            "ALTER", "AND", "ANY", "APPLY",
            "AS", "ASC", "ASCII", "AUTHORIZE",
            "BATCH", "BEGIN", "BIGINT", "BLOB",
            "BOOLEAN", "BY", "CLUSTERING", "COLUMNFAMILY",
            "COMPACT", "CONSISTENCY", "COUNT", "COUNTER",
            "CREATE", "CUSTOM", "DECIMAL", "DELETE",
            "DESC", "DISTINCT", "DOUBLE", "DROP",
            "EACH_QUORUM", "ENTRIES", "EXISTS", "FILTERING",
            "FLOAT", "FROM", "FROZEN", "FULL",
            "GRANT", "IF", "IN", "INDEX",
            "INET", "INFINITY", "INSERT", "INT",
            "INTO", "KEY", "KEYSPACE", "KEYSPACES",
            "LEVEL", "LIMIT", "LIST", "LOCAL_ONE",
            "LOCAL_QUORUM", "MAP", "MATERIALIZED", "MODIFY",
            "NAN", "NORECURSIVE", "NOSUPERUSER", "NOT",
            "OF", "ON", "ONE", "ORDER",
            "PARTITION", "PASSWORD", "PER", "PERMISSION",
            "PERMISSIONS", "PRIMARY", "QUORUM", "RENAME",
            "REVOKE", "SCHEMA", "SELECT", "SET",
            "STATIC", "STORAGE", "SUPERUSER", "TABLE",
            "TEXT", "TIME", "TIMESTAMP", "TIMEUUID",
            "THREE", "TO", "TOKEN", "TRUNCATE",
            "TTL", "TUPLE", "TWO", "TYPE",
            "UNLOGGED", "UPDATE", "USE", "USER",
            "USERS", "USING", "UUID", "VALUES",
            "VARCHAR", "VARINT", "VIEW", "WHERE",
            "WITH", "WRITETIM", "IS", "OR" };

    /**
     * This helper function will randomly pick keyspace and return its
     * tablename as parameter.
     */
    public static Parameter chooseKeyspace(State state, Command command,
            Object init) {

        ParameterType.ConcreteType keyspaceNameType = new ParameterType.InCollectionType(
                CONSTANTSTRINGType.instance,
                (s, c) -> Utilities.strings2Parameters(
                        ((CassandraState) s).keyspace2tables.keySet()),
                null);
        return keyspaceNameType.generateRandomParameter(state, command, init);
    }

    /**
     * This helper function will randomly pick one table and return its
     * table name as parameter.
     */
    public static Parameter chooseTable(State state, Command command,
            Object init) {

        ParameterType.ConcreteType tableNameType = new ParameterType.InCollectionType(
                CONSTANTSTRINGType.instance,
                (s, c) -> Utilities
                        .strings2Parameters(((CassandraState) s).keyspace2tables
                                .get(c.params.get(0).toString())
                                .keySet()),
                null);
        return tableNameType.generateRandomParameter(state, command, init);
    }

    @Override
    public void separate(State state) {
    }
}
