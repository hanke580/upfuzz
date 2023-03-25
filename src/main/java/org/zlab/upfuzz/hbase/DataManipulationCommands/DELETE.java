package org.zlab.upfuzz.hbase.DataManipulationCommands;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;
import org.zlab.upfuzz.utils.Pair;

import java.util.Collection;

public class DELETE extends HBaseCommand {

    public DELETE(HBaseState state) {
        Parameter tableName = chooseTable(state, this, null);
        this.params.add(tableName); // [0] table name

        Parameter columnFamilyName = chooseColumnFamily(state, this, null);
        this.params.add(columnFamilyName); // [1] column family name

        Parameter rowKey = chooseRowKey(state, this, null);
        this.params.add(rowKey); // [1] column family name

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

        return "DELETE "
                + "'" + tableName.toString() + "', "
                + "'" + rowKey.toString() + "', "
                + "'" + columnFamilyName.toString() + ":"
                + columnName.toString() + "'";
    }

    @Override
    public void updateState(State state) {

    }
}
