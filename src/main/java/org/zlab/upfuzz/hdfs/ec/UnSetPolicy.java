package org.zlab.upfuzz.hdfs.ec;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HDFSParameterType.HDFSDirPathType;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;

public class UnSetPolicy extends ErasureCoding {

    public UnSetPolicy(HdfsState state) {
        super(state.subdir);

        Parameter unsetPolicyCmd = new CONSTANTSTRINGType("-unsetPolicy")
                .generateRandomParameter(null, null);

        Parameter pathOpt = new CONSTANTSTRINGType("-path")
                .generateRandomParameter(null, null);

        Parameter path = new HDFSDirPathType()
                .generateRandomParameter(state, null);

        params.add(unsetPolicyCmd);
        params.add(pathOpt);
        params.add(path);
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
