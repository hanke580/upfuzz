package org.zlab.upfuzz.hbase.tools;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.utils.Utilities;

public class MAJOR_COMPACT extends HBaseCommand {
    public MAJOR_COMPACT(HBaseState state) {
        super(state);
        Parameter tableName = chooseTable(state, this, null);
        this.params.add(tableName); // 0 tableName

        // columnFamily
        ParameterType.ConcreteType columnFamilyNameType = new ParameterType.OptionalType(
                new ParameterType.InCollectionType(
                        CONSTANTSTRINGType.instance,
                        (s, c) -> Utilities
                                .strings2Parameters(
                                        ((HBaseState) s).table2families
                                                .get(c.params.get(0).toString())
                                                .keySet()),
                        null),
                null);
        Parameter columnFamilyName = columnFamilyNameType
                .generateRandomParameter(state, this);
        this.params.add(columnFamilyName); // 1 columnFamilyName
    }

    @Override
    public String constructCommandString() {
        String tableName = "'" + params.get(0).toString() + "'";
        String columnFamilyName = params.get(1).toString().isEmpty() ? ""
                : ", '" + params.get(1) + "'";
        return "major_compact " + tableName + columnFamilyName;
    }

    @Override
    public void updateState(State state) {
    }
}