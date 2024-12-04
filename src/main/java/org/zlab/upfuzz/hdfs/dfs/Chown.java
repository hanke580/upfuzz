package org.zlab.upfuzz.hdfs.dfs;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HDFSParameterType.HDFSDirPathType;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.utils.STRINGType;

public class Chown extends Dfs {

    public Chown(HdfsState state) {
        super(state.subdir);
        // [-chown [-R] [OWNER][:[GROUP]] PATH...]
        Parameter cmd = new CONSTANTSTRINGType("-chown")
                .generateRandomParameter(state, this);

        Parameter RParameter = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-R"), null)
                        .generateRandomParameter(state, this);

        Parameter ownerParameter = new ParameterType.NotEmpty(
                new STRINGType(10))
                        .generateRandomParameter(state, this);

        Parameter pathParameter = new HDFSDirPathType()
                .generateRandomParameter(state, null);

        params.add(cmd);
        params.add(RParameter);
        params.add(ownerParameter);
        params.add(pathParameter);
    }

    @Override
    public void updateState(State state) {
    }

    @Override
    public String constructCommandString() {
        return constructCommandStringWithDirSeparation(type);
    }
}
