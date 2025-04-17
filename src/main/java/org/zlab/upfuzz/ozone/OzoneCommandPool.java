package org.zlab.upfuzz.ozone;

import java.util.AbstractMap;

import org.zlab.upfuzz.CommandPool;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.ozone.fs.*;
import org.zlab.upfuzz.ozone.sh.bucket.*;
import org.zlab.upfuzz.ozone.sh.key.*;
import org.zlab.upfuzz.ozone.sh.volume.CreateVolume;
import org.zlab.upfuzz.ozone.sh.volume.DeleteVolume;
import org.zlab.upfuzz.ozone.sh.volume.VolumeGetAcl;
import org.zlab.upfuzz.ozone.sh.volume.VolumeInfo;

public class OzoneCommandPool extends CommandPool {
    public static int createCommandRate = 5;
    public static int writeCommandRate = 5;
    public static int readCommandRate = 5;
    public static int deleteLargeDataRate = 1;

    @Override
    public void registerReadCommands() {
        if (Config.getConf().testFSCommands) {
            readCommandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(Cat.class,
                            readCommandRate));
            readCommandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(Checksum.class,
                            readCommandRate));
            readCommandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(Count.class,
                            readCommandRate));
        }
        if (Config.getConf().testSHCommands) {
            if (Config.getConf().enable_VolumeInfo) {
                readCommandClassList
                        .add(new AbstractMap.SimpleImmutableEntry<>(
                                VolumeInfo.class,
                                readCommandRate));
            }
            if (Config.getConf().enable_KeyLs) {
                readCommandClassList
                        .add(new AbstractMap.SimpleImmutableEntry<>(KeyLs.class,
                                readCommandRate));
            }
            readCommandClassList
                    .add(new AbstractMap.SimpleImmutableEntry<>(
                            VolumeGetAcl.class,
                            readCommandRate));

            // FIXME: implemented wrong
            // readCommandClassList
            // .add(new AbstractMap.SimpleImmutableEntry<>(BucketLs.class,
            // readCommandRate));
            if (Config.getConf().enable_BucketInfo) {
                readCommandClassList
                        .add(new AbstractMap.SimpleImmutableEntry<>(
                                BucketInfo.class,
                                readCommandRate));
            }
            readCommandClassList
                    .add(new AbstractMap.SimpleImmutableEntry<>(
                            BucketGetAcl.class,
                            readCommandRate));
            readCommandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(CatKey.class,
                            readCommandRate));
            readCommandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(KeyInfo.class,
                            readCommandRate));
            readCommandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(KeyGetAcl.class,
                            readCommandRate));
            readCommandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(Ls.class,
                            readCommandRate));
        }
    }

    @Override
    public void registerWriteCommands() {
        if (Config.getConf().testFSCommands) {
            commandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(Chmod.class, 3));
            commandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(Cp.class, 10));
            if (Config.getConf().support_createSnapshot) {
                commandClassList.add(
                        new AbstractMap.SimpleImmutableEntry<>(
                                CreateSnapshot.class,
                                2));
            }
            commandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(Expunge.class, 2));
            commandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(Put.class, 5));
            commandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(
                            Mkdir.class, 5));
            commandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(
                            Mv.class, 5));
            commandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(
                            RmDir.class, 2));
            commandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(
                            RmFile.class, 2));
            commandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(Touchz.class, 3));
            commandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(Setacl_RM.class, 4));
            commandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(Setacl_Set.class,
                            4));
        }
        if (Config.getConf().testSHCommands) {
            commandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(
                            CreateVolume.class, writeCommandRate));
            commandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(
                            DeleteVolume.class, deleteLargeDataRate));
            commandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(PutKey.class,
                            writeCommandRate));
            commandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(CreateBucket.class,
                            writeCommandRate));
            commandClassList.add(new AbstractMap.SimpleImmutableEntry<>(
                    DeleteBucket.class, deleteLargeDataRate));
            commandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(CpKey.class,
                            writeCommandRate));
            commandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(RenameKey.class,
                            writeCommandRate));
            commandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(DeleteKey.class,
                            writeCommandRate));
        }
    }

    @Override
    public void registerCreateCommands() {
        if (Config.getConf().testSHCommands) {
            createCommandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(
                            CreateVolume.class, createCommandRate));
            createCommandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(
                            CreateBucket.class, createCommandRate));
        }
        if (Config.getConf().testFSCommands) {
            createCommandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(Mkdir.class, 2));
            createCommandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(Touchz.class, 2));
        }
    }
}
