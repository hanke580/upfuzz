package org.zlab.upfuzz.hbase;

import java.util.AbstractMap;
import org.zlab.upfuzz.CommandPool;
import org.zlab.upfuzz.hbase.tools.*;
import org.zlab.upfuzz.hbase.ddl.*;
import org.zlab.upfuzz.hbase.dml.*;

public class HBaseCommandPool extends CommandPool {

    @Override
    public void registerReadCommands() {
        readCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(SCAN.class, 5));
        readCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(GET.class, 5));
    }

    @Override
    public void registerWriteCommands() {
        // Data Definition Commands
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(CREATE.class, 1));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(ALTER_ADD_FAMILY.class,
                        5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(
                        ALTER_DELETE_FAMILY.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(DISABLE.class, 1));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(ENABLE.class, 1));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(DROP.class, 1));

        // Data Manipulation Commands
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(DELETE.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(DELETEALL.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(PUT_MODIFY.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(PUT_NEW.class,
                        5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(
                        PUT_NEW.class, 1));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(SCAN.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(TRUNCATE.class, 5));

        // Admin Commands
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(COMPACT.class, 1));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(SPLIT.class, 1));
    }

    @Override
    public void registerCreateCommands() {
        createCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(CREATE.class, 1));
    }

}
