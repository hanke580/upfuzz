package org.zlab.upfuzz.hdfs.dfs;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HDFSParameterType.ConcatenateType;
import org.zlab.upfuzz.hdfs.HDFSParameterType.HDFSDirPathType;
import org.zlab.upfuzz.hdfs.HDFSParameterType.RandomLocalPathType;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.hdfs.MockFS.HadoopFileSystem;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.utils.INTType;

public class Put extends Dfs {

    /*
     * Copy single src, or multiple srcs from local file system to the
     * destination file system. Also reads input from stdin and writes to
     * destination file system if the source is set to “-” Copying fails if the
     * file already exists, unless the -f flag is given.
     */
    public Put(HdfsState state) {
        super(state.subdir);

        Parameter putcmd = new CONSTANTSTRINGType("-put")
                .generateRandomParameter(null, null);

        // -f : Overwrites the destination if it already exists.
        Parameter fOption = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-f"), null)
                        .generateRandomParameter(null, null);

        // -p : Preserves access and modification times, ownership and the
        // permissions. (assuming the permissions can be propagated across
        // filesystems)
        Parameter pOption = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-p"), null)
                        .generateRandomParameter(null, null);

        // -l : Allow DataNode to lazily persist the file to disk, Forces a
        // replication factor of 1. This flag will result in reduced durability.
        // Use with care.
        // Parameter lOption = new ParameterType.OptionalType(
        // new CONSTANTSTRINGType("-l"), null)
        // .generateRandomParameter(null, null);

        // -d : Skip creation of temporary file with the suffix ._COPYING_.
        Parameter dOption = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-d"), null)
                        .generateRandomParameter(null, null);

        // -t <thread count> : Number of threads to be used, default is 1.
        // Useful when uploading directories containing more than 1 file.
        Parameter tOption = new CONSTANTSTRINGType("-t")
                .generateRandomParameter(null, null);
        Parameter threadNumberParameter = new INTType(1, 16 + 1)
                .generateRandomParameter(null, null);
        Parameter threadOption = new ParameterType.OptionalType(
                new ConcatenateType(tOption, threadNumberParameter), null)
                        .generateRandomParameter(null, null);

        // -q <thread pool queue size> : Thread pool queue size to be used,
        // default is 1024. It takes effect only when thread count greater than
        // 1.

        Parameter qOption = new CONSTANTSTRINGType("-q")
                .generateRandomParameter(null, null);
        Parameter poolQueueParameter = new INTType(1024, 65536 + 1)
                .generateRandomParameter(null, null);
        Parameter threadQueueOption = new ParameterType.OptionalType(
                new ConcatenateType(qOption, poolQueueParameter),
                null)
                        .generateRandomParameter(null, null);

        Parameter srcParameter = new RandomLocalPathType()
                .generateRandomParameter(state, null);

        Parameter dstParameter = new HDFSDirPathType() // give a subpath
                                                       // here
                .generateRandomParameter(state, null);

        params.add(putcmd);
        params.add(fOption);
        params.add(pOption);
        // params.add(lOption);
        params.add(dOption);
        // params.add(threadOption);
        // params.add(threadQueueOption);
        params.add(srcParameter);
        params.add(dstParameter);
    }

    @Override
    public String constructCommandString() {
        return "dfs" + " " +
                params.get(0) + " " +
                params.get(1) + " " +
                params.get(2) + " " +
                params.get(3) + " " +
                params.get(4) + " " +
                subdir +
                params.get(5);
    }

    @Override
    public void updateState(State state) {
        File f = Paths.get(params.get(4).toString()).toFile();
        Path baseDir = Paths.get(params.get(5).toString());
        updateDfs(((HdfsState) state).dfs, f, baseDir);
    }

    public void updateDfs(HadoopFileSystem dfs, File f, Path baseDir) {
        if (f.isDirectory()) {
            String dirName = f.getName();
            dfs.createDir(baseDir.resolve(dirName).toString());
            for (File subFile : f.listFiles()) {
                updateDfs(dfs, subFile, baseDir.resolve(dirName));
            }
        } else {
            String fileName = f.getName();
            dfs.createFile(baseDir.resolve(fileName).toString());
        }
    }
}
