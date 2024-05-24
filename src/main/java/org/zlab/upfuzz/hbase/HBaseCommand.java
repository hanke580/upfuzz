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

    public static String[] CONSISTENCYTypes = { "TIMELINE", "STRONG" };

    public static String[] VISIBILITYTypes = { "PRIVATE", "SECRET" };

    public static String[] BLOCKCACHETypes = { "true", "false" };

    public static String[] MOB_COMPACT_PARTITION_POLICY_TYPES = { "daily",
            "weekly", "monthly" };

    public static String[] SPLIT_ENABLED_TYPES = { "true", "false" };
    public static String[] MERGE_ENABLED_TYPES = { "true", "false" };

    public static String[] CACHE_BLOCKS_TYPES = { "true", "false" };

    public static String[] IS_MOB_TYPES = { "true", "false" };
    public static String[] SPLITALGO_TYPES = { "HexStringSplit", "UniformSplit",
            "DecimalStringSplit" };

    public static String[] CREATE_CONFIG_OPTIONS = {
            "hbase.store.file-tracker.impl",
            "hbase.hregion.scan.loadColumnFamiliesOnDemand",
            "hbase.hstore.blockingStoreFiles",
            "hbase.acl.sync.to.hdfs.enable",
            "hbase.regionserver.region.split_restriction.type",
//        "hbase.hstore.defaultengine.compactionpolicy.class",
            "hbase.rsgroup.name",
            "CACHE_DATA_IN_L1"
    };

    public static String[] TABLE_CONFIG_OPTIONS = {
            "MAX_FILESIZE",
            "READONLY",
            "MEMSTORE_FLUSHSIZE",
            "NORMALIZATION_ENABLED",
            "NORMALIZER_TARGET_REGION_COUNT",
//            "NORMALIZER_TARGET_REGION_SIZE_MB",
            "DURABILITY"
    };

    public static String[] CREATE_CONFIG_OPTIONS_BOOLEAN_TYPES = { "true",
            "false" };

    public static String[] CREATE_CONFIG_REGION_SPLIT_RESTRICTION_TYPES = {
            "KeyPrefix",
            "DelimitedKeyPrefix"
    };

    public static String[] CREATE_REGION_SPLIT_RESTRICTION_DELIMS = {
            ",", ".", "!", "/", "\\\\", "!", "@", "#", "$", "%", "^", "&", "*",
            "_", "-", "="
//            "\\\\"
    };

    public static String[] FILTER_TYPES = {
            "DependentColumnFilter", "KeyOnlyFilter",
            "ColumnCountGetFilter", /*"SingleColumnValueFilter",*/
            "PrefixFilter",
            /*"SingleColumnValueExcludeFilter",*/ "FirstKeyOnlyFilter",
            "ColumnRangeFilter",
            "ColumnValueFilter", /*"TimestampsFilter",*/ "FamilyFilter",
            "QualifierFilter", "ColumnPrefixFilter",
            "RowFilter",
            /*"MultipleColumnPrefixFilter",*/ "InclusiveStopFilter",
            "PageFilter", "ValueFilter",
            "ColumnPaginationFilter"
    };

    public static String[] BINARY_OPS = {
            "<", "<=", "=", "!=", ">", ">="
    };

    public static String[] COMPARATOR_TYPES = {
            "binary", "binaryprefix", /*"regexstring", "substring"*/
    };

    public static String[] COLUMN_RANGE_FILTER_EXCLUDE_START_TYPES = { "true",
            "false" };
    public static String[] COLUMN_RANGE_FILTER_EXCLUDE_END_TYPES = { "true",
            "false" };

    // READ('R'), WRITE('W'), EXEC('X'), CREATE('C'), ADMIN('A')
    // we precompute every combination, of every possible size
    public static String[] PERMISSION_OPTIONS = {
            "R", "W", "X", "C", "A", "RW", "RX", "RC", "RA", "WX", "WC", "WA",
            "XC", "XA", "CA", "RWX", "RWC", "RWA", "RXC", "RXA", "RCA", "WXC",
            "WXA", "WCA", "XCA", "RWXC", "RWXA", "RWCA", "RXCA", "WXCA", "RWXCA"
    };

    public static String[] DURABILITY_TYPES = { "ASYNC_WAL", "FSYNC_WAL",
            "SKIP_WAL", "SYNC_WAL" };

    /**
     *
     *
     * FORMATTER
     *
     * arg values (from: https://github.com/apache/hbase/blob/c0fb41fea61d2ee8d64b63e793abab9acb990d35/hbase-shell/src/main/ruby/shell/commands/scan.rb#L98):
     * The FORMATTER can be stipulated:
     *
     *  1. either as a org.apache.hadoop.hbase.util.Bytes method name (e.g, toInt, toString)
     *  2. or as a custom class followed by method name: e.g. 'c(MyFormatterClass).format'.
     *
     * You can set a
     * formatter for all columns (including, all key parts) using the "FORMATTER"
     * and "FORMATTER_CLASS" options. The default "FORMATTER_CLASS" is
     * "org.apache.hadoop.hbase.util.Bytes".
     *
     *   hbase> scan 't1', {FORMATTER => 'toString'}
     *          https://hbase.apache.org/apidocs/org/apache/hadoop/hbase/util/Bytes.html
     *          toDouble
     *          toFloat
     *          toInt
     *          toLong
     *          toShort
     *          toBigDecimal
     *
     *   hbase> scan 't1', {FORMATTER_CLASS => 'org.apache.hadoop.hbase.util.Bytes', FORMATTER => 'toString'}
     */
    public static String[] FORMATTER_CLASS_TYPES = {
            "org.apache.hadoop.hbase.util.Bytes"
    };
    public static String[] DEFAULT_FORMATTER_FUNCTIONS = {
            "toDouble", "toFloat", "toInt", "toLong", "toShort", "toBigDecimal"
    };

    public static String[] AUTHORIZATION_TYPES = { "['PRIVATE']", "['SECRET']",
            "['PRIVATE', 'SECRET']" };

    public static String[] LIST_REGIONS_LOCALITY_THRESHOLD = {
            "0.0",
            "0.1",
            "0.2",
            "0.3",
            "0.4",
            "0.5",
            "0.6",
            "0.7",
            "0.8",
            "0.9",
            "1.0"
    };

    public static final String[] CLONE_TABLE_SCHEMA_OPTIONAL_ARGS = {
            "", ", false"
    };

    public static final String REGEX_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";
    public static final String REGEX_METACHARACTERS = ".^$*+?()[]{}|\\";
    public static final String REGEX_QUANTIFIERS = ".*+?";

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

    public static Parameter chooseNotNullColumn(State state,
            Command command,
            Object init) {

        ArrayList<Parameter> columns = new ArrayList<>();
        String tableName = command.params.get(0).toString();

        if (((HBaseState) state).table2families.containsKey(tableName)) {
            for (HBaseColumnFamily cf : ((HBaseState) state).table2families
                    .get(tableName).values()) {
                columns.addAll(cf.colName2Type);
            }
        }
        ParameterType.ConcreteType columnType = new ParameterType.InCollectionType(
                CONSTANTSTRINGType.instance,
                (s, c) -> columns,
                null);
        return columnType.generateRandomParameter(state, command,
                init);
    }

    public static boolean tableHasNoQualifiers(State state,
            Command command) {
        List<String> columnFamilies = new ArrayList<>(
                ((HBaseState) state).table2families
                        .get(command.params.get(0).toString())
                        .keySet());
        HashSet<String> columns = new HashSet<>();
        List<Parameter> colName2Type;
        for (String columnFamily : columnFamilies) {
            colName2Type = ((HBaseState) state).getColumnFamily(
                    command.params.get(0).toString(),
                    columnFamily).colName2Type;
            if (colName2Type != null && !colName2Type.isEmpty()) {
                for (Parameter p : colName2Type) {
                    columns.add(p.toString());
                }
            }
        }
        return columns.isEmpty();
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
                    && !colName2Type.isEmpty()) {
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

    @Override
    public boolean mutate(State s) throws Exception {
        try {
            super.mutate(s);
            return true;
        } catch (CustomExceptions.PredicateUnSatisfyException e) {
            return false;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
