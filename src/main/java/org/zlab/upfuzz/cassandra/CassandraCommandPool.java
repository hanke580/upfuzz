package org.zlab.upfuzz.cassandra;

import java.util.AbstractMap;
import org.zlab.upfuzz.CommandPool;

import org.zlab.upfuzz.cassandra.cqlcommands.*;
import org.zlab.upfuzz.fuzzingengine.Config;

public class CassandraCommandPool extends CommandPool {
    public static int boundaryWriteCommandRate = 15;
    public static int basicCommandRate = 1;
    public static int createCommandRate = 5;
    public static int writeCommandRate = 5;
    public static int readCommandRate = 5;
    public static int deleteLargeDataRate = 1;

    public void eval_UnitTest() {
        createCommandClassList.add(new AbstractMap.SimpleImmutableEntry<>(
                CREATE_KEYSPACE.class, createCommandRate));
        createCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(CREATE_TABLE.class,
                        createCommandRate));
        commandClassList.add(new AbstractMap.SimpleImmutableEntry<>(
                CREATE_KEYSPACE.class, basicCommandRate));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(CREATE_TABLE.class,
                        writeCommandRate));
    }

    public void eval_CASSANDRA13939() {
        // limit to only one table for each test
        commandClassList.add(new AbstractMap.SimpleImmutableEntry<>(
                CREATE_KEYSPACE.class, basicCommandRate));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(CREATE_TABLE.class,
                        writeCommandRate));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(INSERT.class,
                        boundaryWriteCommandRate));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(ALTER_TABLE_DROP.class,
                        writeCommandRate));

        createCommandClassList.add(new AbstractMap.SimpleImmutableEntry<>(
                CREATE_KEYSPACE.class, createCommandRate));
        createCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(CREATE_TABLE.class,
                        createCommandRate));
    }

    public void eval_CASSANDRA14912() {
        // Create commands
        createCommandClassList.add(new AbstractMap.SimpleImmutableEntry<>(
                CREATE_KEYSPACE.class, createCommandRate));
        createCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(CREATE_TABLE.class,
                        createCommandRate));

        // Write commands
        commandClassList.add(new AbstractMap.SimpleImmutableEntry<>(
                CREATE_KEYSPACE.class, writeCommandRate));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(CREATE_TABLE.class,
                        writeCommandRate));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(INSERT.class,
                        writeCommandRate));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(ALTER_TABLE_DROP.class,
                        writeCommandRate));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(ALTER_TABLE_ADD.class,
                        writeCommandRate));
    }

    public void eval_CASSANDRA15970() {
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(DELETE.class,
                        writeCommandRate));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(ALTER_TABLE_DROP.class,
                        writeCommandRate));

        createCommandClassList.add(new AbstractMap.SimpleImmutableEntry<>(
                CREATE_KEYSPACE.class, createCommandRate));
        createCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(CREATE_TABLE.class,
                        createCommandRate));
    }

    @Override
    public void registerReadCommands() {
        readCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(SELECT.class,
                        readCommandRate));
    }

    @Override
    public void registerWriteCommands() {
        if (Config.getConf().eval_UnitTest) {
            eval_UnitTest();
            return;
        }
        if (Config.getConf().eval_CASSANDRA13939) {
            eval_CASSANDRA13939();
            return;
        }
        if (Config.getConf().eval_CASSANDRA14912) {
            eval_CASSANDRA14912();
            return;
        }
        if (Config.getConf().eval_CASSANDRA15970) {
            eval_CASSANDRA15970();
            return;
        }
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(ALTER_KEYSPACE.class,
                        writeCommandRate));
        // commandClassList.add(
        // new AbstractMap.SimpleImmutableEntry<>(ALTER_ROLE.class,
        // writeCommandRate));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(ALTER_TABLE.class,
                        writeCommandRate));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(ALTER_TABLE_ADD.class,
                        writeCommandRate));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(ALTER_TABLE_DROP.class,
                        writeCommandRate));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(ALTER_TABLE_RENAME.class,
                        writeCommandRate));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(ALTER_TABLE_TYPE.class,
                        writeCommandRate));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(ALTER_TYPE.class,
                        writeCommandRate));
        // commandClassList.add(
        // new AbstractMap.SimpleImmutableEntry<>(ALTER_USER.class,
        // writeCommandRate));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(CREATE_INDEX.class,
                        writeCommandRate));
        commandClassList.add(new AbstractMap.SimpleImmutableEntry<>(
                CREATE_KEYSPACE.class, basicCommandRate));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(CREATE_TABLE.class,
                        writeCommandRate));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(CREATE_TYPE.class,
                        writeCommandRate));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(DELETE.class,
                        writeCommandRate));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(DROP_INDEX.class,
                        writeCommandRate));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(DROP_KEYSPACE.class,
                        deleteLargeDataRate));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(DROP_TABLE.class,
                        deleteLargeDataRate));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(DROP_TYPE.class,
                        writeCommandRate));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(INSERT.class,
                        writeCommandRate));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(TRUNCATE.class,
                        deleteLargeDataRate));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(UPDATE.class,
                        writeCommandRate));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(USE.class,
                        writeCommandRate));
    }

    @Override
    public void registerCreateCommands() {
        if (Config.getConf().eval_UnitTest
                || Config.getConf().eval_CASSANDRA13939
                || Config.getConf().eval_CASSANDRA14912) {
            // commands added when registerWriteCommands() is invoked.
            return;
        }
        createCommandClassList.add(new AbstractMap.SimpleImmutableEntry<>(
                CREATE_KEYSPACE.class, createCommandRate));
        createCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(CREATE_TABLE.class,
                        createCommandRate));
        createCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(CREATE_TYPE.class,
                        createCommandRate));
    }
}
