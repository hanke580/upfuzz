package org.zlab.upfuzz.hdfs.dfsadmin;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.utils.Utilities;

import java.util.LinkedList;
import java.util.List;

public class RollingUpgrade extends Dfsadmin {

    public static List<String> options = new LinkedList<>();
    static {
        options.add("query");
        options.add("prepare");
        options.add("finalize");
    }

    public RollingUpgrade(HdfsState state) {
        super(state.subdir);

        Parameter cmd = new CONSTANTSTRINGType("-rollingUpgrade")
                .generateRandomParameter(null,
                        null);
        Parameter opt = new ParameterType.InCollectionType(
                CONSTANTSTRINGType.instance,
                (s, c) -> Utilities.strings2Parameters(
                        options),
                null).generateRandomParameter(null, null);

        params.add(cmd);
        params.add(opt);
    }

    @Override
    public void updateState(State state) {

    }
}
