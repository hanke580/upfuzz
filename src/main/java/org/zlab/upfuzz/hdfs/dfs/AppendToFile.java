package org.zlab.upfuzz.hdfs.dfs;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HDFSParameterType.HDFSFilePathType;
import org.zlab.upfuzz.hdfs.HDFSParameterType.RandomLocalPathType;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;

public class AppendToFile extends Dfs {

    public AppendToFile(HdfsState state) {
        super(state.subdir);

        Parameter catCmd = new CONSTANTSTRINGType("-appendToFile")
                .generateRandomParameter(state, null);

        Parameter srcParameter = new RandomLocalPathType()
                .generateRandomParameter(state, null);
        Parameter destParameter = new HDFSFilePathType()
                .generateRandomParameter(state, null);

        params.add(catCmd);
        params.add(srcParameter);
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