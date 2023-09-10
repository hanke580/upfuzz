package org.zlab.upfuzz.hbase.dml;

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
        super(state);
        Parameter tableName = chooseTable(state, this, null);
        this.params.add(tableName); // [0] table name

        Parameter rowKey = chooseRowKey(state, this, null);
        this.params.add(rowKey); // [1] row key

        // Num of columns to get
        Parameter numberOfColumns = new INTType(0, 3)
                .generateRandomParameter(state, this);
        this.params.add(numberOfColumns); // [2] column family name

        // column
        for (int i = 0; i < 2; i++) {
            // FIXME: The column family might not be corresponding to the row
            // key. Fix this will increase the validity.
            Parameter columnFamilyName = chooseNotEmptyColumnFamily(state, this,
                    null);
            this.params.add(columnFamilyName); // [3] column family name

            Parameter column = chooseColumnName(state, this,
                    columnFamilyName.toString(), null);
            params.add(column); // [4] column2type

            Parameter version = new INTType(1, 5)
                    .generateRandomParameter(state, this);
            params.add(version); // [5] version
        }
    }

    @Override
    public String constructCommandString() {
        int cfNum = Integer.parseInt(params.get(2).toString());
        StringBuilder sb = new StringBuilder();
        sb.append("get ").append("'" + params.get(0) + "'" + ", ")
                .append("'" + params.get(1) + "'");
        if (cfNum > 0) {
            // Iterate columnFamilyName, column, version
            for (int i = 0; i < cfNum; i += 3) {
                sb.append(", ");
                Parameter cfName = params.get(3 + i);
                Parameter column = params.get(3 + i + 1);
                Parameter version = params.get(3 + i + 2);

                sb.append(String.format("{COLUMN => '%s:%s', VERSIONS => %d}",
                        cfName, column.toString().split(" ")[0],
                        Integer.parseInt(version.toString())));
                //
                // sb.append("'" + params.get(3 + i * 3) + "'")
                // .append(":").append("'" + params.get(4 + i * 3) + "'")
                // .append(", ").append(params.get(5 + i * 3));
            }
        }
        return sb.toString();
    }

    @Override
    public void updateState(State state) {
    }
}
