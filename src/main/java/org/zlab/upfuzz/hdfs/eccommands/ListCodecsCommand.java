package org.zlab.upfuzz.hdfs.eccommands;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HDFSParameterType.RandomHadoopPathType;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;

public class ListCodecsCommand extends ErasureCodingCommand {

    public ListCodecsCommand(HdfsState state) {
        super(state.subdir);

        Parameter listCodecsCmd = new CONSTANTSTRINGType("-listCodecs")
                .generateRandomParameter(null, null);

        params.add(listCodecsCmd);
    }

    @Override
    public void updateState(State state) {

    }
}
