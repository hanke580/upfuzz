package org.zlab.upfuzz.hbase.dml;

import org.zlab.upfuzz.CustomExceptions;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseColumnFamily;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.utils.Pair;
import org.zlab.upfuzz.utils.Utilities;

import java.util.Collection;

public class DELETE extends HBaseCommand {

    boolean validConstruction;

    public DELETE(HBaseState state) {
        super(state);
        validConstruction = true;

        try {
            Parameter tableName = chooseTable(state, this, null);
            this.params.add(tableName); // [0] table name

            Parameter rowKey = chooseRowKey(state, this, null);
            this.params.add(rowKey); // [1] row key

            Parameter columnFamilyName = chooseNotEmptyColumnFamily(state, this,
                    null);
            this.params.add(columnFamilyName); // [2] column family name

            HBaseColumnFamily cf = state.table2families
                    .get(tableName.toString())
                    .get(columnFamilyName.toString());

            Parameter qualifier = cf.getRandomQualifier();

            // this should not be the case since chooseNotEmptyColumnFamily
            // returns non-empty
            // column families, but for sanity
            if (qualifier == null) {
                validConstruction = false;
                return;
            }

            this.params.add(qualifier); // [3] qualifier

            Parameter VISIBILITYType = new ParameterType.OptionalType(
                    new ParameterType.InCollectionType(
                            CONSTANTSTRINGType.instance,
                            (s, c) -> Utilities
                                    .strings2Parameters(
                                            VISIBILITYTypes),
                            null),
                    null).generateRandomParameter(state, this);
            this.params.add(VISIBILITYType); // [4] visibility
        } catch (CustomExceptions.EmptyCollectionException e) {
            validConstruction = false;
        }
    }

    @Override
    public String constructCommandString() {
        if (!validConstruction) {
            return "delete ";
        }
        Parameter tableName = params.get(0);
        Parameter rowKey = params.get(1);
        Parameter columnFamilyName = params.get(2);
        Parameter qualifier = params.get(3);
        Parameter VISIBILITYType = this.params.get(4);

        return "delete "
                + "'" + tableName.toString() + "', "
                + "'" + rowKey + "', "
                + "'" + columnFamilyName.toString() + ":"
                + qualifier.toString() + "'"
                +
                (VISIBILITYType.toString().isEmpty() ? ""
                        : ", {VISIBILITY=>'" + VISIBILITYType + "'}");
    }

    @Override
    public void updateState(State state) {

    }

    @Override
    public boolean mutate(State s) throws Exception {
        if (!validConstruction) {
            return false;
        }
        try {
            super.mutate(s);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
