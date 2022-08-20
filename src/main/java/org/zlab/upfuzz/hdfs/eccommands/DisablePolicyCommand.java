package org.zlab.upfuzz.hdfs.eccommands;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;

public class DisablePolicyCommand extends ErasureCodingCommand {

    public DisablePolicyCommand(HdfsState hdfsState) {
        Parameter disablePolicyCmd = new CONSTANTSTRINGType("-disablePolicy")
                .generateRandomParameter(null, null);

        Parameter policyOpt = new CONSTANTSTRINGType("-policy")
                .generateRandomParameter(null, null);

        Parameter policy = new ParameterType.InCollectionType(
                CONSTANTSTRINGType.instance,
                (s, c) -> (((ErasureCodingCommand) c).policies),
                null).generateRandomParameter(null, null);

        params.add(disablePolicyCmd);
        params.add(policyOpt);
        params.add(policy);
    }

    @Override
    public void updateState(State state) {
    }

}
