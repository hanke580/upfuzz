package org.zlab.upfuzz.hbase.dml;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;
import org.zlab.upfuzz.utils.Pair;

import java.util.ArrayList;
import java.util.List;

public class PUT_MODIFY extends HBaseCommand {
    public PUT_MODIFY(HBaseState state) {
        super(state);
        Parameter tableName = chooseTable(state, this, null);
        this.params.add(tableName); // [0] table name

        Parameter columnFamilyName = chooseNotNullColumnFamily(state, this,
                null);
        this.params.add(columnFamilyName); // [1] column family name

        // FIXME: If there's no row key in current cf, this will fail
        // with exception.
        Parameter rowKey = chooseRowKey(state, this, null);
        this.params.add(rowKey); // [2] column family name

        Parameter column = chooseColumnName(state, this,
                columnFamilyName.toString(), null);
        params.add(column); // [3] column2type

        ParameterType.ConcreteType insertValuesType = new ParameterType.Type2ValueType(
                null, (s, c) -> {
                    List<Parameter> list = new ArrayList<>();
                    list.add(c.params.get(3));
                    return list;
                },
                p -> ((Pair) ((Parameter) p).getValue()).right);
        Parameter insertValues = insertValuesType
                .generateRandomParameter(state, this);
        this.params.add(insertValues); // [4] column2type
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

        return "put "
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
