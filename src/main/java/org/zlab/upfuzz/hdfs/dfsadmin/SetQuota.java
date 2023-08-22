package org.zlab.upfuzz.hdfs.dfsadmin;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HDFSParameterType.HDFSDirPathType;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.utils.LONGType;

public class SetQuota extends Dfsadmin {

    public SetQuota(HdfsState state) {
        super(state.subdir);

        Parameter cmd = new CONSTANTSTRINGType("-setQuota")
                .generateRandomParameter(null,
                        null);
        Parameter opt1 = new LONGType(1L, null).generateRandomParameter(null,
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
