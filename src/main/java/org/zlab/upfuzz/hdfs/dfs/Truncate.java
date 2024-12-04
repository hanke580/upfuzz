package org.zlab.upfuzz.hdfs.dfs;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HDFSParameterType.HDFSFilePathType;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.utils.Utilities;

import java.util.Arrays;
import java.util.List;

public class Truncate extends Dfs {

    // [-truncate [-w] <length> <path> ...]
    public static List<String> length = Arrays.asList("0", "1", "2", "16", "32",
            "64", "128", "512");

    public Truncate(HdfsState hdfsState) {
        super(hdfsState.subdir);

        Parameter cmd = new CONSTANTSTRINGType("-truncate")
                .generateRandomParameter(hdfsState, null);

        Parameter wParameter = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-w"), null)
                        .generateRandomParameter(hdfsState, this);

        Parameter lengthParameter = Utilities
                .createInStringCollectionType(length)
                .generateRandomParameter(hdfsState, this);

        Parameter pathParameter = new HDFSFilePathType()
                .generateRandomParameter(hdfsState, null);

        params.add(cmd);
        params.add(wParameter);
        params.add(lengthParameter);
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
