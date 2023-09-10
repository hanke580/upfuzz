package org.zlab.upfuzz.hbase.snapshot;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseColumnFamily;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;
import org.zlab.upfuzz.hbase.Utils;
import org.zlab.upfuzz.utils.STRINGType;

import java.util.Map;

public class SNAPSHOT extends HBaseCommand {

    public SNAPSHOT(HBaseState state) {
        super(state);
        Parameter tableName = chooseTable(state, this);
        this.params.add(tableName); // 0 tableName

        Parameter snapshotName = new ParameterType.NotEmpty(new STRINGType(10))
                .generateRandomParameter(state, this);
        this.params.add(snapshotName); // 1 snapshotName
    }

    @Override
    public String constructCommandString() {
        // snapshot 'mytable', 'mysnapshot'
        return "snapshot " + "'" + params.get(0) + "'" + ", " + "'"
                + params.get(1) + "'";
    }

    @Override
    public void updateState(State state) {
        String oriTableName = params.get(0).toString();
        Map<String, HBaseColumnFamily> oriTable = ((HBaseState) state).table2families
                .get(oriTableName);
        Map<String, HBaseColumnFamily> newTable = Utils.deepCopyTable(oriTable);

        String snapshotName = params.get(1).toString();
        ((HBaseState) state).addSnapshot(snapshotName, newTable);
    }
}
