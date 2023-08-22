package org.zlab.upfuzz.hdfs.dfs;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HDFSParameterType.HDFSFilePathType;
import org.zlab.upfuzz.hdfs.HDFSParameterType.RandomLocalPathType;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.utils.INTType;

public class Chmod extends Dfs {

    public Chmod(HdfsState state) {
        super(state.subdir);

        Parameter cmd = new CONSTANTSTRINGType("-chmod")
                .generateRandomParameter(state, null);

        Parameter opt = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-R"), null)
                        .generateRandomParameter(null, null);

        // Mode
        Parameter permission1 = new INTType(0, 8)
                .generateRandomParameter(null, null);
        Parameter permission2 = new INTType(0, 8)
                .generateRandomParameter(null, null);
        Parameter permission3 = new INTType(0, 8)
                .generateRandomParameter(null, null);
        Parameter destParameter = new HDFSFilePathType()
                .generateRandomParameter(state, null);

        params.add(cmd);
        params.add(opt);
        params.add(permission1);
        params.add(permission2);
        params.add(permission3);
        params.add(destParameter);
    }

    @Override
    public String constructCommandString() {
        return "dfs" + " " +
                params.get(0) + " " +
                params.get(1) +
                params.get(2) +
                params.get(3) + " " +
                params.get(4) + " " +
                subdir +
                params.get(5);
    }

    @Override
    public void updateState(State state) {
    }
}