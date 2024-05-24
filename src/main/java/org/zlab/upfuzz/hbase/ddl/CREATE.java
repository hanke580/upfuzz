package org.zlab.upfuzz.hbase.ddl;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseColumnFamily;
import org.zlab.upfuzz.hbase.HBaseState;
import org.zlab.upfuzz.utils.*;

import java.util.List;

public class CREATE extends HBaseCommand {

    /**
     *
     * TODO:
     * 1. make the parameters for column families and the table optionalType
     * 2. Get parameter value limits for TTL, MOB_THRESHOLD (units is bytes ig), VERSIONS, numregions, regionReplication
     * 3. Work out the configuration parameter (https://github.com/search?q=repo%3Aapache%2Fhbase+%22CONFIGURATION+%3D%3E%22&type=code)
     * 4. work out the metadata param
     *   METADATA => { 'mykey' => 'myvalue' } // note the separate {} // for the table
    CONFIGURATION => {
         'hbase.store.file-tracker.impl' => 'FILE', // FILE is the only options? https://blog.cloudera.com/unlocking-hbase-on-s3-with-the-new-store-file-tracking-feature/
    
         'hbase.hregion.scan.loadColumnFamiliesOnDemand' => 'true',
    
         // default (below) is 10, possibly 60, 100: https://github.com/apache/hbase/blob/a16f45811ec54ce3ede229579177151675781862/src/main/asciidoc/_chapters/architecture.adoc#L2084
         'hbase.hstore.blockingStoreFiles' => '10',
    
         'hbase.acl.sync.to.hdfs.enable' => 'true',
    
         # Create a table with a "KeyPrefix" split restriction, where the prefix length is 2 bytes
         # KeyPrefix and DelimitedKeyPrefix are the only 2 options
         https://hbase.apache.org/devapidocs/org/apache/hadoop/hbase/regionserver/RegionSplitRestriction.html
         hbase> create 'tbl1', 'fam',
         {CONFIGURATION => {'hbase.regionserver.region.split_restriction.type' => 'KeyPrefix',
         'hbase.regionserver.region.split_restriction.prefix_length' => '2'}}
         # Create a table with a "DelimitedKeyPrefix" split restriction, where the delimiter is a comma
         hbase> create 'tbl2', 'fam',
         {CONFIGURATION => {'hbase.regionserver.region.split_restriction.type' => 'DelimitedKeyPrefix',
         'hbase.regionserver.region.split_restriction.delimiter' => ','}}
    
            find the poilicies here: https://github.com/apache/hbase/tree/a16f45811ec54ce3ede229579177151675781862/hbase-server/src/main/java/org/apache/hadoop/hbase/regionserver/compactions
         'hbase.hstore.defaultengine.compactionpolicy.class' =>
        'org.apache.hadoop.hbase.regionserver.compactions.FIFOCompactionPolicy'
            doesn't support default TTL
    `       "RatioBasedCompactionPolicy"
        "ExploringCompactionPolicy"
         StripeCompactionPolicy
         DateTieredCompactionPolicy
    
    
     https://github.com/apache/hbase/blob/a16f45811ec54ce3ede229579177151675781862/src/main/asciidoc/_chapters/ops_mgt.adoc#L3831
     CONFIGURATION => { 'hbase.rsgroup.name' => group_name }
    `
         CACHE_DATA_IN_L1 => 'true'
    
         SPLIT_ENABLED => false,
    
         MERGE_ENABLED => false,
    
         DFS_REPLICATION => 1, can't be -ve, seen upto 7 in search
     IN_MEMORY => 'true'
    
     create 'playCompactionPolicy', {NAME => 'f1', DFS_REPLICATION => 1, TTL => 123456, 'hbase.hstore.defaultengine.compactionpolicy.class' => 'FIFOCompactionPolicy'}
     alter 'playCompactionPolicy', {NAME => 'f1', DFS_REPLICATION => 1, TTL => 123456, 'compactionpolicy.class' => 'org.apache.hadoop.hbase.regionserver.compactions.FIFOCompactionPolicy'}
     alter 'playCompactionPolicy', {NAME => 'f1', DFS_REPLICATION => 1, TTL => 123456, DefaultStoreEngine.DEFAULT_COMPACTION_POLICY_CLASS_KEY => 'hbase.hstore.defaultengine.compactionpolicy.class'}
     }
     */

