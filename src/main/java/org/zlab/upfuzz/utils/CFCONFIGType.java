package org.zlab.upfuzz.utils;

import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseState;

import java.util.ArrayList;
import java.util.Random;

import static org.zlab.upfuzz.hbase.HBaseCommand.*;

/*
     *
     * TODO:
    CONFIGURATION => {
         'hbase.store.file-tracker.impl' => 'FILE', // FILE is the only options? https://blog.cloudera.com/unlocking-hbase-on-s3-with-the-new-store-file-tracking-feature/
         'hbase.hregion.scan.loadColumnFamiliesOnDemand' => 'true',
         // default (below) is 10, possibly 60, 100: https://github.com/apache/hbase/blob/a16f45811ec54ce3ede229579177151675781862/src/main/asciidoc/_chapters/architecture.adoc#L2084
         'hbase.hstore.blockingStoreFiles' => '10',
         'hbase.acl.sync.to.hdfs.enable' => 'true',
         {CONFIGURATION => {'hbase.regionserver.region.split_restriction.type' => 'KeyPrefix',
         'hbase.regionserver.region.split_restriction.prefix_length' => '2'}}
         {CONFIGURATION => {'hbase.regionserver.region.split_restriction.type' => 'DelimitedKeyPrefix',
         'hbase.regionserver.region.split_restriction.delimiter' => ','}}
         'hbase.hstore.defaultengine.compactionpolicy.class' => 'org.apache.hadoop.hbase.regionserver.compactions.FIFOCompactionPolicy'
`         "RatioBasedCompactionPolicy"
         https://github.com/apache/hbase/blob/a16f45811ec54ce3ede229579177151675781862/src/main/asciidoc/_chapters/ops_mgt.adoc#L3831
         CONFIGURATION => { 'hbase.rsgroup.name' => group_name }
         CACHE_DATA_IN_L1 => 'true'
     }
 */

public class CFCONFIGType extends ParameterType.ConcreteType {

    private ArrayList<ArrayList<Parameter>> params;

    public CFCONFIGType() {
        this.params = new ArrayList<>();
    }

    public static final CFCONFIGType instance = new CFCONFIGType();

    @Override
    public Parameter generateRandomParameter(State s, Command c, Object init) {
        return generateRandomParameter(s, c);
    }

    @Override
    public Parameter generateRandomParameter(State s, Command c) {
        assert (s instanceof HBaseState);
        init((HBaseState) s, c);
        return new Parameter(this, genString());
    }

    @Override
    public String generateStringValue(Parameter p) {
        return genString();
    }

    @Override
    public boolean isValid(State s, Command c, Parameter p) {
        return true;
    }

    @Override
    public void regenerate(State s, Command c, Parameter p) {
        this.params.clear();
        generateRandomParameter(s, c);
    }

    @Override
    public boolean isEmpty(State s, Command c, Parameter p) {
        return this.params.isEmpty();
    }

    @Override
    public boolean mutate(State s, Command c, Parameter p) {
        // pick a random index
        Parameter numFilters = this.params.get(0).get(1);
        Random random = new Random();
        // we add 1 because params[0] is [table name, number of filters]; the
        // items from [1] to [params.size() - 1] are filters
        int mutateIdx = random.nextInt(Integer.parseInt(numFilters.toString()))
                + 1;
        this.params.set(mutateIdx, generateSingleOption((HBaseState) s, c));
        return true;
    }

    private void init(HBaseState state, Command c) {
        ArrayList<Parameter> metadata = new ArrayList<>();
        Parameter numOptions = new INTType(1, 3)
                .generateRandomParameter(state, c);
        metadata.add(numOptions);
        this.params.add(metadata);
        for (int i = 0; i < Integer.parseInt(numOptions.toString()); i++) {
            this.params.add(generateSingleOption(state, c));
        }
    }

