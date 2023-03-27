package org.zlab.upfuzz.hbase;

import java.util.AbstractMap;
import org.zlab.upfuzz.CommandPool;
import org.zlab.upfuzz.hbase.AdminCommands.*;
import org.zlab.upfuzz.hbase.DataDefinitionCommands.*;
import org.zlab.upfuzz.hbase.DataManipulationCommands.*;
import org.zlab.upfuzz.hbase.hbasecommands.STATUS;
import org.zlab.upfuzz.hbase.hbasecommands.TABLE_HELP;
import org.zlab.upfuzz.hbase.hbasecommands.VERSION;
import org.zlab.upfuzz.hbase.hbasecommands.WHOAMI;

public class HBaseCommandPool extends CommandPool {

    public HBaseCommandPool() {

        // Data Definition Commands
        commandClassList.add(new AbstractMap.SimpleImmutableEntry<>(ALTER_ADD_FAMILY.class, 1));
        commandClassList.add(new AbstractMap.SimpleImmutableEntry<>(ALTER_DELETE_FAMILY.class, 1));
        commandClassList.add(new AbstractMap.SimpleImmutableEntry<>(CREATE.class, 1));
        commandClassList.add(new AbstractMap.SimpleImmutableEntry<>(DESCRIBE.class, 1));
        commandClassList.add(new AbstractMap.SimpleImmutableEntry<>(DISABLE.class, 1));
        commandClassList.add(new AbstractMap.SimpleImmutableEntry<>(DROP.class, 1));
        commandClassList.add(new AbstractMap.SimpleImmutableEntry<>(ENABLE.class, 1));
        commandClassList.add(new AbstractMap.SimpleImmutableEntry<>(EXISTS.class, 1));
        commandClassList.add(new AbstractMap.SimpleImmutableEntry<>(IS_DISABLED.class, 1));
        commandClassList.add(new AbstractMap.SimpleImmutableEntry<>(IS_ENABLED.class, 1));
        commandClassList.add(new AbstractMap.SimpleImmutableEntry<>(LIST.class, 1));

        // Data Manipulation Commands
        commandClassList.add(new AbstractMap.SimpleImmutableEntry<>(COUNT.class, 1));
        commandClassList.add(new AbstractMap.SimpleImmutableEntry<>(DELETE.class, 1));
        commandClassList.add(new AbstractMap.SimpleImmutableEntry<>(DELETEALL.class, 1));
        commandClassList.add(new AbstractMap.SimpleImmutableEntry<>(GET.class, 1));
        commandClassList.add(new AbstractMap.SimpleImmutableEntry<>(PUT_MODIFY.class, 1));
        commandClassList.add(new AbstractMap.SimpleImmutableEntry<>(PUT_NEW_COLUMN.class, 1));
        commandClassList.add(new AbstractMap.SimpleImmutableEntry<>(PUT_NEW_COLUMN_and_NEW_ITEM.class, 1));
        commandClassList.add(new AbstractMap.SimpleImmutableEntry<>(PUT_NEW_ITEM.class, 1));
        commandClassList.add(new AbstractMap.SimpleImmutableEntry<>(SCAN.class, 1));
        commandClassList.add(new AbstractMap.SimpleImmutableEntry<>(TRUNCATE.class, 1));

        // hbase commands
        commandClassList.add(new AbstractMap.SimpleImmutableEntry<>(STATUS.class, 1));
        commandClassList.add(new AbstractMap.SimpleImmutableEntry<>(TABLE_HELP.class, 1));
        commandClassList.add(new AbstractMap.SimpleImmutableEntry<>(VERSION.class, 1));
        commandClassList.add(new AbstractMap.SimpleImmutableEntry<>(WHOAMI.class, 1));

        // Admin Commands
        commandClassList.add(new AbstractMap.SimpleImmutableEntry<>(COMPACT.class, 1));
        commandClassList.add(new AbstractMap.SimpleImmutableEntry<>(SPLIT.class, 1));
    }
}
