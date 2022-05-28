package org.zlab.upfuzz.hdfs;

import java.util.AbstractMap;

import org.zlab.upfuzz.CommandPool;
import org.zlab.upfuzz.hdfs.dfscommands.CpCommand;
import org.zlab.upfuzz.hdfs.dfscommands.MvCommand;
import org.zlab.upfuzz.hdfs.dfscommands.PutCommand;
import org.zlab.upfuzz.hdfs.dfscommands.RmCommand;

public class HdfsCommandPool extends CommandPool {

    public HdfsCommandPool() {
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(PutCommand.class, 10));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(CpCommand.class, 1));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(MvCommand.class, 8));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(RmCommand.class, 6));

        createCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(PutCommand.class, 2));

        readCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(MvCommand.class, 10));
    }

}
