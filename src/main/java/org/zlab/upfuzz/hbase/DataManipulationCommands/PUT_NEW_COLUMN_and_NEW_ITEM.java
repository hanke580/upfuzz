package org.zlab.upfuzz.hbase.DataManipulationCommands;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.cassandra.CassandraTypes;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;
import org.zlab.upfuzz.hbase.HBaseTypes;
import org.zlab.upfuzz.utils.*;

import java.util.Collection;

public class PUT_NEW_COLUMN_and_NEW_ITEM  extends HBaseCommand {
    // Table->ColumnFamily->Column
    // CREATE TABLE+ColumnFamily
    // PUT TABLE rowKey ColumnFamily:Column value
    // Table->RowKey:primaryKey
    public PUT_NEW_COLUMN_and_NEW_ITEM(HBaseState state){
        Parameter tableName = chooseTable(state, this, null);
        this.params.add(tableName); // [0] table name

        Parameter columnFamilyName = chooseColumnFamily(state, this, null);
        this.params.add(columnFamilyName); // [1] column family name

        ParameterType.ConcreteType rowKeyType = new ParameterType.NotInCollectionType(
                new ParameterType.NotEmpty(UUIDType.instance),
                (s, c) -> ((HBaseState) s).getRowKey(tableName.toString()), null);
        Parameter rowKeyName = rowKeyType
                .generateRandomParameter(state, this);
        this.params.add(rowKeyName); // [2] row key

        ParameterType.ConcreteType columnsType = // LIST<PAIR<String,TYPEType>>
                new ParameterType.NotEmpty(
                        ParameterType.ConcreteGenericType
                                .constructConcreteGenericType(
                                        PAIRType.instance,
                                        new ParameterType.NotEmpty(
                                                new STRINGType(20)),
                                        HBaseTypes.TYPEType.instance)
                );
        Parameter columns = columnsType
                .generateRandomParameter(state, this);
        params.add(columns); // [3] column2type

        //ParameterType.ConcreteType insertValuesType = new ParameterType.Type2ValueType(
        //        null, columns.value.right, // columns
        //        p -> ((Pair) ((Parameter) p).value).right);
        //Parameter insertValues = insertValuesType
        //        .generateRandomParameter(state, this);
        //this.params.add(insertValues); // [4] insert value
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

        //String columnString = columnFamilies.toString();
        //for (String colFamiStr: columnFamiliesString.split(",")){
        //    String colFamiName = colFamiStr.substring(0, colFamiStr.indexOf(" "));
        //    commandStr.append(", '"+colFamiName+"'");
        //}

        return "PUT "
                + "table name: '" + tableName.toString() + "', "
                + "row key: '" + rowKey.toString() + "', "
                + "column family: '" + columnFamilyName.toString() + ": column name: "
                + columnName.toString() + "', "
                + "insert value: '" + insertValues.toString() + "'";
    }

    @Override
    public void updateState(State state) {
        Parameter tableName = params.get(0);
        Parameter columnFamilyName = params.get(1);
        Parameter rowKey = params.get(2);
        ((HBaseState) state).addRowKey(tableName.toString(), rowKey.toString());
    }
}
