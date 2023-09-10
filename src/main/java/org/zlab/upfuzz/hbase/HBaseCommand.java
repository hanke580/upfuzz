package org.zlab.upfuzz.hbase;

import org.zlab.upfuzz.*;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.utils.UUIDType;
import org.zlab.upfuzz.utils.Utilities;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public abstract class HBaseCommand extends Command {
    public static final boolean DEBUG = false;

    // SNAPPY, LZ4, LZO results in error, might need other lib for installation
    public static String[] COMPRESSIONTypes = { "NONE", "GZ" };
    public static String[] BLOOMFILTERTypes = { "NONE", "ROW", "ROWCOL" };
    public static String[] INMEMORYTypes = { "false", "true" };
    // Namespace
    // public static String[] methodTypes = { "set", "unset" };
    public static String[] THROTTLE_TYPES_RW = { "READ", "WRITE" };
    public static String[] QUOTA_SPACE_POLICY_TYPES = { "NO_INSERTS",
            "NO_WRITES",
            "NO_WRITES_COMPACTIONS", "NO_INSERTS_NO_WRITES" };

    public HBaseCommand(HBaseState state) {
    }

    public static HBaseCommandPool hbaseCommandPool = new HBaseCommandPool();

    public static Parameter chooseNamespace(State state, Command command,
            Object init) {
        ParameterType.ConcreteType nameType = new ParameterType.InCollectionType(
                CONSTANTSTRINGType.instance,
                (s, c) -> ((HBaseState) s).getNamespaces(),
                null);
        return nameType.generateRandomParameter(state, command, init);
    }

    public static Parameter chooseTable(State state, Command command,
            Object init) {
        ParameterType.ConcreteType tableNameType = new ParameterType.InCollectionType(
                CONSTANTSTRINGType.instance,
                (s, c) -> Utilities.strings2Parameters(
                        ((HBaseState) s).table2families.keySet()),
                null);
        return tableNameType.generateRandomParameter(state, command, init);
    }

    public static Parameter chooseTable(State state, Command command) {
        ParameterType.ConcreteType tableNameType = new ParameterType.InCollectionType(
                CONSTANTSTRINGType.instance,
                (s, c) -> Utilities.strings2Parameters(
                        ((HBaseState) s).table2families.keySet()),
                null);
        return tableNameType.generateRandomParameter(state, command);
    }

    public static Parameter chooseNewTable(State state, Command command) {
        return new ParameterType.NotInCollectionType(
                new ParameterType.NotEmpty(UUIDType.instance),
                (s, c) -> ((HBaseState) s).getTables(), null)
                        .generateRandomParameter(state, command);
    }

    public static Parameter chooseRowKey(State state, Command command,
            Object init) {
        ParameterType.ConcreteType rowKeyNameType = new ParameterType.InCollectionType(
                CONSTANTSTRINGType.instance,
                (s, c) -> Utilities
                        .strings2Parameters(((HBaseState) s).table2rowKeys
                                .get(c.params.get(0).toString())),
                null);
        return rowKeyNameType.generateRandomParameter(state, command,
                init);
    }

    public static Parameter chooseColumnName(State state, Command command,
            String columnFamilyName,
            Object init) {

        ParameterType.ConcreteType columnNameType = new ParameterType.InCollectionType(
                CONSTANTSTRINGType.instance,
                (s, c) -> ((HBaseState) s).table2families
                        .get(c.params.get(0).toString())
                        .get(columnFamilyName).colName2Type,
                null);
        return columnNameType.generateRandomParameter(state, command, init);
    }

    public static Parameter chooseNotNullColumnFamily(State state,
            Command command,
            Object init) {
        List<String> columnFamilies = new ArrayList<>(
                ((HBaseState) state).table2families
                        .get(command.params.get(0).toString())
                        .keySet());
        HashSet<String> notNullColumnFamilies = new HashSet<>();
        for (String columnFamily : columnFamilies) {
            if (((HBaseState) state).getColumnFamily(
                    command.params.get(0).toString(),
                    columnFamily).colName2Type != null) {
                notNullColumnFamilies.add(columnFamily);
            }
        }
        ParameterType.ConcreteType columnFamilyNameType = new ParameterType.InCollectionType(
                CONSTANTSTRINGType.instance,
                (s, c) -> Utilities
                        .strings2Parameters(notNullColumnFamilies),
                null);
        return columnFamilyNameType.generateRandomParameter(state, command,
                init);
    }

    public static Parameter chooseNotEmptyColumnFamily(State state,
            Command command,
            Object init) {
        List<String> columnFamilies = new ArrayList<>(
                ((HBaseState) state).table2families
                        .get(command.params.get(0).toString())
                        .keySet());
        HashSet<String> notNullColumnFamilies = new HashSet<>();
        for (String columnFamily : columnFamilies) {
            List<Parameter> colName2Type = ((HBaseState) state).getColumnFamily(
                    command.params.get(0).toString(),
                    columnFamily).colName2Type;

            if (((HBaseState) state).getColumnFamily(
                    command.params.get(0).toString(),
                    columnFamily).colName2Type != null
                    && colName2Type.size() > 0) {
                notNullColumnFamilies.add(columnFamily);
            }
        }
        ParameterType.ConcreteType columnFamilyNameType = new ParameterType.InCollectionType(
                CONSTANTSTRINGType.instance,
                (s, c) -> Utilities
                        .strings2Parameters(notNullColumnFamilies),
                null);
        return columnFamilyNameType.generateRandomParameter(state, command,
                init);
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
        return columnFamilyNameType.generateRandomParameter(state, command,
                init);
    }

    public static Parameter chooseColumnFamily(State state, Command command) {
        ParameterType.ConcreteType columnFamilyNameType = new ParameterType.InCollectionType(
                CONSTANTSTRINGType.instance,
                (s, c) -> Utilities
                        .strings2Parameters(((HBaseState) s).table2families
                                .get(c.params.get(0).toString())
                                .keySet()),
                null);
        return columnFamilyNameType.generateRandomParameter(state, command);
    }

    public static Parameter chooseOptionalColumnFamily(State state,
            Command command) {
        return new ParameterType.OptionalType(
                new ParameterType.InCollectionType(
                        CONSTANTSTRINGType.instance,
                        (s, c) -> Utilities
                                .strings2Parameters(
                                        ((HBaseState) s).table2families
                                                .get(c.params.get(0).toString())
                                                .keySet()),
                        null),
                null).generateRandomParameter(state, command);
    }

    public static Parameter chooseSnapshot(State state, Command command) {
        ParameterType.ConcreteType nameType = new ParameterType.InCollectionType(
                CONSTANTSTRINGType.instance,
                (s, c) -> Utilities.strings2Parameters(
                        ((HBaseState) s).snapshots.keySet()),
                null);
        return nameType.generateRandomParameter(state, command);
    }

    @Override
    public void separate(State state) {
    }
}
