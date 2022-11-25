package org.zlab.upfuzz.hdfs.dfscommands;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HDFSParameterType.ConcatenateType;
import org.zlab.upfuzz.hdfs.HDFSParameterType.RandomHadoopPathType;
import org.zlab.upfuzz.hdfs.HDFSParameterType.RandomLocalPathType;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.utils.INTType;

public class MvCommand extends DfsCommand {

    /*
     * Moves files from source to destination. This command allows multiple
     * sources as well in which case the destination needs to be a directory.
     * Moving files across file systems is not permitted.
     */
    public MvCommand(HdfsState state) {
        super(state.subdir);

        Parameter mvcmd = new CONSTANTSTRINGType("-mv")
                .generateRandomParameter(null, null);

        Parameter srcParameter = new RandomLocalPathType()
                .generateRandomParameter(state, null);

        Parameter dstParameter = new RandomHadoopPathType()
                .generateRandomParameter(state, null);

        params.add(mvcmd);
        params.add(srcParameter);
        params.add(dstParameter);
    }

    @Override
    public String constructCommandString() {
        return "dfs" + " " +
                params.get(0) + " " +
                subdir +
                params.get(1) + " " +
                subdir +
                params.get(2);
    }

    @Override
    public void updateState(State state) {
    }
}
