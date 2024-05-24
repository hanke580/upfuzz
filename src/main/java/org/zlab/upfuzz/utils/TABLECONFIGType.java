package org.zlab.upfuzz.utils;

import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseState;

import java.util.ArrayList;
import java.util.Random;

import static org.zlab.upfuzz.hbase.HBaseCommand.*;

public class TABLECONFIGType extends ParameterType.ConcreteType {

    private ArrayList<ArrayList<Parameter>> params;

    public TABLECONFIGType() {
        this.params = new ArrayList<>();
    }

    public static final TABLECONFIGType instance = new TABLECONFIGType();

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
        Parameter numOptions = new INTType(1, 7)
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
        ArrayList<Parameter> current;
//        sb.append("{ ");
        int idx;
        Parameter configOption;
        for (int i = 0; i < Integer.parseInt(numFilters.toString()); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            current = this.params.get(i + 1);
            idx = 0;
            configOption = current.get(idx++);
            switch (configOption.toString()) {
            case "MAX_FILESIZE":
                Parameter fileSize = current.get(idx++);
                sb.append(String.format("MAX_FILESIZE => %s",
                        fileSize.toString()));
                break;
            case "MEMSTORE_FLUSHSIZE":
                Parameter flushSize = current.get(idx++);
                sb.append(String.format("MEMSTORE_FLUSHSIZE => %s",
                        flushSize.toString()));
                break;
            case "NORMALIZER_TARGET_REGION_COUNT":
                Parameter targetRegionCount = current.get(idx++);
                sb.append(String.format("NORMALIZER_TARGET_REGION_COUNT => %s",
                        targetRegionCount.toString()));
                break;
            case "NORMALIZER_TARGET_REGION_SIZE_MB":
                Parameter targetRegionSize = current.get(idx++);
                sb.append(
                        String.format("NORMALIZER_TARGET_REGION_SIZE_MB => %s",
                                targetRegionSize.toString()));
                break;
            case "DURABILITY":
                Parameter durability = current.get(idx++);
                sb.append(String.format("DURABILITY => '%s'",
                        durability.toString()));
                break;
            case "READONLY":
            case "NORMALIZATION_ENABLED":
                Parameter trueFalse = current.get(idx++);
                sb.append(String.format(
                        "%s => '%s'",
                        configOption.toString(), trueFalse.toString()));
                break;
            default:
//                throw new IllegalArgumentException(
//                        "got: " + configOption.toString());
                return "";
            }
        }
//        sb.append(" }");
        return sb.toString();
    }

    private ArrayList<Parameter> generateSingleOption(HBaseState state,
            Command c) {
        ArrayList<Parameter> current = new ArrayList<>();
        Parameter configOption = new InCollectionType(
                CONSTANTSTRINGType.instance,
                (s, c_) -> Utilities
                        .strings2Parameters(
                                TABLE_CONFIG_OPTIONS),
                null).generateRandomParameter(state, c);
        current.add(configOption);

        switch (configOption.toString()) {
        case "MAX_FILESIZE":
            Parameter fileSize = new INTType(2097152, 1073741824)
                    .generateRandomParameter(state, c);
            current.add(fileSize);
            break;
        case "MEMSTORE_FLUSHSIZE":
            Parameter flushSize = new INTType(1048576, 16777216)
                    .generateRandomParameter(state, c);
            current.add(flushSize);
            break;
        case "NORMALIZER_TARGET_REGION_COUNT":
            Parameter targetRegionCount = new INTType(10, 200)
                    .generateRandomParameter(state, c);
            current.add(targetRegionCount);
            break;
        case "NORMALIZER_TARGET_REGION_SIZE_MB":
            Parameter targetRegionSize = new INTType(1, 1000)
                    .generateRandomParameter(state, c);
            current.add(targetRegionSize);
            break;
        case "DURABILITY":
            Parameter durability = new InCollectionType(
                    CONSTANTSTRINGType.instance,
                    (s, c_) -> Utilities
                            .strings2Parameters(
                                    DURABILITY_TYPES),
                    null).generateRandomParameter(state, c);
            current.add(durability);
            break;
        case "READONLY":
        case "NORMALIZATION_ENABLED":
            Parameter trueFalse = new InCollectionType(
                    CONSTANTSTRINGType.instance,
                    (s, c_) -> Utilities
                            .strings2Parameters(
                                    CREATE_CONFIG_OPTIONS_BOOLEAN_TYPES),
                    null).generateRandomParameter(state, c);
            current.add(trueFalse);
            break;
        default:
            throw new IllegalArgumentException(
                    "got: " + configOption.toString());
        }
        return current;
    }
}