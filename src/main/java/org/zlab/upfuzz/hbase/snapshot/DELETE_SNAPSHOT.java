package org.zlab.upfuzz.hbase.snapshot;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;

public class DELETE_SNAPSHOT extends HBaseCommand {

    public DELETE_SNAPSHOT(HBaseState state) {
        super(state);
        Parameter snapshotName = chooseSnapshot(state, this);
        this.params.add(snapshotName); // 0 snapshotName
    }

    @Override
    public String constructCommandString() {
        // clone_snapshot 'snapshot_name', 'new_table_name'
        return "delete_snapshot " + "'" + params.get(0) + "'";
    }

    @Override
    public void updateState(State state) {
        String snapshotName = params.get(0).toString();
        String newTableName = params.get(1).toString();
        ((HBaseState) state).addTable(newTableName,
                ((HBaseState) state).snapshots.get(snapshotName));
    }
}
