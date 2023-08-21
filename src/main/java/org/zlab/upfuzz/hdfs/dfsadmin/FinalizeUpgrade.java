package org.zlab.upfuzz.hdfs.dfsadmin;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;

public class FinalizeUpgrade extends Dfsadmin {

    public FinalizeUpgrade(HdfsState state) {
        super(state.subdir);

        Parameter refreshNodesCmd = new CONSTANTSTRINGType("-finalizeUpgrade")
                .generateRandomParameter(null, null);

        params.add(refreshNodesCmd);
    }

    @Override
    public void updateState(State state) {
    }
}
