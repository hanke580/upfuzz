package org.zlab.upfuzz.hdfs.dfsadmin;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;

public class RefreshNodes extends Dfsadmin {

    /**
     * Re-read the hosts and exclude files to update the set of Datanodes that
     * are allowed to connect to the Namenode and those that should be
     * decommissioned or recommissioned.
     */
    public RefreshNodes(HdfsState state) {
        super(state.subdir);

        Parameter refreshNodesCmd = new CONSTANTSTRINGType("-refreshNodes")
                .generateRandomParameter(null, null);

        params.add(refreshNodesCmd);
    }

    @Override
    public void updateState(State state) {
    }
}
