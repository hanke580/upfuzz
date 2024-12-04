package org.zlab.upfuzz.hdfs.dfsadmin;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HDFSParameterType.HDFSDirPathType;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;

/**
 * Store metadata in a file under log dir in local FS system
 */
public class DisallowSnapshot extends Dfsadmin {

    public DisallowSnapshot(HdfsState state) {
        super(state.subdir);

        Parameter cmd = new CONSTANTSTRINGType("-disallowSnapshot")
                .generateRandomParameter(null,
                        null);

        // filename: should be a random string...
        Parameter snapshotPath = new HDFSDirPathType()
                .generateRandomParameter(state, null);

        params.add(cmd);
        params.add(snapshotPath);
    }

    @Override
    public String constructCommandString() {
        return constructCommandStringWithDirSeparation("dfsadmin");
    }

    @Override
    public void updateState(State state) {
    }
}