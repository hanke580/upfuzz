package org.zlab.upfuzz.hbase.ddl;

import org.zlab.upfuzz.*;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.utils.Utilities;

public class ALTER_DELETE_FAMILY extends HBaseCommand {
    // alter '<table_name>', {NAME => '<column_family_name>', METHOD =>
    // 'delete'}

    public ALTER_DELETE_FAMILY(HBaseState state) {
        super(state);
        Parameter tableName = chooseTable(state, this, null);
        this.params.add(tableName); // [0] table name

        // if (state.table2families.get(tableName.toString()).size() < 2) {
        // throw new CustomExceptions.PredicateUnSatisfyException("There must be
        // at least 2 column families in the table", null);
        // }
        Predicate predicate = (s, c) -> {
            assert c instanceof ALTER_DELETE_FAMILY;
            return ((HBaseState) s).table2families
                    .get(c.params.get(0).toString()).size() >= 2;
        };
        ParameterType.ConcreteType columnFamilyNameType = new ParameterType.InCollectionType(
                CONSTANTSTRINGType.instance,
                (s, c) -> Utilities
                        .strings2Parameters(((HBaseState) s).table2families
                                .get(c.params.get(0).toString())
                                .keySet()),
                null, predicate);

        Parameter columnFamilyName = columnFamilyNameType
                .generateRandomParameter(state, this);
        this.params.add(columnFamilyName); // [1] column family name
    }

    @Override
    public String constructCommandString() {
        Parameter tableName = params.get(0);
        Parameter columnFamilyName = params.get(1);
        // alter 'mytable', {NAME => 'cf1', METHOD => 'delete'}
        return String.format("alter '%s', {NAME => '%s', METHOD => 'delete'}",
                tableName.toString(), columnFamilyName.toString());
    }

    @Override
    public void updateState(State state) {
        Parameter tableName = params.get(0);
        Parameter columnFamilyName = params.get(1);
        ((HBaseState) state).deleteColumnFamily(
                tableName.toString(),
                columnFamilyName.toString());
    }
}
