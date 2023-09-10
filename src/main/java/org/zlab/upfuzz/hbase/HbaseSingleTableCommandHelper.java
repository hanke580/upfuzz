package org.zlab.upfuzz.hbase;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;

public class HbaseSingleTableCommandHelper extends HBaseCommand {
    String command;

    public HbaseSingleTableCommandHelper(HBaseState state, String command) {
        super(state);
        // choose an existing table
        this.command = command;

        Parameter tableName = chooseTable(state, this);
        this.params.add(tableName); // 0 tableName
    }

    @Override
    public String constructCommandString() {
        return command + " " + "'" + params.get(0) + "'";
    }

    @Override
    public void updateState(State state) {

    }
}
