package org.zlab.upfuzz.hdfs.dfscommands;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HDFSParameterType.*;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.utils.INTType;

public class CpCommand extends DfsCommand {

    /*
     * Copy files from source to destination. This command allows multiple
     * sources as well in which case the destination must be a directory.
     */
    public CpCommand(HdfsState state) {
        super(state.subdir);

        Parameter cpcmd = new CONSTANTSTRINGType("-cp")
                .generateRandomParameter(null, null);

        // -f : Overwrites the destination if it already exists.
        Parameter fOption = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-f"), null)
                        .generateRandomParameter(null, null);

        // -p : Preserve file attributes [topx] (timestamps, ownership,
        // permission, ACL, XAttr). If -p is specified with no arg, then
        // preserves timestamps, ownership, permission. If -pa is specified,
        // then preserves permission also because ACL is a super-set of
        // permission. Determination of whether raw namespace extended
        // attributes are preserved is independent of the -p flag.
        Parameter pOption = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-p"), null)
                        .generateRandomParameter(null, null);

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

        Parameter srcParameter = new HDFSRandomPathType()
                .generateRandomParameter(state, null);

        Parameter dstParameter = new HDFSDirPathType()
                .generateRandomParameter(state, null);

        params.add(cpcmd);
        params.add(fOption);
        params.add(pOption);
        params.add(dOption);
        params.add(threadOption);
        params.add(threadQueueOption);
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
                params.get(5) + " " +
                subdir +
                params.get(6) + " " +
                subdir +
                params.get(7);
    }

    @Override
    public void updateState(State state) {
        HdfsState hdfsState = (HdfsState) state;
        for (String dir : hdfsState.dfs.getDirs(params.get(6).toString())) {
            String newDir = dir.replace(params.get(6).toString(),
                    params.get(7).toString());
            hdfsState.dfs.createDir(newDir);
        }
        for (String file : hdfsState.dfs.getFiles(params.get(6).toString())) {
            String newFile = file.replace(params.get(6).toString(),
                    params.get(7).toString());
            hdfsState.dfs.createFile(newFile);
        }
    }
}
