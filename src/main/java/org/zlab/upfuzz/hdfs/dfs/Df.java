package org.zlab.upfuzz.hdfs.dfs;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HDFSParameterType.HDFSDirPathType;
import org.zlab.upfuzz.hdfs.HDFSParameterType.HDFSFilePathType;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;

public class Df extends Dfs {

    /*
     * Moves files from source to destination. This command allows multiple
     * sources as well in which case the destination needs to be a directory.
     * Moving files across file systems is not permitted.
     */
    public Df(HdfsState hdfsState) {
        super(hdfsState.subdir);

        Parameter cmd = new CONSTANTSTRINGType("-df")
                .generateRandomParameter(hdfsState, null);

        Parameter opt = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-h"), null)
                        .generateRandomParameter(hdfsState, null);

        Parameter param = new HDFSDirPathType()
                .generateRandomParameter(hdfsState, null);

        params.add(cmd);
        params.add(opt);
        params.add(param);
    }

    @Override
    public String constructCommandString() {
        return constructCommandStringWithDirSeparation("dfs");
    }

    @Override
    public void updateState(State state) {
    }
}
