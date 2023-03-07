package org.zlab.upfuzz.hbase.DataDefinitionCommands;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseColumnFamily;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;
import org.zlab.upfuzz.utils.Pair;
import org.zlab.upfuzz.utils.STRINGType;
import org.zlab.upfuzz.utils.Utilities;

import java.util.Collection;

public class ALTER_ADD_FAMILY extends HBaseCommand {

    public ALTER_ADD_FAMILY(HBaseState state){
        Parameter tableName = chooseTable(state, this, null);
        this.params.add(tableName); // [0] table name

        ParameterType.ConcreteType columnFamilyType = new ParameterType.NotInCollectionType(
                new ParameterType.NotEmpty(new STRINGType(10)),
                (s, c) -> ((HBaseState) s).getColumnFamiliesInTable(tableName.toString()), null);
        Parameter columnFamily = columnFamilyType
                .generateRandomParameter(state, this);
        this.params.add(columnFamily); // [1] column family
    }

    @Override
    public String constructCommandString() {
        Parameter tableName = params.get(0);
        Parameter columnFamilyName = params.get(1);

        return "ALTER "
                + "'" + tableName.toString() + "',"
                + "'" + columnFamilyName.toString() + "'";
    }

    @Override
    public void updateState(State state) {
        Parameter tableName = params.get(0);
        Parameter columnFamilyName = params.get(1);

        ((HBaseState) state).addColumnFamily(
                tableName.toString(),
                columnFamilyName.toString(),
                new HBaseColumnFamily(
                        columnFamilyName.toString(),
                        null
                )
        );
    }
}
