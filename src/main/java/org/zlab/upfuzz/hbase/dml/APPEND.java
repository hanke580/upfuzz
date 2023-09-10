package org.zlab.upfuzz.hbase.dml;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseState;

public class APPEND extends PUT_MODIFY {

    // append '<table_name>', '<row_key>', '<family:qualifier>', '<value>'
    public APPEND(HBaseState state) {
        super(state);
    }

    @Override
    public String constructCommandString() {
        Parameter tableName = params.get(0);
        Parameter columnFamilyName = params.get(1);
        Parameter rowKey = params.get(2);
        Parameter columnName = params.get(3);
        String colNameStr = columnName.toString();
        colNameStr = colNameStr.substring(0, colNameStr.indexOf(" "));
        Parameter insertValues = params.get(4);
        String valueStr = insertValues.toString();

        return "append "
                + "'" + tableName.toString() + "', "
                + "'" + rowKey.toString() + "', "
                + "'" + columnFamilyName.toString() + ":"
                + colNameStr + "', "
                + valueStr;
    }

    @Override
    public void updateState(State state) {
    }
}
