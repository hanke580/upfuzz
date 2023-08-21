package org.zlab.upfuzz.hdfs;

import java.util.AbstractMap;
import org.zlab.upfuzz.CommandPool;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.hdfs.dfsadmin.*;
import org.zlab.upfuzz.hdfs.dfs.*;
import org.zlab.upfuzz.hdfs.ec.*;

public class HdfsCommandPool extends CommandPool {
    // Enable support_EC
    @Override
    public void registerReadCommands() {
        readCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(Cat.class, 2));
        readCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(Count.class, 2));
        readCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(Ls.class, 2));

        if (Config.getConf().support_EC) {
            readCommandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(
                            GetPolicy.class,
                            2));
            readCommandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(Help.class,
                            2));
            readCommandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(
                            ListCodecs.class,
                            2));
            readCommandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(
                            ListPolicies.class, 2));
            readCommandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(Help.class,
                            2));
        }
    }

    @Override
    public void registerWriteCommands() {
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(Cp.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(
                        Mkdir.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(
                        Mv.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(
                        Put.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(
                        RefreshNodes.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(
                        Report.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(
                        RmFile.class, 5));
        // Heuristic: delete command with lower prob
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(
                        RmDir.class, 2));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(
                        RollEdits.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(
                        SaveNamespace.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(
                        Setacl.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(
                        SetSpaceQuota.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(Touchz.class, 5));

        if (Config.getConf().support_EC) {
            commandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(
                            AddPolicies.class, 5));
            commandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(
                            DisablePolicy.class, 5));
            commandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(
                            EnablePolicy.class, 5));
            commandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(
                            RemovePolicy.class, 5));
            commandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(
                            SetPolicy.class, 5));
            commandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(
                            UnSetPolicy.class, 5));
        }
    }

    @Override
    public void registerCreateCommands() {
        // -------dfs/dfsadmin-------
        createCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(Mkdir.class, 2));
        createCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(Put.class, 2));
        createCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(Touchz.class, 2));
    }
}
