package org.zlab.upfuzz.hbase.dml;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;

public class GET_COUNTER extends HBaseCommand {
    /**
     * hbase(main):001:0> get_counter '<table_name>', '<row_key>', '<family:qualifier>'
     */
    public GET_COUNTER(HBaseState state) {
        super(state);
        Parameter tableName = chooseTable(state, this, null);
        this.params.add(tableName); // [0] table name

        Parameter rowKey = chooseRowKey(state, this, null);
        this.params.add(rowKey); // [1] row key

        Parameter columnFamilyName = chooseNotEmptyColumnFamily(state, this,
                null);
        this.params.add(columnFamilyName); // [2] column family name

        // FIXME: the target column should be increased using incr
        Parameter column = chooseColumnName(state, this,
                columnFamilyName.toString(), null);
        params.add(column); // [3] column2type
    }

    @Override
    public String constructCommandString() {
        // get_counter '<table_name>', '<row_key>', '<family:qualifier>'
        return "get_counter " + "'" + params.get(0) + "'" + ", "
                + "'" + params.get(1) + "'" + ", "
                + "'" + params.get(2) + ":"
                + params.get(3).toString().split(" ")[0] + "'";
    }

    @Override
    public void updateState(State state) {
    }
}
