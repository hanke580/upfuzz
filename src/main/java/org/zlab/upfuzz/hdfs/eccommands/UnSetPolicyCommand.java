package org.zlab.upfuzz.hdfs.eccommands;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HDFSParameterType.RandomHadoopPathType;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;

public class UnSetPolicyCommand extends ErasureCodingCommand {

    public UnSetPolicyCommand(HdfsState hdfsState) {
        Parameter unsetPolicyCmd = new CONSTANTSTRINGType("-unsetPolicy")
                .generateRandomParameter(null, null);

        Parameter pathOpt = new CONSTANTSTRINGType("-path")
                .generateRandomParameter(null, null);

        Parameter path = new RandomHadoopPathType()
                .generateRandomParameter(hdfsState, null);

        params.add(unsetPolicyCmd);
        params.add(pathOpt);
        params.add(path);
    }

    @Override
    public void updateState(State state) {

    }
}
