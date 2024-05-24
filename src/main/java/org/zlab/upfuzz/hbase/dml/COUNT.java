package org.zlab.upfuzz.hbase.dml;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;
import org.zlab.upfuzz.utils.*;

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

        Parameter cacheBlocks = new ParameterType.OptionalType(
                new ParameterType.InCollectionType(
                        CONSTANTSTRINGType.instance,
                        (s, c) -> Utilities
                                .strings2Parameters(
                                        CACHE_BLOCKS_TYPES),
                        null),
                null).generateRandomParameter(state, this);
        params.add(cacheBlocks);

//        Parameter filter = new ParameterType.OptionalType(new FILTERType(),
//                null).generateRandomParameter(state, this);
//        params.add(filter);
    }

    @Override
    public String constructCommandString() {
        // count 't1', INTERVAL => 10, CACHE => 1000
        String interval = params.get(1).toString().isEmpty() ? ""
                : ", INTERVAL => " + params.get(1);
        String cache = params.get(2).toString().isEmpty() ? ""
                : ", CACHE => " + params.get(2);
        String cacheBlocks = params.get(3).toString().isEmpty() ? ""
                : ", CACHE_BLOCKS => " + params.get(3);
        return "count '" + params.get(0) + "'" + interval + cache + cacheBlocks;
    }

    @Override
    public void updateState(State state) {

    }

    @Override
    public boolean mutate(State s) throws Exception {
        try {
            super.mutate(s);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
