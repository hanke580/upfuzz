package org.zlab.upfuzz.hbase.ddl;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseColumnFamily;
import org.zlab.upfuzz.hbase.HBaseState;
import org.zlab.upfuzz.utils.*;

public class CREATE extends HBaseCommand {
    // create 'tb3', {NAME => 'cf1', VERSIONS => 3}, {NAME => 'cf2', VERSIONS =>
    // 1}
    // create '<table_name>',
    // {NAME => '<column_family1>', <option1> => <value1>, ...},
    // {NAME => '<column_family2>', <option2> => <value2>, ...},
    // ...
    // Actually, type doesn't matter here, we just need a list of strings

    public CREATE(HBaseState state) {
        super(state);
        Parameter tableName = chooseNewTable(state, this);
        this.params.add(tableName); // [0]=tableName

        // TODO: Add other common options
        // ParameterType.ConcreteType columnsType = // LIST<PAIR<String,
        // Version>>
        // new ParameterType.NotEmpty(ParameterType.ConcreteGenericType
        // .constructConcreteGenericType(
        // CassandraTypes.MapLikeListType.instance,
        // ParameterType.ConcreteGenericType
        // .constructConcreteGenericType(
        // PAIRType.instance,
        // new ParameterType.NotEmpty(
        // new STRINGType(20)),
        // new INTType(1, 5))));
        // Parameter columnFamilies = columnsType.generateRandomParameter(state,
        // this);
        // params.add(columnFamilies); // [1]=columnFamilies

        // =================
        // Another way to generate columnFamilies
        Parameter cfNum = new INTType(1, Config.getConf().MAX_CF_NUM)
                .generateRandomParameter(state, this);
        this.params.add(cfNum);

        for (int i = 0; i < Config.instance.MAX_CF_NUM; i++) {
            // String + version
            Parameter cfName = new ParameterType.NotEmpty(UUIDType.instance)
                    .generateRandomParameter(state, this);
            Parameter version = new INTType(1, 5)
                    .generateRandomParameter(state, this);
            Parameter COMPRESSIONType = new ParameterType.InCollectionType(
                    CONSTANTSTRINGType.instance,
                    (s, c) -> Utilities
                            .strings2Parameters(
                                    COMPRESSIONTypes),
                    null).generateRandomParameter(state, this);
            Parameter BLOOMFILTERType = new ParameterType.InCollectionType(
                    CONSTANTSTRINGType.instance,
                    (s, c) -> Utilities
                            .strings2Parameters(
                                    BLOOMFILTERTypes),
                    null).generateRandomParameter(state, this);
            Parameter INMEMORYType = new ParameterType.InCollectionType(
                    CONSTANTSTRINGType.instance,
                    (s, c) -> Utilities
                            .strings2Parameters(
                                    INMEMORYTypes),
                    null).generateRandomParameter(state, this);
            params.add(cfName);
            params.add(version);
            params.add(COMPRESSIONType);
            params.add(BLOOMFILTERType);
            params.add(INMEMORYType);
        }
    }

    @Override
    public String constructCommandString() {
        // TODO: Need a helper function, add space between all strings

        // create 'tb3', {NAME => 'cf1', VERSIONS => 3}, {NAME => 'cf2',
        // VERSIONS => 1}

        Parameter tableName = params.get(0);
        StringBuilder commandStr = new StringBuilder(
                "create " + "'" + tableName.toString() + "'");
        Parameter cfNum = params.get(1);
        int cfNumVal = (int) cfNum.getValue();
        for (int i = 0; i < cfNumVal; i++) {
            Parameter cfName = params.get(2 + i * 5);
            Parameter version = params.get(3 + i * 5);
            Parameter COMPRESSIONType = params.get(4 + i * 5);
            Parameter BLOOMFILTERType = params.get(5 + i * 5);
            Parameter INMEMORYType = params.get(6 + i * 5);
            commandStr.append(String.format(
                    ", {NAME => '%s', VERSIONS => %d, COMPRESSION => '%s', BLOOMFILTER => '%s', IN_MEMORY => '%s'}",
                    cfName.toString(), version.getValue(),
                    COMPRESSIONType.toString(), BLOOMFILTERType.toString(),
                    INMEMORYType.toString()));
        }
        //
        // Parameter cfs = params.get(1); // LIST<PAIR<TEXTType,TYPE>>
        // List<Parameter> cfArrVal = (List<Parameter>) cfs.getValue();
        //
        // for (Parameter cfParameter : cfArrVal) {
        // String[] cfName2Version = cfParameter.toString().split(" ");
        // String cfName = cfName2Version[0];
        // int version = Integer.parseInt(cfName2Version[1]);
        // commandStr.append(String.format(", {NAME => '%s', VERSIONS => %d}",
        // cfName, version));
        //
        // // HBaseColumnFamily hBaseColumnFamily = new HBaseColumnFamily(
        // // cfName, null);
        // // ((HBaseState) state).addColumnFamily(tableName.toString(),
        // // cfName.toString(), hBaseColumnFamily);
        // }
        return commandStr.toString();
    }

    @Override
    public void updateState(State state) {
        Parameter tableName = params.get(0);
        ((HBaseState) state).addTable(tableName.toString());

        Parameter cfNum = params.get(1);
        int cfNumVal = (int) cfNum.getValue();
        for (int i = 0; i < cfNumVal; i++) {
            Parameter cfName = params.get(2 + i * 5);
            HBaseColumnFamily hBaseColumnFamily = new HBaseColumnFamily(
                    cfName.toString(), null);
            ((HBaseState) state).addColumnFamily(tableName.toString(),
                    cfName.toString(), hBaseColumnFamily);
        }
    }

    @Override
    public void separate(State state) {
        this.params.get(0).regenerate(null, this);
    }

}
