package org.zlab.upfuzz.hdfs.dfs;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HDFSParameterType.HDFSDirPathType;
import org.zlab.upfuzz.hdfs.HDFSParameterType.HDFSSnapshotPathType;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.utils.STRINGType;

import java.nio.file.Path;

public class DeleteSnapshot extends Dfs {
    // [-deleteSnapshot <snapshotDir> <snapshotName>]

    public DeleteSnapshot(HdfsState state) {
        super(state.subdir);

        Parameter cmd = new CONSTANTSTRINGType("-deleteSnapshot")
                .generateRandomParameter(state, null);

        Parameter snapshotPathParameter = new HDFSSnapshotPathType()
                .generateRandomParameter(state, null);

        params.add(cmd);
        params.add(snapshotPathParameter);
    }

    @Override
    public String constructCommandString() {
        assert params.size() == 2;
        Path p = Path.of(params.get(1).toString());
        // get filename
        String snapshotDir = p.getParent().toString() + "/";
        String snapshotName = p.getFileName().toString();

        return "dfs" + " " +
                params.get(0) + " " +
                subdir +
                snapshotDir + " " + snapshotName;
    }

    @Override
    public void updateState(State state) {
        assert params.size() == 2;
        Path p = Path.of(params.get(1).toString());
        // get filename
        String snapshotDir = p.getParent().toString() + "/";
        String snapshotName = p.getFileName().toString();

        ((HdfsState) state).dfs.rmSnapShotFile(snapshotDir, snapshotName);
    }
}
