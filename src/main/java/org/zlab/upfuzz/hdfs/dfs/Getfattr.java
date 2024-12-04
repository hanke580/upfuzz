package org.zlab.upfuzz.hdfs.dfs;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HDFSParameterType.HDFSDirPathType;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;

public class Getfattr extends Dfs {

    public Getfattr(HdfsState state) {
        super(state.subdir);
        // [-getfacl [-R] <path>]

        Parameter cmd = new CONSTANTSTRINGType("-getfattr")
                .generateRandomParameter(state, this);

        Parameter hyphenR = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-R"), null)
                        .generateRandomParameter(state, this);

        Parameter dParameter = new CONSTANTSTRINGType("-d")
                .generateRandomParameter(state, this);

        Parameter eParameter = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-e hex"), null)
                        .generateRandomParameter(state, this);

        Parameter pathParameter = new HDFSDirPathType()
                .generateRandomParameter(state, null);

        params.add(cmd);
        params.add(hyphenR);
        params.add(dParameter);
        params.add(eParameter);
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
