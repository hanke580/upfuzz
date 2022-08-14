package org.zlab.upfuzz.hdfs.eccommands;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;

public class EnablePolicyCommand extends ErasureCodingCommand {

    public EnablePolicyCommand(HdfsState hdfsState) {
        Parameter enablePolicyCmd = new CONSTANTSTRINGType("-enablePolicy")
                .generateRandomParameter(null, null);

        Parameter policyOpt = new CONSTANTSTRINGType("-policy")
                .generateRandomParameter(null, null);

        Parameter policy = new ParameterType.InCollectionType(
                CONSTANTSTRINGType.instance,
                (s, c) -> (((ErasureCodingCommand) c).policies),
                null).generateRandomParameter(null, null);

        params.add(enablePolicyCmd);
        params.add(policyOpt);
        params.add(policy);
    }

    @Override
    public void updateState(State state) {
    }

}
