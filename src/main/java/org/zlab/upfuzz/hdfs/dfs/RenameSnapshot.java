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

public class RenameSnapshot extends Dfs {
    // [-renameSnapshot <snapshotDir> <oldName> <newName>]

    public RenameSnapshot(HdfsState state) {
        super(state.subdir);

        Parameter cmd = new CONSTANTSTRINGType("-renameSnapshot")
                .generateRandomParameter(state, null);

        Parameter snapshotPathParameter = new HDFSSnapshotPathType()
                .generateRandomParameter(state, null);

        Parameter name = new ParameterType.OptionalType(new STRINGType(20),
                null)
                        .generateRandomParameter(state, null);

        params.add(cmd);
        params.add(snapshotPathParameter);
        params.add(name);
    }

    @Override
    public String constructCommandString() {
        Path p = Path.of(params.get(1).toString());
        // get filename
        String snapshotDir = p.getParent().toString() + "/";
        String snapshotName = p.getFileName().toString();

        return "dfs" + " " +
                params.get(0) + " " +
                subdir +
                snapshotDir + " " +
                snapshotName + " " +
                params.get(2);
    }

    @Override
    public void updateState(State state) {
        Path p = Path.of(params.get(1).toString());
        // get filename
        String snapshotDir = p.getParent().toString() + "/";
        String snapshotName = p.getFileName().toString();

        ((HdfsState) state).dfs.rmSnapShotFile(snapshotDir, snapshotName);
        ((HdfsState) state).dfs.createSnapShotFile(snapshotDir,
                params.get(2).toString());
    }
}
