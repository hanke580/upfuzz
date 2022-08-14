package org.zlab.upfuzz.hdfs.eccommands;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HDFSParameterType.RandomHadoopPathType;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;

public class GetPolicyCommand extends ErasureCodingCommand {

    public GetPolicyCommand(HdfsState hdfsState) {
        Parameter getPolicyCmd = new CONSTANTSTRINGType("-getPolicy")
                .generateRandomParameter(null, null);

        Parameter pathOpt = new CONSTANTSTRINGType("-path")
                .generateRandomParameter(null, null);

        Parameter path = new RandomHadoopPathType()
                .generateRandomParameter(hdfsState, null);

        params.add(getPolicyCmd);
        params.add(pathOpt);
        params.add(path);
    }

    @Override
    public void updateState(State state) {

    }
}
