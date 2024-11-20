package org.zlab.upfuzz.ozone;

import java.util.AbstractMap;
import java.util.LinkedList;
import java.util.List;

import org.zlab.upfuzz.CommandPool;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.ozone.key.*;
import org.zlab.upfuzz.ozone.bucket.*;
import org.zlab.upfuzz.ozone.fs.*;
import org.zlab.upfuzz.ozone.volume.*;

public class OzoneCommandPool extends CommandPool {

    @Override
    public void registerReadCommands() {
        // readCommandClassList
        // .add(new AbstractMap.SimpleImmutableEntry<>(Df.class, 2));
        readCommandClassList
                .add(new AbstractMap.SimpleImmutableEntry<>(VolumeInfo.class,
                        1));
        readCommandClassList
                .add(new AbstractMap.SimpleImmutableEntry<>(KeyLs.class,
                        2));
        readCommandClassList
                .add(new AbstractMap.SimpleImmutableEntry<>(VolumeGetAcl.class,
                        2));
        readCommandClassList
                .add(new AbstractMap.SimpleImmutableEntry<>(BucketLs.class,
                        2));
        readCommandClassList
                .add(new AbstractMap.SimpleImmutableEntry<>(BucketInfo.class,
                        2));
        readCommandClassList
                .add(new AbstractMap.SimpleImmutableEntry<>(BucketGetAcl.class,
                        2));
        readCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(Cat.class, 2));
        readCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(Checksum.class, 2));
        readCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(Count.class, 2));
        readCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(CatKey.class, 2));
        readCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(KeyInfo.class, 2));
        readCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(KeyGetAcl.class, 2));
        readCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(Ls.class, 2));
    }

    @Override
    public void registerWriteCommands() {
        // commandClassList.add(
        // new AbstractMap.SimpleImmutableEntry<>(AppendToFile.class, 5));
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
                new AbstractMap.SimpleImmutableEntry<>(
                        CreateVolume.class, 3));
        // commandClassList.add(
        // new AbstractMap.SimpleImmutableEntry<>(
        // DeleteVolume.class, 1));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(Put.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(PutKey.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(
                        CreateBucket.class, 3));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(
                        DeleteBucket.class, 3));
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
                new AbstractMap.SimpleImmutableEntry<>(CpKey.class, 3));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(RenameKey.class, 2));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(DeleteKey.class, 3));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(Setacl.class, 5));
    }

    @Override
    public void registerCreateCommands() {
        createCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(
                        CreateVolume.class, 1));
        createCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(
                        CreateBucket.class, 1));
        createCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(Mkdir.class, 2));
        createCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(Touchz.class, 2));
    }
}
