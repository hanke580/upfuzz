package org.zlab.upfuzz.hdfs.dfs;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HDFSParameterType.HDFSDirPathType;
import org.zlab.upfuzz.hdfs.HDFSParameterType.HDFSRandomPathType;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.utils.Utilities;

public class Stat extends Dfs {

    public Stat(HdfsState state) {
        super(state.subdir);
        // [-getfacl [-R] <path>]

        Parameter cmd = new CONSTANTSTRINGType("-stat")
                .generateRandomParameter(state, this);

        Parameter formatParameter = Utilities
                .createInStringCollectionType(formatOptions)
                .generateRandomParameter(state, this);

        Parameter pathParameter = new HDFSRandomPathType()
                .generateRandomParameter(state, null);

        params.add(cmd);
        params.add(formatParameter);
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
