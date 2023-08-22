package org.zlab.upfuzz.hdfs.dfs;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HDFSParameterType.HDFSFilePathType;
import org.zlab.upfuzz.hdfs.HDFSParameterType.RandomLocalPathType;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;

public class Chgrp extends Dfs {

    /**
     * This command should be used together with "group add" inside docker
     */
    public Chgrp(HdfsState state) {
        super(state.subdir);

        Parameter catCmd = new CONSTANTSTRINGType("-chgrp")
                .generateRandomParameter(state, null);

        Parameter opt = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-R"), null)
                        .generateRandomParameter(null, null);

        Parameter destParameter = new HDFSFilePathType()
                .generateRandomParameter(state, null);

        params.add(catCmd);
        params.add(opt);
        params.add(destParameter);
    }

    @Override
    public String constructCommandString() {
        return constructCommandStringWithDirSeparation("dfs");
    }

    @Override
    public void updateState(State state) {
    }
}