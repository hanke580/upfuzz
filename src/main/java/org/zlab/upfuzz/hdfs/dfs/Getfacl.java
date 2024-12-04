package org.zlab.upfuzz.hdfs.dfs;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HDFSParameterType.HDFSDirPathType;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;

public class Getfacl extends Dfs {

    public Getfacl(HdfsState state) {
        super(state.subdir);
        // [-getfacl [-R] <path>]

        Parameter cmd = new CONSTANTSTRINGType("-getfacl")
                .generateRandomParameter(state, this);

        Parameter RParameter = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-R"), null)
                        .generateRandomParameter(state, this);

        Parameter pathParameter = new HDFSDirPathType()
                .generateRandomParameter(state, null);

        params.add(cmd);
        params.add(RParameter);
        params.add(pathParameter);
    }

    @Override
    public String constructCommandString() {
        return constructCommandStringWithDirSeparation(type);
    }

    @Override
    public void updateState(State state) {
    }
}
