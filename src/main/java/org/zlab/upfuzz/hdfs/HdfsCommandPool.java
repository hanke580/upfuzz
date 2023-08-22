package org.zlab.upfuzz.hdfs;

import java.util.AbstractMap;
import java.util.LinkedList;
import java.util.List;

import org.zlab.upfuzz.CommandPool;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.hdfs.dfsadmin.*;
import org.zlab.upfuzz.hdfs.dfs.*;
import org.zlab.upfuzz.hdfs.ec.*;

public class HdfsCommandPool extends CommandPool {

    @Override
    public void registerReadCommands() {
        // dfs read
        readCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(Cat.class, 2));
        readCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(Checksum.class, 2));
        readCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(Count.class, 2));
        readCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(Df.class, 2));
        readCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(Du.class, 2));
        readCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(Ls.class, 2));
        // dfs admin
        readCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(ListOpenFiles.class, 2));
        readCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(PrintTopology.class, 2));
        readCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(Report.class, 2));
        // ec
        if (Config.getConf().support_EC) {
            readCommandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(
                            GetPolicy.class, 2));
            readCommandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(
                            ListCodecs.class,
                            2));
            readCommandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(
                            ListPolicies.class, 2));
        }
    }

    @Override
    public void registerWriteCommands() {
        // dfs
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(AppendToFile.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(Chmod.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(Cp.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(CreateSnapshot.class,
                        5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(Expunge.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(Get.class, 5));
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
                        RmDir.class, 2));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(
                        RmFile.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(
                        Setacl.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(
                        SpecialMkdir.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(Touchz.class, 5));
        // dfsadmin
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(ClrQuota.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(ClrSpaceQuota.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(Metasave.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(RefreshCallQueue.class,
                        5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(RefreshNodes.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(RefreshServiceAcl.class,
                        5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(
                        RefreshSuperUserGroupsConfiguration.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(
                        RefreshUserToGroupsMappings.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(
                        RestoreFailedStorage.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(RollEdits.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(SaveNamespace.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(
                        SetBalancerBandwidth.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(SetQuota.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(SetSpaceQuota.class, 5));
        // ec
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
            commandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(
                            VerifyClusterSetup.class, 5));
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
