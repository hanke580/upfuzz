package org.zlab.upfuzz.hdfs.dfsadmin;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;

public class RefreshServiceAcl extends Dfsadmin {

    public RefreshServiceAcl(HdfsState state) {
        super(state.subdir);

        Parameter refreshNodesCmd = new CONSTANTSTRINGType("-refreshServiceAcl")
                .generateRandomParameter(null, null);

        params.add(refreshNodesCmd);
    }

    @Override
    public void updateState(State state) {
    }
}
