package org.zlab.upfuzz.hdfs.dfscommands;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;

public class RefreshNodesCommand extends DfsadminCommand {

    /**
     * Re-read the hosts and exclude files to update the set of Datanodes that are allowed to connect to the Namenode and those that should be decommissioned or recommissioned.
     */
    public RefreshNodesCommand(HdfsState hdfsState) {
        Parameter refreshNodesCmd = new CONSTANTSTRINGType("-refreshNodes")
                .generateRandomParameter(null, null);

        params.add(refreshNodesCmd);
    }

    @Override
    public void updateState(State state) {
    }
}
