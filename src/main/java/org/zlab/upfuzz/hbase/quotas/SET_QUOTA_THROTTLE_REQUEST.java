package org.zlab.upfuzz.hbase.quotas;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.utils.INTType;
import org.zlab.upfuzz.utils.Utilities;

public class SET_QUOTA_THROTTLE_REQUEST
        extends org.zlab.upfuzz.hbase.HBaseCommand {

    public SET_QUOTA_THROTTLE_REQUEST(HBaseState state) {
        super(state);

        Parameter tableName = chooseTable(state, this);
        this.params.add(tableName); // 1 tableName

        Parameter limit = new INTType(10, 5000).generateRandomParameter(state,
                this);
        this.params.add(limit); // 2 limit
    }

    @Override
    public String constructCommandString() {
        // hbase> set_quota TYPE => SPACE, TABLE => 'my_table', LIMIT => '10G'
        return String.format(
                "set_quota TYPE => THROTTLE, THROTTLE_TYPE => REQUEST, TABLE => '%s', LIMIT => '%sreq/sec'",
                params.get(0), params.get(1));
    }

    @Override
    public void updateState(State state) {

    }
}
