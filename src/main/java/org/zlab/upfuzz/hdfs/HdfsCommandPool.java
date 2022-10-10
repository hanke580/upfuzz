package org.zlab.upfuzz.hdfs;

import java.util.AbstractMap;
import org.zlab.upfuzz.CommandPool;
import org.zlab.upfuzz.hdfs.dfscommands.*;

public class HdfsCommandPool extends CommandPool {

    public HdfsCommandPool() {

        createCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(Mkdir.class, 2));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(
                        SetSpaceQuotaCommand.class, 3));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(Mkdir.class, 3));
        readCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(CountCommand.class, 10));

        // commandClassList.add(
        // new AbstractMap.SimpleImmutableEntry<>(PutCommand.class, 8));
        // commandClassList.add(
        // new AbstractMap.SimpleImmutableEntry<>(CpCommand.class, 2));
        // commandClassList.add(
        // new AbstractMap.SimpleImmutableEntry<>(MvCommand.class, 5));
        // commandClassList.add(
        // new AbstractMap.SimpleImmutableEntry<>(RmCommand.class, 5));
        // createCommandClassList.add(
        // new AbstractMap.SimpleImmutableEntry<>(PutCommand.class, 2));
        // createCommandClassList.add(
        // new AbstractMap.SimpleImmutableEntry<>(LsCommand.class, 3));
        // createCommandClassList.add(
        // new AbstractMap.SimpleImmutableEntry<>(CatCommand.class, 1));
        // createCommandClassList.add(
        // new AbstractMap.SimpleImmutableEntry<>(RollEditsCommand.class,
        // 2));
        // createCommandClassList.add(new AbstractMap.SimpleImmutableEntry<>(
        // SaveNamespaceCommand.class, 2));
        // createCommandClassList.add(new AbstractMap.SimpleImmutableEntry<>(
        // RefreshNodesCommand.class, 1));
        // createCommandClassList.add(
        // new AbstractMap.SimpleImmutableEntry<>(ReportCommand.class, 1));
        //
        // readCommandClassList.add(
        // new AbstractMap.SimpleImmutableEntry<>(LsCommand.class, 10));
        // readCommandClassList.add(
        // new AbstractMap.SimpleImmutableEntry<>(CatCommand.class, 2));
        // readCommandClassList.add(
        // new AbstractMap.SimpleImmutableEntry<>(ReportCommand.class, 1));
    }
}