    private String genString() {
        if (this.params.size() == 1) {
            return "";
        }
        Parameter numFilters = this.params.get(0).get(0);
        StringBuilder sb = new StringBuilder();
        Parameter trueFalse, blockingStoreFiles, regionSplitRestriction,
                restrictionDelim;
        Parameter prefixLength, rsGroupName;
        ArrayList<Parameter> current;
        sb.append("{ ");
        int idx;
        Parameter configOption;
//        for (int i = 0; i < Integer.parseInt(numFilters.toString()) ; i++) {
        for (int i = 1; i < this.params.size(); i++) {
            if (i > 1) {
                sb.append(", ");
            }
//            current = this.params.get(i+1);
            current = this.params.get(i);
            idx = 0;
            configOption = current.get(idx++);
            switch (configOption.toString()) {
            case "hbase.store.file-tracker.impl":
                sb.append("'hbase.store.file-tracker.impl' => 'FILE'");
                break; // no choices to make, just one, fixed param
            case "hbase.hregion.scan.loadColumnFamiliesOnDemand":
            case "hbase.acl.sync.to.hdfs.enable":
            case "CACHE_DATA_IN_L1":
                trueFalse = current.get(idx++);
                sb.append(String.format(
                        "'%s' => '%s'",
                        configOption.toString(), trueFalse.toString()));
                break;
            case "hbase.hstore.blockingStoreFiles":
                blockingStoreFiles = current.get(idx++);
                sb.append(String.format(
                        "'hbase.hstore.blockingStoreFiles' => '%s'",
                        blockingStoreFiles.toString()));
                break;
            case "hbase.regionserver.region.split_restriction.type":
                regionSplitRestriction = current.get(idx++);
                switch (regionSplitRestriction.toString()) {
                case "KeyPrefix":
                    prefixLength = current.get(idx++);
                    sb.append(String.format(
                            "'hbase.regionserver.region.split_restriction.type' => 'KeyPrefix', "
                                    +
                                    "'hbase.regionserver.region.split_restriction.prefix_length' => '%s'",
                            prefixLength.toString()));
                    break;
                case "DelimitedKeyPrefix":
                    restrictionDelim = current.get(idx++);
                    sb.append(String.format(
                            "'hbase.regionserver.region.split_restriction.type' => 'DelimitedKeyPrefix', "
                                    +
                                    "'hbase.regionserver.region.split_restriction.delimiter' => \"%s\"",
                            restrictionDelim.toString()));
                    break;
                }
                break;
            // can't seem to use this option
            case "hbase.hstore.defaultengine.compactionpolicy.class":
                break;
            case "hbase.rsgroup.name":
                rsGroupName = current.get(idx++);
                sb.append(String.format("'hbase.rsgroup.name' => '%s'",
                        rsGroupName.toString()));
                break;
            default:
                throw new IllegalArgumentException(
                        "got: " + configOption.toString());
            }
        }
        sb.append(" }");
        return sb.toString();
    }

    private ArrayList<Parameter> generateSingleOption(HBaseState state,
            Command c) {
        ArrayList<Parameter> current = new ArrayList<>();
        Parameter trueFalse, blockingStoreFiles, regionSplitRestriction,
                restrictionDelim;
        Parameter prefixLength, rsGroupName;
        ConcreteType valueType;
        Parameter configOption = new InCollectionType(
                CONSTANTSTRINGType.instance,
                (s, c_) -> Utilities
                        .strings2Parameters(
                                CREATE_CONFIG_OPTIONS),
                null).generateRandomParameter(state, c);
        current.add(configOption);

        switch (configOption.toString()) {
        case "hbase.store.file-tracker.impl":
            break; // no choices to make, just one, fixed param
        case "hbase.hregion.scan.loadColumnFamiliesOnDemand":
        case "hbase.acl.sync.to.hdfs.enable":
        case "CACHE_DATA_IN_L1":
            trueFalse = new InCollectionType(
                    CONSTANTSTRINGType.instance,
                    (s, c_) -> Utilities
                            .strings2Parameters(
                                    CREATE_CONFIG_OPTIONS_BOOLEAN_TYPES),
                    null).generateRandomParameter(state, c);
            current.add(trueFalse);
            break;
        case "hbase.hstore.blockingStoreFiles":
            blockingStoreFiles = new INTType(1, 100)
                    .generateRandomParameter(state, c);
            current.add(blockingStoreFiles);
            break;
        case "hbase.regionserver.region.split_restriction.type":
            regionSplitRestriction = new InCollectionType(
                    CONSTANTSTRINGType.instance,
                    (s, c_) -> Utilities
                            .strings2Parameters(
                                    CREATE_CONFIG_REGION_SPLIT_RESTRICTION_TYPES),
                    null).generateRandomParameter(state, c);
            current.add(regionSplitRestriction);
            switch (regionSplitRestriction.toString()) {
            case "KeyPrefix":
                prefixLength = new INTType(1, 4)
                        .generateRandomParameter(state, c);
                current.add(prefixLength);
                break;
            case "DelimitedKeyPrefix":
                restrictionDelim = new InCollectionType(
                        CONSTANTSTRINGType.instance,
                        (s, c_) -> Utilities
                                .strings2Parameters(
                                        CREATE_REGION_SPLIT_RESTRICTION_DELIMS),
                        null).generateRandomParameter(state, c);
                current.add(restrictionDelim);
                break;
            }
            break;
        // can't seem to use this option
        case "hbase.hstore.defaultengine.compactionpolicy.class":
            break;
        case "hbase.rsgroup.name":
            valueType = new ParameterType.NotEmpty(new STRINGType(3));
            rsGroupName = valueType.generateRandomParameter(state, c);
            current.add(rsGroupName);
            break;
        default:
            throw new IllegalArgumentException(
                    "got: " + configOption.toString());
        }
        return current;
    }
}
