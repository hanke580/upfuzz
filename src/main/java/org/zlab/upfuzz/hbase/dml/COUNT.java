package org.zlab.upfuzz.hbase.dml;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;
import org.zlab.upfuzz.utils.INTType;

public class COUNT extends HBaseCommand {
    public COUNT(HBaseState state) {
        super(state);
        Parameter tableName = chooseTable(state, this, null);
        this.params.add(tableName); // 0 tableName

        // interval
        Parameter interval = new ParameterType.OptionalType(
                new INTType(10, 500), null)
                        .generateRandomParameter(state, this);
        params.add(interval);

        Parameter cache = new ParameterType.OptionalType(new INTType(100, 1000),
                null)
                        .generateRandomParameter(state, this);
        params.add(cache);

        // TODO: Add Filter
    }

    @Override
    public String constructCommandString() {
        // count 't1', INTERVAL => 10, CACHE => 1000
        String interval = params.get(1).toString().isEmpty() ? ""
                : ", INTERVAL => " + params.get(1);
        String cache = params.get(2).toString().isEmpty() ? ""
                : ", CACHE => " + params.get(2);
        return "count '" + params.get(0) + "'" + interval + cache;
    }

    @Override
    public void updateState(State state) {
    }
}
