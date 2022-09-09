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
                new AbstractMap.SimpleImmutableEntry<>(CREATE_TABLE.class, 1));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(INSERT.class, 8));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(DELETE.class, 6));
        // commandClassList.add(
        // new AbstractMap.SimpleImmutableEntry<>(SELECT.class, 8));

        createCommandClassList.add(new AbstractMap.SimpleImmutableEntry<>(
                CREATE_KEYSPACE.class, 2));
        createCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(CREATE_TABLE.class, 3));

        readCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(SELECT.class, 10));
    }
}
