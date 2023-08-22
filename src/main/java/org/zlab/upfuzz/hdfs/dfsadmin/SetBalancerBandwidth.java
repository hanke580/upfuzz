package org.zlab.upfuzz.hdfs.dfsadmin;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HDFSParameterType.HDFSDirPathType;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.utils.INTType;
import org.zlab.upfuzz.utils.LONGType;

public class SetBalancerBandwidth extends Dfsadmin {

    public SetBalancerBandwidth(HdfsState state) {
        super(state.subdir);

        Parameter cmd = new CONSTANTSTRINGType("-setBalancerBandwidth")
                .generateRandomParameter(null,
                        null);
        Parameter opt1 = new INTType(0, null).generateRandomParameter(null,
                null);
        Parameter dir = new HDFSDirPathType()
                .generateRandomParameter(state, null);

        params.add(cmd);
        params.add(opt1);
        params.add(dir);
    }

    @Override
    public String constructCommandString() {
        return constructCommandStringWithDirSeparation("dfsadmin");
    }

    @Override
    public void updateState(State state) {

    }
}