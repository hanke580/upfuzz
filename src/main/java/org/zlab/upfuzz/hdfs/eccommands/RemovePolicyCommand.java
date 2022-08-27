package org.zlab.upfuzz.hdfs.eccommands;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.utils.Utilities;

public class RemovePolicyCommand extends ErasureCodingCommand {

    public RemovePolicyCommand(HdfsState hdfsState) {
        Parameter removePolicyCmd = new CONSTANTSTRINGType("-removePolicy")
                .generateRandomParameter(null, null);

        Parameter policyOpt = new CONSTANTSTRINGType("-policy")
                .generateRandomParameter(null, null);

        Parameter policy = new ParameterType.InCollectionType(
                CONSTANTSTRINGType.instance,
                (s, c) -> Utilities.strings2Parameters(
                        (((ErasureCodingCommand) c).policies)),
                null).generateRandomParameter(null, null);

        params.add(removePolicyCmd);
        params.add(policyOpt);
        params.add(policy);
    }

    @Override
    public void updateState(State state) {
    }

}
