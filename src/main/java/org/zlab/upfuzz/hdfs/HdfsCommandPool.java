package org.zlab.upfuzz.hdfs;

import java.util.AbstractMap;
import org.zlab.upfuzz.CommandPool;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.hdfs.dfscommands.*;
import org.zlab.upfuzz.hdfs.eccommands.*;

public class HdfsCommandPool extends CommandPool {
    // Enable eval_ec_hdfs
    @Override
    public void registerReadCommands() {
        readCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(CatCommand.class, 2));
        readCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(CountCommand.class, 4));
        readCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(LsCommand.class, 1));

        if (Config.getConf().eval_ec_hdfs) {
            readCommandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(
                            GetPolicyCommand.class,
                            10));
            readCommandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(HelpCommand.class,
                            10));
            readCommandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(
                            ListCodecsCommand.class,
                            10));
            readCommandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(
                            ListPoliciesCommand.class, 10));
            readCommandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(HelpCommand.class,
                            10));
        }
    }

    @Override
    public void registerWriteCommands() {
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(CpCommand.class, 2));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(
                        Mkdir.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(
                        MvCommand.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(
                        PutCommand.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(
                        RefreshNodesCommand.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(
                        ReportCommand.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(
                        RmFile.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(
                        RmDir.class, 1));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(
                        RollEditsCommand.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(
                        SaveNamespaceCommand.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(
                        SetaclCommand.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(
                        SetSpaceQuotaCommand.class, 8));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(Touchz.class, 2));

        if (Config.getConf().eval_ec_hdfs) {
            commandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(
                            AddPoliciesCommand.class, 2));
            commandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(
                            DisablePolicyCommand.class, 2));
            commandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(
                            EnablePolicyCommand.class, 2));
            commandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(
                            RemovePolicyCommand.class, 2));
            commandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(
                            SetPolicyCommand.class, 2));
            commandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(
                            UnSetPolicyCommand.class, 2));
        }
    }

    @Override
    public void registerCreateCommands() {
        // -------dfs/dfsadmin-------
        createCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(Mkdir.class, 2));
        createCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(PutCommand.class, 2));
        createCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(Touchz.class, 2));
    }
}
