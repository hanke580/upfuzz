package org.zlab.upfuzz.hbase;

import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.utils.Utilities;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public abstract class HBaseCommand extends Command {
    public static final boolean DEBUG = false;

    public static HBaseCommandPool hBaseCommandPool = new HBaseCommandPool();


    public static Parameter chooseTable(State state, Command command,
                                           Object init) {

        ParameterType.ConcreteType tableNameType = new ParameterType.InCollectionType(
                CONSTANTSTRINGType.instance,
                (s, c) -> Utilities.strings2Parameters(
                        ((HBaseState) s).table2families.keySet()),
                null);
        return tableNameType.generateRandomParameter(state, command, init);
    }

    public static Parameter chooseRowKey(State state, Command command,
                                               Object init) {

        ParameterType.ConcreteType columnFamilyNameType = new ParameterType.InCollectionType(
                CONSTANTSTRINGType.instance,
                (s, c) -> Utilities
                        .strings2Parameters(((HBaseState) s).table2rowKeys
                                .get(c.params.get(0).toString())),
                null);
        return columnFamilyNameType.generateRandomParameter(state, command, init);
    }

    public static Parameter chooseColumnName(State state, Command command, String columnFamilyName,
                                               Object init) {

        ParameterType.ConcreteType columnNameType = new ParameterType.InCollectionType(
                CONSTANTSTRINGType.instance,
                (s, c) -> ((HBaseState) s).table2families
                                .get(c.params.get(0).toString()).get(columnFamilyName).colName2Type,
                null);
        return columnNameType.generateRandomParameter(state, command, init);
    }

    public static Parameter chooseNotNullColumnFamily(State state, Command command,
                                               Object init) {
        List<String> columnFamilies = new ArrayList<>(((HBaseState) state).table2families
                .get(command.params.get(0).toString())
                .keySet());
        HashSet<String> notNullColumnFamilies = new HashSet<>();
        for (String columnFamily:columnFamilies){
            if(((HBaseState) state).getColumnFamily(command.params.get(0).toString(), columnFamily).colName2Type != null){
                notNullColumnFamilies.add(columnFamily);
            }
        }
        ParameterType.ConcreteType columnFamilyNameType = new ParameterType.InCollectionType(
                CONSTANTSTRINGType.instance,
                (s, c) -> Utilities
                        .strings2Parameters(notNullColumnFamilies),
                null);
        return columnFamilyNameType.generateRandomParameter(state, command, init);
    }

    public static Parameter chooseColumnFamily(State state, Command command,
                                        Object init) {

        ParameterType.ConcreteType columnFamilyNameType = new ParameterType.InCollectionType(
                CONSTANTSTRINGType.instance,
                (s, c) -> Utilities
                        .strings2Parameters(((HBaseState) s).table2families
                                .get(c.params.get(0).toString())
                                .keySet()),
                null);
        return columnFamilyNameType.generateRandomParameter(state, command, init);
    }

    @Override
    public void separate(State state) {
    }
}
