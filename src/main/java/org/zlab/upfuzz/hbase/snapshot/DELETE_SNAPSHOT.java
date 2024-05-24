package org.zlab.upfuzz.hbase.snapshot;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;

public class DELETE_SNAPSHOT extends HBaseCommand {

    boolean validConstruction;

    public DELETE_SNAPSHOT(HBaseState state) {
        super(state);
        validConstruction = true;
        try {
            Parameter snapshotName = chooseSnapshot(state, this);
            this.params.add(snapshotName); // 0 snapshotName
        } catch (Exception e) {
            validConstruction = false;
        }
    }

    @Override
    public String constructCommandString() {
        if (!validConstruction)
            return "delete_snapshot ";
        // clone_snapshot 'snapshot_name', 'new_table_name'
        return "delete_snapshot " + "'" + params.get(0) + "'";
    }

    @Override
    public void updateState(State state) {
        if (!validConstruction)
            return;
        String snapshotName = params.get(0).toString();
        String newTableName = params.get(1).toString();

        if (!((HBaseState) state).snapshots.containsKey(snapshotName)) {
            return;
        }
        ((HBaseState) state).addTable(newTableName,
                ((HBaseState) state).snapshots.get(snapshotName));
    }
}
