package org.zlab.upfuzz.hdfs.dfs;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HDFSParameterType.*;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;

public class Mv extends Dfs {

    /*
     * Moves files from source to destination. This command allows multiple
     * sources as well in which case the destination needs to be a directory.
     * Moving files across file systems is not permitted.
     */
    public Mv(HdfsState state) {
        super(state.subdir);

        Parameter mvcmd = new CONSTANTSTRINGType("-mv")
                .generateRandomParameter(null, null);

        Parameter srcParameter = new HDFSRandomPathType()
                .generateRandomParameter(state, null);

        Parameter dstParameter = new HDFSDirPathType()
                .generateRandomParameter(state, null);

        params.add(mvcmd);
        params.add(srcParameter);
        params.add(dstParameter);
    }

    @Override
    public String constructCommandString() {
        return "dfs" + " " +
                params.get(0).toString() + " " +
                subdir +
                params.get(1).toString() + " " +
                subdir +
                params.get(2).toString();
    }

    @Override
    public void updateState(State state) {
        HdfsState hdfsState = (HdfsState) state;

        // move to new path
        for (String dir : hdfsState.dfs.getDirs(params.get(1).toString())) {
            String newDir = dir.replace(params.get(1).toString(),
                    params.get(2).toString());
            hdfsState.dfs.createDir(newDir);
        }
        for (String file : hdfsState.dfs.getFiles(params.get(1).toString())) {
            String newFile = file.replace(params.get(1).toString(),
                    params.get(2).toString());
            hdfsState.dfs.createFile(newFile);
        }

        // remove the original path
        hdfsState.dfs.removeDir(params.get(1).toString());
        hdfsState.dfs.removeFile(params.get(1).toString());

    }
}
