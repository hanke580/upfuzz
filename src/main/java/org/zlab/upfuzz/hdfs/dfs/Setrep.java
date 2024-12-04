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

public class Setrep extends Dfs {
    // [-setrep [-R] [-w] <rep> <path> ...]

    public static List<String> rep = Arrays.asList("0", "1", "2");

    public Setrep(HdfsState state) {
        super(state.subdir);

        // -setacl
        Parameter cmd = new CONSTANTSTRINGType("-setrep")
                .generateRandomParameter(state, this);

        Parameter p1 = Utilities.createOptionalString("-R")
                .generateRandomParameter(state, this);

        Parameter p2 = Utilities.createOptionalString("-w")
                .generateRandomParameter(state, this);

        // att name
        Parameter p3 = Utilities.createInStringCollectionType(rep)
                .generateRandomParameter(state, this);

        // <path>
        Parameter path = new HDFSRandomPathType()
                .generateRandomParameter(state, null);

        params.add(cmd);
        params.add(p1);
        params.add(p2);
        params.add(p3);
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
