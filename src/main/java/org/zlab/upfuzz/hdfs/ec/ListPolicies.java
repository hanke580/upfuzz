package org.zlab.upfuzz.hdfs.ec;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;

public class ListPolicies extends ErasureCoding {

    public ListPolicies(HdfsState state) {
        super(state.subdir);

        Parameter listPolicyCmd = new CONSTANTSTRINGType("-listPolicies")
                .generateRandomParameter(null, null);

        params.add(listPolicyCmd);
    }

    @Override
    public void updateState(State state) {

    }
}
