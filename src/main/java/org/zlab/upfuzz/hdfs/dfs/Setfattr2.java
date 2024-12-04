package org.zlab.upfuzz.hdfs.dfs;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HDFSParameterType.HDFSRandomPathType;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.utils.Utilities;

import java.util.Arrays;
import java.util.List;

public class Setfattr2 extends Dfs {
    // hdfs dfs -setfattr {-n name [-v value] | -x name} <path>
    // hdfs dfs -setfattr -x name <path>

    public Setfattr2(HdfsState state) {
        super(state.subdir);

        // -setacl
        Parameter cmd = new CONSTANTSTRINGType("-setfattr")
                .generateRandomParameter(state, this);

        // -x
        Parameter xParameter = new CONSTANTSTRINGType("-x")
                .generateRandomParameter(state, this);

        // att name
        Parameter optsParameter = Utilities.createInStringCollectionType(opts)
                .generateRandomParameter(state, this);

        // <path>
        Parameter path = new HDFSRandomPathType()
                .generateRandomParameter(state, null);

        params.add(cmd);
        params.add(xParameter);
        params.add(optsParameter);
        params.add(path);
    }

    @Override
    public void updateState(State state) {
    }

    @Override
    public String constructCommandString() {
        return constructCommandStringWithDirSeparation(type);
    }
}
