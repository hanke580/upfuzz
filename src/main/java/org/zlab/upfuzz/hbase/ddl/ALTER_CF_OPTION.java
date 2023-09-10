package org.zlab.upfuzz.hbase.ddl;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.utils.INTType;
import org.zlab.upfuzz.utils.Utilities;

public class ALTER_CF_OPTION extends HBaseCommand {

    public ALTER_CF_OPTION(HBaseState state) {
        super(state);
        Parameter tableName = chooseTable(state, this, null);
        this.params.add(tableName); // [0] table name

        Parameter cfNameType = chooseColumnFamily(state, this, null);
        this.params.add(cfNameType); // [1] column family

        Parameter version = new ParameterType.OptionalType(new INTType(1, 5),
                null)
                        .generateRandomParameter(state, this);
        Parameter COMPRESSIONType = new ParameterType.OptionalType(
                new ParameterType.InCollectionType(
                        CONSTANTSTRINGType.instance,
                        (s, c) -> Utilities
                                .strings2Parameters(
                                        COMPRESSIONTypes),
                        null),
                null).generateRandomParameter(state, this);
        Parameter BLOOMFILTERType = new ParameterType.OptionalType(
                new ParameterType.InCollectionType(
                        CONSTANTSTRINGType.instance,
                        (s, c) -> Utilities
                                .strings2Parameters(
                                        BLOOMFILTERTypes),
                        null),
                null).generateRandomParameter(state, this);
        Parameter INMEMORYType = new ParameterType.OptionalType(
                new ParameterType.InCollectionType(
                        CONSTANTSTRINGType.instance,
                        (s, c) -> Utilities
                                .strings2Parameters(
                                        INMEMORYTypes),
                        null),
                null).generateRandomParameter(state, this);
        params.add(version);
        params.add(COMPRESSIONType);
        params.add(BLOOMFILTERType);
        params.add(INMEMORYType);
    }

    @Override
    public String constructCommandString() {
        Parameter tableName = params.get(0);
        Parameter columnFamilyName = params.get(1);

        String version = params.get(2).toString();
        String versionRuby = version.isEmpty() ? ""
                : ", VERSIONS => " + version;

        String COMPRESSIONType = params.get(3).toString();
        String COMPRESSIONTypeRuby = COMPRESSIONType.isEmpty() ? ""
                : ", COMPRESSION => " + "'" + COMPRESSIONType + "'";

        String BLOOMFILTERType = params.get(4).toString();
        String BLOOMFILTERTypeRuby = BLOOMFILTERType.isEmpty() ? ""
                : ", BLOOMFILTER => " + "'" + BLOOMFILTERType + "'";

        String INMEMORYType = params.get(5).toString();
        String INMEMORYTypeRuby = INMEMORYType.isEmpty() ? ""
                : ", IN_MEMORY => " + INMEMORYType;

        return String.format("alter '%s', {NAME => '%s'%s%s%s%s}",
                tableName.toString(), columnFamilyName.toString(), versionRuby,
                COMPRESSIONTypeRuby, BLOOMFILTERTypeRuby, INMEMORYTypeRuby);
    }

    @Override
    public void updateState(State state) {
        Parameter tableName = params.get(0);
        Parameter columnFamilyName = params.get(1);
    }
}
