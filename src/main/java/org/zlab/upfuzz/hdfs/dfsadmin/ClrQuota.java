package org.zlab.upfuzz.hdfs.dfsadmin;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HDFSParameterType.HDFSDirPathType;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;

public class ClrQuota extends Dfsadmin {

    public ClrQuota(HdfsState state) {
        super(state.subdir);

        Parameter cmd = new CONSTANTSTRINGType("-clrQuota")
                .generateRandomParameter(null,
                        null);
        Parameter dir = new HDFSDirPathType()
                .generateRandomParameter(state, null);

        params.add(cmd);
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
