package org.zlab.upfuzz.hdfs;

import java.util.AbstractMap;
import org.zlab.upfuzz.CommandPool;
import org.zlab.upfuzz.hdfs.dfscommands.*;
import org.zlab.upfuzz.hdfs.eccommands.*;

public class HdfsCommandPool extends CommandPool {

    public HdfsCommandPool() {

        // dfs command
        createCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(Mkdir.class, 2));
        createCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(PutCommand.class, 2));
        createCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(Touchz.class, 2));

        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(CpCommand.class, 2));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(
                        Mkdir.class, 2));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(
                        MvCommand.class, 2));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(
                        PutCommand.class, 2));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(
                        RefreshNodesCommand.class, 2));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(
                        ReportCommand.class, 2));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(
                        RmDir.class, 2));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(
                        RollEditsCommand.class, 2));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(
                        SaveNamespaceCommand.class, 2));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(
                        SetaclCommand.class, 2));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(
                        SetSpaceQuotaCommand.class, 2));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(
                        Touchz.class, 2));

        readCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(CatCommand.class, 10));
        readCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(CountCommand.class, 10));
        readCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(LsCommand.class, 10));

        // -------ec command-------
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

        readCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(GetPolicyCommand.class,
                        10));
        readCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(HelpCommand.class, 10));
        readCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(ListCodecsCommand.class,
                        10));
        readCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(
                        ListPoliciesCommand.class, 10));
        readCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(HelpCommand.class, 10));
    }
}
