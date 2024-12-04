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

public class Setfacl extends Dfs {

    private static List<String> opts = Arrays.asList("-b", "-k");

    public Setfacl(HdfsState state) {
        super(state.subdir);

        // Support: -setfacl [-R] {-b|-k} -m <path>

        // [-setfacl [-R] [{-b|-k} {-m|-x <acl_spec>} <path>] |[--set <acl_spec>
        // <path>]]

        // -setacl
        Parameter cmd = new CONSTANTSTRINGType("-setfacl")
                .generateRandomParameter(state, this);

        // [-R]
        Parameter hyphenR = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-R"), null)
                        .generateRandomParameter(state, this);

        // -b|-k
        Parameter optsParameter = Utilities.createInStringCollectionType(opts)
                .generateRandomParameter(state, this);

        // -m
        Parameter mParameter = new CONSTANTSTRINGType("-m")
                .generateRandomParameter(state, this);

        // <path>
        Parameter path = new HDFSRandomPathType()
                .generateRandomParameter(state, null);

        params.add(cmd);
        params.add(hyphenR);
        params.add(optsParameter);
        params.add(mParameter);
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
