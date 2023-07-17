package org.zlab.upfuzz.cassandra;

import java.util.AbstractMap;
import org.zlab.upfuzz.CommandPool;

import org.zlab.upfuzz.cassandra.cqlcommands.*;

public class CassandraCommandPool extends CommandPool {
    // public static CommandPool instance = new CassandraCommandPool();

    public CassandraCommandPool() {
        commandClassList.add(new AbstractMap.SimpleImmutableEntry<>(
                CREATE_KEYSPACE.class, 1));

        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(ALTER_KEYSPACE.class,
                        5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(ALTER_ROLE.class,
                        5));
        // commandClassList.add(
        // new AbstractMap.SimpleImmutableEntry<>(ALTER_TABLE.class,
        // 2));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(ALTER_TABLE_ADD.class,
                        5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(ALTER_TABLE_DROP.class,
                        5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(ALTER_TABLE_RENAME.class,
                        5));
        // commandClassList.add(
        // new AbstractMap.SimpleImmutableEntry<>(ALTER_TABLE_TYPE.class,
        // 5));
        // commandClassList.add(
        // new AbstractMap.SimpleImmutableEntry<>(ALTER_TYPE.class,
        // 5));
        // commandClassList.add(
        // new AbstractMap.SimpleImmutableEntry<>(ALTER_USER.class,
        // 5));
        // commandClassList.add(
        // new AbstractMap.SimpleImmutableEntry<>(CREATE_INDEX.class, 1));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(CREATE_KEYSPACE.class,
                        1));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(CREATE_TABLE.class, 1));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(CREATE_TYPE.class, 1));

        // commandClassList.add(
        // new AbstractMap.SimpleImmutableEntry<>(DELETE.class, 6));
        // commandClassList.add(
        // new AbstractMap.SimpleImmutableEntry<>(DROP_INDEX.class, 6));
        // commandClassList.add(
        // new AbstractMap.SimpleImmutableEntry<>(DROP_KEYSPACE.class, 2));
        // commandClassList.add(
        // new AbstractMap.SimpleImmutableEntry<>(DROP_TABLE.class, 2));
        // commandClassList.add(
        // new AbstractMap.SimpleImmutableEntry<>(DROP_TYPE.class, 2));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(INSERT.class, 8));
        // commandClassList.add(
        // new AbstractMap.SimpleImmutableEntry<>(TRUNCATE.class, 8));
        // commandClassList.add(
        // new AbstractMap.SimpleImmutableEntry<>(UPDATE.class, 8));
        // commandClassList.add(
        // new AbstractMap.SimpleImmutableEntry<>(USE.class, 5));

        createCommandClassList.add(new AbstractMap.SimpleImmutableEntry<>(
                CREATE_KEYSPACE.class, 2));
        createCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(CREATE_TABLE.class, 3));
        createCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(CREATE_TYPE.class, 1));

        readCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(SELECT.class, 10));
    }
}
