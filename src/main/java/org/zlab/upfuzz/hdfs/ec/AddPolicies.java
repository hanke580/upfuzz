package org.zlab.upfuzz.hdfs.ec;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HDFSParameterType.HDFSDirPathType;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;

public class AddPolicies extends ErasureCoding {

    public AddPolicies(HdfsState state) {
        super(state.subdir);

        Parameter addPolicyCmd = new CONSTANTSTRINGType("-addPolicies")
                .generateRandomParameter(null, null);

        Parameter policyFileOpt = new CONSTANTSTRINGType("-policyFile")
                .generateRandomParameter(null, null);

        // TODO: This needs to be a new policy file!
        Parameter policyFile = new HDFSDirPathType()
                .generateRandomParameter(state, null);

        params.add(addPolicyCmd);
        params.add(policyFileOpt);
        params.add(policyFile);
    }

    @Override
    public String constructCommandString() {
        return "ec" + " " +
                params.get(0) + " " +
                params.get(1) + " " +
                subdir +
                params.get(2);
    }

    @Override
    public void updateState(State state) {

    }
}
