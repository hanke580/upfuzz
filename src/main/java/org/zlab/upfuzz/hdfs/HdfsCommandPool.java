package org.zlab.upfuzz.hdfs;

import java.util.AbstractMap;

import org.zlab.upfuzz.CommandPool;
import org.zlab.upfuzz.hdfs.dfscommands.CatCommand;
import org.zlab.upfuzz.hdfs.dfscommands.CpCommand;
import org.zlab.upfuzz.hdfs.dfscommands.LsCommand;
import org.zlab.upfuzz.hdfs.dfscommands.MvCommand;
import org.zlab.upfuzz.hdfs.dfscommands.PutCommand;
import org.zlab.upfuzz.hdfs.dfscommands.RefreshNodesCommand;
import org.zlab.upfuzz.hdfs.dfscommands.ReportCommand;
import org.zlab.upfuzz.hdfs.dfscommands.RmCommand;
import org.zlab.upfuzz.hdfs.dfscommands.RollEditsCommand;
import org.zlab.upfuzz.hdfs.dfscommands.SaveNamespaceCommand;

public class HdfsCommandPool extends CommandPool {

    public HdfsCommandPool() {
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(PutCommand.class, 8));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(CpCommand.class, 2));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(MvCommand.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(RmCommand.class, 5));
        createCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(PutCommand.class, 2));
        createCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(LsCommand.class, 3));
        createCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(CatCommand.class, 1));
        createCommandClassList.add(new AbstractMap.SimpleImmutableEntry<>(
                RollEditsCommand.class, 2));
        createCommandClassList.add(new AbstractMap.SimpleImmutableEntry<>(
                SaveNamespaceCommand.class, 2));
        createCommandClassList.add(new AbstractMap.SimpleImmutableEntry<>(
                RefreshNodesCommand.class, 1));
        createCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(ReportCommand.class, 1));

        readCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(LsCommand.class, 10));
        readCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(CatCommand.class, 2));
        readCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(ReportCommand.class, 1));

    }

}
