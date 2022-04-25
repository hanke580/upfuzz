package org.zlab.upfuzz.hdfs.dfscommands;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.hdfs.HDFSParameterType.ConcatenateType;
import org.zlab.upfuzz.hdfs.HDFSParameterType.RandomHadoopPathType;
import org.zlab.upfuzz.hdfs.HDFSParameterType.RandomLocalPathType;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.utils.INTType;

public class MvCommand extends DfsCommands {

    /*
     * Moves files from source to destination. This command allows multiple sources as well in which case the destination needs to be a directory. Moving files across file systems is not permitted.
     */
    public MvCommand(HdfsState hdfsState) {
        Parameter mvcmd = new CONSTANTSTRINGType("-mv").generateRandomParameter(null, null);

        Parameter srcParameter = new RandomLocalPathType().generateRandomParameter(hdfsState, null);

        Parameter dstParameter = new RandomHadoopPathType().generateRandomParameter(hdfsState, null);

        params.add(mvcmd);
        params.add(srcParameter);
        params.add(dstParameter);
    }

    @Override
    public void updateState(State state) {
    }
}
