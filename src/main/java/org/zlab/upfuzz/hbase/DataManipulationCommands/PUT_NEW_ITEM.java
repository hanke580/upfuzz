package org.zlab.upfuzz.hbase.DataManipulationCommands;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;
import org.zlab.upfuzz.hbase.HBaseTypes;
import org.zlab.upfuzz.utils.PAIRType;
import org.zlab.upfuzz.utils.Pair;
import org.zlab.upfuzz.utils.STRINGType;
import org.zlab.upfuzz.utils.Utilities;

import java.util.Collection;

public class PUT_NEW_ITEM extends HBaseCommand {

    public PUT_NEW_ITEM(HBaseState state){
        Parameter tableName = chooseTable(state, this, null);
        this.params.add(tableName); // [0] table name

        Parameter columnFamilyName = chooseColumnFamily(state, this, null);
        this.params.add(columnFamilyName); // [1] column family name

        ParameterType.ConcreteType rowKeyType = new ParameterType.NotInCollectionType(
                new ParameterType.NotEmpty(new STRINGType(10)),
                (s, c) -> Utilities
                        .strings2Parameters(((HBaseState) s).getRowKey(tableName.toString())), null);
        Parameter rowKeyName = rowKeyType
                .generateRandomParameter(state, this);
        this.params.add(rowKeyName); // [2] row key

        ParameterType.ConcreteType columnsType = new ParameterType.SuperSetType(
                new ParameterType.SubsetType(null,
                        (s, c) -> ((HBaseState) s).getColumnFamily(
                                c.params.get(0).toString(),
                                c.params.get(1).toString()).colName2Type,
                        null),
                null,
                null);
        Parameter columns = columnsType
                .generateRandomParameter(state, this);
        this.params.add(columns);

        ParameterType.ConcreteType insertValuesType = new ParameterType.Type2ValueType(
                null, (s, c) -> (Collection) c.params.get(2).getValue(), // columns
                p -> ((Pair) ((Parameter) p).value).right);
        Parameter insertValues = insertValuesType
                .generateRandomParameter(state, this);
        this.params.add(insertValues);
    }

    @Override
    public String constructCommandString() {
        Parameter tableName = params.get(0);
        Parameter columnFamilyName = params.get(1);
        Parameter rowKey = params.get(2);
        ParameterType.ConcreteType columnNameType = new ParameterType.StreamMapType(
                null, (s, c) -> (Collection) c.params.get(3).getValue(),
                p -> ((Pair) ((Parameter) p).getValue()).left);
        Parameter columnName = columnNameType.generateRandomParameter(null,
                this);
        Parameter insertValues = params.get(4);


        return "PUT "
                + "'" + tableName.toString() + "' "
                + "'" + rowKey.toString() + "' "
                + "'" + columnFamilyName.toString() + ":"
                + columnName.toString() + "' "
                + "'" + insertValues.toString() + "'";
    }

    @Override
    public void updateState(State state) {
        Parameter tableName = params.get(0);
        Parameter columnFamilyName = params.get(1);
        Parameter rowKey = params.get(2);
        ((HBaseState) state).addRowKey(tableName.toString(), rowKey.toString());
    }
}
