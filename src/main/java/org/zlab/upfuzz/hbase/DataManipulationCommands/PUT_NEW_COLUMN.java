package org.zlab.upfuzz.hbase.DataManipulationCommands;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;
import org.zlab.upfuzz.utils.Pair;

import java.util.Collection;

public class PUT_NEW_COLUMN extends HBaseCommand {

    public PUT_NEW_COLUMN(HBaseState state) {
        Parameter tableName = chooseTable(state, this, null);
        this.params.add(tableName);

        Parameter columnFamilyName = chooseColumnFamily(state, this, null);
        this.params.add(columnFamilyName);

        ParameterType.ConcreteType columnsType = new ParameterType.SuperSetType(
                new ParameterType.SubsetType<>(null,
                        (s, c) -> ((HBaseState) s).getColumnFamily(
                                tableName.toString(),
                                columnFamilyName.toString()).colName2Type,
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
        Parameter keyspaceName = params.get(0);
        Parameter tableName = params.get(1);
        ParameterType.ConcreteType columnNameType = new ParameterType.StreamMapType(
                null, (s, c) -> (Collection) c.params.get(2).getValue(),
                p -> ((Pair) ((Parameter) p).getValue()).left);
        Parameter columnName = columnNameType.generateRandomParameter(null,
                this);
        Parameter insertValues = params.get(3);

        return "INSERT INTO " + keyspaceName.toString() + "."
                + tableName.toString() + " (" + columnName.toString()
                + ") VALUES (" + insertValues.toString() + ");";
    }

    @Override
    public void updateState(State state) {
    }
}
