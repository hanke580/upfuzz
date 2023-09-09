package org.zlab.upfuzz.hbase.DataManipulationCommands;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;
import org.zlab.upfuzz.utils.INTType;

public class GET extends HBaseCommand {
    /**
     * Example
     * get "uuid8780ad9e9e9f421b8724b83b09cc9eae", "row1", "JCIWaTQ:c1", "JCIWaTQ:c2"
     */
    public GET(HBaseState state) {
        Parameter tableName = chooseTable(state, this, null);
        this.params.add(tableName); // [0] table name

        Parameter rowKey = chooseRowKey(state, this, null);
        this.params.add(rowKey); // [1] row key

        // Num of columns to get
        Parameter numberOfColumns = new INTType(2, 3)
                .generateRandomParameter(state, this);
        this.params.add(numberOfColumns); // [1] column family name

        // column
        for (int i = 0; i < 2; i++) {
            Parameter columnFamilyName = chooseNotEmptyColumnFamily(state, this,
                    null);
            this.params.add(columnFamilyName); // [1] column family name
            Parameter column = chooseColumnName(state, this,
                    columnFamilyName.toString(), null);
            params.add(column); // [3] column2type
        }
    }

    @Override
    public String constructCommandString() {
        int cfNum = Integer.parseInt(params.get(2).toString());
        StringBuilder sb = new StringBuilder();
        sb.append("get ").append("'" + params.get(0) + "'" + ", ")
                .append("'" + params.get(1) + "'");
        if (cfNum > 0) {
            sb.append(", ");
            // for (int i = 0; i < cfNum; i++) {
            // String cfName = params.get(i).toString();
            // String columnName = params.get(i * 2 + 4).toString().split("
            // ")[0];
            // sb.append("'" + params.get(2) + ":" + columnName + "'");
            // }
        }
        return sb.toString();
    }

    @Override
    public void updateState(State state) {
    }
}
