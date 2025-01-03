package org.zlab.upfuzz.ozone;

import java.util.AbstractMap;

import org.zlab.upfuzz.CommandPool;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.ozone.key.*;
import org.zlab.upfuzz.ozone.bucket.*;
import org.zlab.upfuzz.ozone.fs.*;
import org.zlab.upfuzz.ozone.sh.bucket.*;
import org.zlab.upfuzz.ozone.sh.volume.CreateVolume;
import org.zlab.upfuzz.ozone.sh.volume.DeleteVolume;
import org.zlab.upfuzz.ozone.sh.volume.VolumeGetAcl;
import org.zlab.upfuzz.ozone.sh.volume.VolumeInfo;
import org.zlab.upfuzz.ozone.volume.*;

public class OzoneCommandPool extends CommandPool {
    public static int basicCommandRate = 1;
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
        readCommandClassList
                .add(new AbstractMap.SimpleImmutableEntry<>(VolumeInfo.class,
                        readCommandRate));
        readCommandClassList
                .add(new AbstractMap.SimpleImmutableEntry<>(KeyLs.class,
                        readCommandRate));
        readCommandClassList
                .add(new AbstractMap.SimpleImmutableEntry<>(VolumeGetAcl.class,
                        readCommandRate));
        readCommandClassList
                .add(new AbstractMap.SimpleImmutableEntry<>(BucketLs.class,
                        readCommandRate));
        readCommandClassList
                .add(new AbstractMap.SimpleImmutableEntry<>(BucketInfo.class,
                        readCommandRate));
        readCommandClassList
                .add(new AbstractMap.SimpleImmutableEntry<>(BucketGetAcl.class,
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

    @Override
    public void registerWriteCommands() {
        if (Config.getConf().testFSCommands) {
            commandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(Chmod.class, 3));
            commandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(Cp.class, 10));
            commandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(CreateSnapshot.class,
                            2));
            commandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(Expunge.class, 5));
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
                            RmFile.class, 3));
            commandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(Touchz.class, 3));
            commandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(Setacl.class, 5));
        }
        if (Config.getConf().testSHCommands) {
            commandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(
                            CreateVolume.class, 3));
            commandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(
                            DeleteVolume.class, deleteLargeDataRate));
            commandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(PutKey.class, 5));
            commandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(
                            CreateBucket.class, 3));
            commandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(
                            DeleteBucket.class, 3));
            commandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(CpKey.class, 3));
            commandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(RenameKey.class, 2));
            commandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(DeleteKey.class, 3));
        }
    }

    @Override
    public void registerCreateCommands() {
        if (Config.getConf().testSHCommands) {
            createCommandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(
                            CreateVolume.class, 1));
            createCommandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(
                            CreateBucket.class, 1));
        }
        if (Config.getConf().testFSCommands) {
            createCommandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(Mkdir.class, 2));
            createCommandClassList.add(
                    new AbstractMap.SimpleImmutableEntry<>(Touchz.class, 2));
        }
    }
}
