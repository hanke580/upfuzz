package org.zlab.upfuzz.hbase.DataManipulationCommands;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;
import org.zlab.upfuzz.hbase.HBaseTypes;
import org.zlab.upfuzz.utils.*;

import java.util.Collection;

public class PUT_NEW_ITEM extends HBaseCommand {

    public PUT_NEW_ITEM(HBaseState state) {
        Parameter tableName = chooseTable(state, this, null);
        this.params.add(tableName); // [0] table name

        Parameter columnFamilyName = chooseNotNullColumnFamily(state, this,
                null);
        this.params.add(columnFamilyName); // [1] column family name

        ParameterType.ConcreteType rowKeyType = new ParameterType.NotInCollectionType(
                new ParameterType.NotEmpty(UUIDType.instance),
                (s, c) -> ((HBaseState) s).getRowKey(tableName.toString()),
                null);
        Parameter rowKeyName = rowKeyType
                .generateRandomParameter(state, this);
        this.params.add(rowKeyName); // [2] row key

        Parameter column = chooseColumnName(state, this,
                columnFamilyName.toString(), null);
        params.add(column); // [3] column2type

        ParameterType.ConcreteType valueType = // LIST<PAIR<String,TYPEType>>
                new ParameterType.NotEmpty(
                        ParameterType.ConcreteGenericType
                                .constructConcreteGenericType(
                                        PAIRType.instance,
                                        new ParameterType.NotEmpty(
                                                new STRINGType(30)),
                                        HBaseTypes.TYPEType.instance));
        Parameter value = valueType
                .generateRandomParameter(state, this);
        params.add(value); // [4] column2type
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
        valueStr = valueStr.substring(0, valueStr.indexOf(" "));

        // String columnString = columnFamilies.toString();
        // for (String colFamiStr: columnFamiliesString.split(",")){
        // String colFamiName = colFamiStr.substring(0, colFamiStr.indexOf("
        // "));
        // commandStr.append(", '"+colFamiName+"'");
        // }

        return "PUT "
                + "'" + tableName.toString() + "', "
                + "'" + rowKey.toString() + "', "
                + "'" + columnFamilyName.toString() + "':'"
                + colNameStr + "', "
                + "'" + valueStr + "'";
    }

    @Override
    public void updateState(State state) {
        Parameter tableName = params.get(0);
        Parameter rowKey = params.get(2);
        ((HBaseState) state).addRowKey(tableName.toString(), rowKey.toString());
    }
}