    public CREATE(HBaseState state) {
        super(state);
        Parameter tableName = chooseNewTable(state, this);
        this.params.add(tableName); // [0]=tableName

        // =================
        // Another way to generate columnFamilies
        Parameter cfNum = new INTType(1, Config.getConf().MAX_CF_NUM)
                .generateRandomParameter(state, this);
        this.params.add(cfNum);

        for (int i = 0; i < Config.instance.MAX_CF_NUM; i++) {
            // String + version

            Parameter cfName = new ParameterType.NotEmpty(UUIDType.instance)
                    .generateRandomParameter(state, this);

            Parameter version = new ParameterType.OptionalType(
                    new INTType(1, 5), null)
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

            Parameter ttl = new ParameterType.OptionalType(
                    new INTType(3600, 1000000), null)
                            .generateRandomParameter(state, this); // in seconds

            Parameter blockCache = new ParameterType.OptionalType(
                    new ParameterType.InCollectionType(
                            CONSTANTSTRINGType.instance,
                            (s, c) -> Utilities
                                    .strings2Parameters(
                                            BLOCKCACHETypes),
                            null),
                    null).generateRandomParameter(state, this);

            Parameter isMob = new ParameterType.OptionalType(
                    new ParameterType.InCollectionType(
                            CONSTANTSTRINGType.instance,
                            (s, c) -> Utilities
                                    .strings2Parameters(
                                            IS_MOB_TYPES),
                            null),
                    null).generateRandomParameter(state, this);

            // TODO: figure out a sensible upper bound for MOB size
            Parameter mobSize = new ParameterType.OptionalType(
                    new INTType(1000000, 2000000), null)
                            .generateRandomParameter(state, this); // in seconds

            Parameter mobCompactionPartitionPolicyType = new ParameterType.OptionalType(
                    new ParameterType.InCollectionType(
                            CONSTANTSTRINGType.instance,
                            (s, c) -> Utilities
                                    .strings2Parameters(
                                            MOB_COMPACT_PARTITION_POLICY_TYPES),
                            null),
                    null).generateRandomParameter(state, this);

            Parameter dfsReplication = new ParameterType.OptionalType(
                    new INTType(1, 7), null)
                            .generateRandomParameter(state, this);

            Parameter config = new ParameterType.OptionalType(
                    new CFCONFIGType(), null)
                            .generateRandomParameter(state, this);

            this.params.add(cfName);
            this.params.add(version);
            this.params.add(COMPRESSIONType);
            this.params.add(BLOOMFILTERType);
            this.params.add(INMEMORYType);
            this.params.add(ttl);
            this.params.add(blockCache);
            this.params.add(isMob);
            this.params.add(mobSize);
            this.params.add(mobCompactionPartitionPolicyType);
            this.params.add(dfsReplication);
            this.params.add(config);

        }
        Parameter splitEnabled = new ParameterType.OptionalType(
                new ParameterType.InCollectionType(
                        CONSTANTSTRINGType.instance,
                        (s, c) -> Utilities
                                .strings2Parameters(
                                        SPLIT_ENABLED_TYPES),
                        null),
                null).generateRandomParameter(state, this);

        Parameter mergeEnabled = new ParameterType.OptionalType(
                new ParameterType.InCollectionType(
                        CONSTANTSTRINGType.instance,
                        (s, c) -> Utilities
                                .strings2Parameters(
                                        MERGE_ENABLED_TYPES),
                        null),
                null).generateRandomParameter(state, this);

        Parameter numRegions = new ParameterType.OptionalType(
                new INTType(2, 20), null)
                        .generateRandomParameter(state, this);

        Parameter splitAlgo = new ParameterType.OptionalType(
                new ParameterType.InCollectionType(
                        CONSTANTSTRINGType.instance,
                        (s, c) -> Utilities
                                .strings2Parameters(
                                        SPLITALGO_TYPES),
                        null),
                null).generateRandomParameter(state, this);

        // if we specify NUMREGIONS, we have to specify a SPLITALGO
        if (!numRegions.toString().isEmpty()
                && splitAlgo.toString().isEmpty()) {
            splitAlgo = new ParameterType.InCollectionType(
                    CONSTANTSTRINGType.instance,
                    (s, c) -> Utilities
                            .strings2Parameters(
                                    SPLITALGO_TYPES),
                    null).generateRandomParameter(state, this);
        }
        // and vice versa
        else if (numRegions.toString().isEmpty()
                && !splitAlgo.toString().isEmpty()) {
            numRegions = new INTType(2, 20).generateRandomParameter(state,
                    this);
        }

        Parameter regionReplication = new ParameterType.OptionalType(
                new INTType(1, 4), null)
                        .generateRandomParameter(state, this);

        Parameter tableOtherConfig = new ParameterType.OptionalType(
                new TABLECONFIGType(), null)
                        .generateRandomParameter(state, this);

        this.params.add(splitEnabled);
        this.params.add(mergeEnabled);
        this.params.add(splitAlgo);
        this.params.add(numRegions);
        this.params.add(regionReplication);
        this.params.add(tableOtherConfig);
    }

