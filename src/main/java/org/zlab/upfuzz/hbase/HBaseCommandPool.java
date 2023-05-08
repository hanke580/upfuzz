package org.zlab.upfuzz.hbase;

import java.util.AbstractMap;
import org.zlab.upfuzz.CommandPool;
import org.zlab.upfuzz.hbase.AdminCommands.*;
import org.zlab.upfuzz.hbase.DataDefinitionCommands.*;
import org.zlab.upfuzz.hbase.DataManipulationCommands.*;
import org.zlab.upfuzz.hbase.hbasecommands.*;

public class HBaseCommandPool extends CommandPool {

    public HBaseCommandPool() {

        // Data Definition Commands
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(CREATE.class, 1));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(ALTER_ADD_FAMILY.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(ALTER_DELETE_FAMILY.class, 5));
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
                new AbstractMap.SimpleImmutableEntry<>(PUT_NEW_ITEM.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(GET.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(PUT_MODIFY.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(PUT_NEW_COLUMN.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(PUT_NEW_COLUMN_and_NEW_ITEM.class, 1));
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
}
