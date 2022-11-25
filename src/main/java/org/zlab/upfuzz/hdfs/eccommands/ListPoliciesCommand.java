package org.zlab.upfuzz.hdfs.eccommands;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HDFSParameterType.RandomHadoopPathType;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;

public class ListPoliciesCommand extends ErasureCodingCommand {

    public ListPoliciesCommand(HdfsState state) {
        super(state.subdir);

        Parameter listPolicyCmd = new CONSTANTSTRINGType("-listPolicies")
                .generateRandomParameter(null, null);

        params.add(listPolicyCmd);
    }

    @Override
    public void updateState(State state) {

    }
}