    @Override
    public String constructCommandString() {
        // TODO: Need a helper function, add space between all strings

        String[] CF_ARG_NAMES = {
                "NAME", "VERSIONS", "COMPRESSION", "BLOOMFILTER", "IN_MEMORY",
                "TTL",
                "BLOCKCACHE", "IS_MOB", "MOB_THRESHOLD",
                "MOB_COMPACT_PARTITION_POLICY",
                "DFS_REPLICATION", "CONFIGURATION"
        };
        // for each of the arguments for the column families, we store
        // a flag to determine whether we surround the argument value with
        // quotes or not
        Boolean[] CF_ARG_SURROUND_WITH_QUOTES = {
                true, false, true, true, true, false,
                false, false, false, true,
                false, false
        };

        Parameter tableName = params.get(0);
        StringBuilder commandStr = new StringBuilder(
                "create " + "'" + tableName.toString() + "'");
        commandStr.append(", ");
        Parameter cfNum = params.get(1);
        int cfNumVal = (int) cfNum.getValue();
        for (int i = 0; i < cfNumVal; i++) {
            if (i > 0)
                commandStr.append(", ");
            commandStr.append("{");
            // we don't need to handle the case where the result of getCfString
            // is empty since one arg is not optional i.e. the cf name
            commandStr.append(
                    getCfString(
                            params.subList(2 + i * 12, 14 + i * 12),
                            List.of(CF_ARG_NAMES),
                            List.of(CF_ARG_SURROUND_WITH_QUOTES)));
            commandStr.append("}");
        }
        String[] TABLE_ARG_NAMES = { "SPLIT_ENABLED", "MERGE_ENABLED",
                "SPLITALGO", "NUMREGIONS", "REGION_REPLICATION" };
        Boolean[] TABLE_ARG_SURROUND_WITH_QUOTES = { false, false, true, false,
                false };
        String tableConfig = getCfString(
                params.subList(params.size() - 6, params.size() - 1),
                List.of(TABLE_ARG_NAMES),
                List.of(TABLE_ARG_SURROUND_WITH_QUOTES));
        if (!tableConfig.isEmpty()) {
            // we place a comma between the last column family parameter, and
            // the table config, if the table config is not empty
            commandStr.append(", ").append(tableConfig);
        }
        Parameter tableOtherConfig = params.get(params.size() - 1);
        if (!tableOtherConfig.toString().isEmpty()) {
            commandStr.append(", ").append(tableOtherConfig.toString());
        }
        return commandStr.toString();
    }

    // handles optional params as well!
    private String getCfString(List<Parameter> args, List<String> argNames,
            List<Boolean> surroundWithQuotes) {
        assert args != null && argNames != null && surroundWithQuotes != null;
        assert args.size() == argNames.size()
                && surroundWithQuotes.size() == argNames.size();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.size(); i++) {
            if (args.get(i).toString().isEmpty())
                continue;
            // we have the not empty check since there is a case where no params
            // are used when ALL args are optional
            if (i > 0 && sb.length() != 0)
                sb.append(", ");
            sb.append(String.format(
                    surroundWithQuotes.get(i) ? "%s => '%s'" : "%s => %s",
                    argNames.get(i), args.get(i).toString()));
        }
        return sb.toString();
    }

    @Override
    public void updateState(State state) {
        Parameter tableName = params.get(0);
        ((HBaseState) state).addTable(tableName.toString());

        Parameter cfNum = params.get(1);
        int cfNumVal = (int) cfNum.getValue();
        for (int i = 0; i < cfNumVal; i++) {
            Parameter cfName = params.get(2 + i * 12);
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

    @Override
    public boolean mutate(State s) throws Exception {
        // the table name of different create commands should be unique
        this.params.get(0).mutate(s, this);
        return super.mutate(s);
    }

}
