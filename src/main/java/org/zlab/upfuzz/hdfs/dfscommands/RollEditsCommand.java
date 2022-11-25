package org.zlab.upfuzz.hdfs.dfscommands;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;

public class RollEditsCommand extends DfsadminCommand {

    /**
     * Rolls the edit log on the active NameNode.
     */
    public RollEditsCommand(HdfsState hdfsState) {
        super(hdfsState.subdir);

        Parameter rollEditsCmd = new CONSTANTSTRINGType("-rollEdits")
                .generateRandomParameter(null, null);

        params.add(rollEditsCmd);
    }

    @Override
    public void updateState(State state) {
    }
}
