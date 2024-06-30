package org.zlab.upfuzz.fuzzingengine.configgen;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.hbase.HBaseConfigGen;
import org.zlab.upfuzz.utils.Pair;
import org.zlab.upfuzz.utils.Utilities;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class ConfigValGenerator {

    private static final Logger logger = LogManager
            .getLogger(ConfigValGenerator.class);

    public static final Random rand = new Random();

    public static final double SHRINKRATIO = 0.3; // shrink size by 0.3 *
                                                  // size_default

    // Input: configName, configName2Type, configName2Init,
    // Enum: enumName2Constants

    public Set<String> configs;
    ConfigInfo configInfo;

    // (config1, config2) pair configurations
    // Relation: val1 < val2
    public Map<String, String> pairConfigs = new HashMap<>();

    public static final int MAX_INT = 10000;
    public static final double MAX_DOUBLE = 1000.;
    public static final double TEST_NUM = 5;
    public static final double SIZE_TEST_NUM = 5;

    public ConfigValGenerator(
            Set<String> configs,
            ConfigInfo configInfo) {
        // Generate values according to type
        this.configs = configs;
        this.configInfo = configInfo;
        // override this function if pair configs are needed
        constructPairs();
    }

    public void constructPairs() {
    }

    /**
     * If config name contains size, we shrink it, or
     * we generate them as usual
     */
    public Map<String, String> generateValues(boolean shrinkSize) {
        Map<String, String> config2Value = new HashMap<>();

        for (String config : configs) {
            if (configInfo.config2type.containsKey(config)) {
                String val;
                String type = configInfo.config2type.get(config);
                String initValue = configInfo.config2init.get(config);
                if (configInfo.enum2constants != null
                        && configInfo.enum2constants.containsKey(type)) {
                    val = generateValue(configInfo.enum2constants.get(type));
                } else {
                    if (config.toLowerCase().contains("size"))
                        val = generateValue(config, type, initValue,
                                shrinkSize);
                    else
                        val = generateValue(config, type, initValue,
                                shrinkSize);
                }
                if (val != null) {
                    config2Value.put(config, val);
                }
            }
        }
        return config2Value;
    }

    /**
     * Relation between pair configs: val1 < val2
     */
    public Map<String, String> generatePairValues(boolean shrinkSize) {
        Map<String, String> config2Value = new HashMap<>();
        for (String config : pairConfigs.keySet()) {
            if (configInfo.config2type.containsKey(config)) {
                // generate 2 values, assign them
                String val1;
                String val2;

                String type = configInfo.config2type.get(config);
                String initValue = configInfo.config2init.get(config);
                if (configInfo.enum2constants != null
                        && configInfo.enum2constants.containsKey(type)) {
                    val1 = generateValue(configInfo.enum2constants.get(type));
                    val2 = generateValue(configInfo.enum2constants.get(type));
                } else {
                    Pair<String, String> pairVal;
                    if (shrinkSize)
                        pairVal = generatePairValue(config, type, initValue,
                                true);
                    else
                        pairVal = generatePairValue(config, type, initValue,
                                false);
                    if (pairVal != null) {
                        val1 = pairVal.left;
                        val2 = pairVal.right;
                    } else {
                        continue;
                    }
                }
                config2Value.put(config, val1);
                config2Value.put(pairConfigs.get(config), val2);
            }
        }
        return config2Value;
    }

    /**
     * Core function to generate value according
     * to config type
     */
    public String generateValue(String config, String configType, String init,
            boolean shrinkSize) {
        if (configType == null) {
            return null;
        }
        if (configType.contains(".")) {
            configType = configType.substring(configType.lastIndexOf(".") + 1);
        }

        double factor = 2;
        if (shrinkSize)
            factor = SHRINKRATIO;

        List<String> vals = new LinkedList<>();
        switch (configType) {
        case "Boolean":
        case "boolean": {
            vals.add("true");
            vals.add("false");
            break;
        }
        case "int":
        case "Integer":
        case "long":
        case "Long": {
            // generate some int values
            // vals.add("0");
            vals.add("1");
            boolean useDefault = false;
            if (init != null && !init.equals("null")) {
                int initVal;
                try {
                    initVal = Utilities.parseInt(init);
                    if (initVal > 0) {
                        useDefault = true;
                        // can generate values using default
                        for (int i = 0; i < SIZE_TEST_NUM; i++) {
                            vals.add(String
                                    .valueOf(rand.nextInt(
                                            (int) (factor * initVal))));
                        }
                    }
                } catch (Exception e) {
                    // cannot use default value
                }
            }
            if (!useDefault) {
                // cannot use default value
                int maxInt = MAX_INT;
                if (config.contains("percentage")) {
                    // only generate 0-100
                    maxInt = 100;
                }
                for (int i = 0; i < TEST_NUM; i++) {
                    vals.add(String.valueOf(rand.nextInt(maxInt)));
                }
            }
            vals.removeAll(Collections.singleton("0"));
            break;
        }
        case "double": {
            boolean useDefault = false;
            vals.add("1");
            double val = rand.nextDouble();
            Double truncVal = BigDecimal.valueOf(val)
                    .setScale(2, RoundingMode.HALF_UP).doubleValue();
            vals.add(String.valueOf(truncVal));
            if (init != null) {
                double initVal;
                try {
                    initVal = Double.parseDouble(init);
                    for (int i = 0; i < SIZE_TEST_NUM; i++) {
                        val = Utilities.randDouble(rand, 0.,
                                factor * initVal);
                        truncVal = BigDecimal.valueOf(val)
                                .setScale(2, RoundingMode.HALF_UP)
                                .doubleValue();
                        useDefault = true;
                        vals.add(String.valueOf(truncVal));
                    }
                } catch (Exception e) {
                    // cannot use default value
                }
            }
            if (!useDefault) {
                for (int i = 0; i < TEST_NUM; i++) {
                    val = MAX_DOUBLE * rand.nextDouble();
                    truncVal = BigDecimal.valueOf(val)
                            .setScale(2, RoundingMode.HALF_UP).doubleValue();
                    vals.add(String.valueOf(truncVal));
                }
            }
            vals.removeAll(Collections.singleton("0.0"));
            break;
        }
        case "unknown": {
            if (Config.getConf().eval_HBASE22503) {
                if (config.equals("hbase.coprocessor.master.classes")) {
                    vals.addAll(HBaseConfigGen.SubTypeForMasterCoprocessor);
                }
                if (config.equals("hbase.coprocessor.region.classes")) {
                    // Subset of coprocessors
                    vals.addAll(HBaseConfigGen.SubTypeForRegionCoprocessor);
                }
            }
        }
        }

        if (vals.isEmpty()) {
            return null;
        }

        int idx = rand.nextInt(vals.size());
        return vals.get(idx);
    }

    // generate (val1, val2) where val1 < val2
    private Pair<String, String> generatePairValue(String config,
            String configType,
            String init, boolean shrinkSize) {
        if (configType == null) {
            return null;
        }
        double factor = 2;
        if (shrinkSize)
            factor = SHRINKRATIO;
        if (configType.contains(".")) {
            configType = configType.substring(configType.lastIndexOf(".") + 1);
        }

        String val1;
        String val2;

        switch (configType) {
        case "int":
        case "Integer":
        case "long":
        case "Long": {
            List<Integer> vals = new LinkedList<>();
            vals.add(1);
            boolean useDefault = false;
            if (init != null) {
                int initVal;
                try {
                    initVal = Integer.parseInt(init);
                    if (initVal > 0) {
                        useDefault = true;
                        // can generate values using default
                        for (int i = 0; i < TEST_NUM; i++) {
                            vals.add(rand.nextInt((int) (factor * initVal)));
                        }
                    }
                } catch (Exception e) {
                    // cannot use default value
                }
            }
            if (!useDefault) {
                // cannot use default value
                int maxInt = MAX_INT;
                if (config.contains("percentage")) {
                    // only generate 0-100
                    maxInt = 100;
                }
                for (int i = 0; i < TEST_NUM; i++) {
                    vals.add(rand.nextInt(maxInt));
                }
            }

            int val1_ = vals.get(rand.nextInt(vals.size()));
            int val2_ = vals.get(rand.nextInt(vals.size()));
            val1 = val1_ <= val2_ ? String.valueOf(val1_)
                    : String.valueOf(val2_);
            val2 = val1_ <= val2_ ? String.valueOf(val2_)
                    : String.valueOf(val1_);
            break;
        }
        case "double": {
            List<Double> vals = new LinkedList<>();
            boolean useDefault = false;
            vals.add(1.);
            if (init != null) {
                double initVal;
                try {
                    initVal = Double.parseDouble(init);
                    // can generate values using default
                    for (int i = 0; i < TEST_NUM; i++) {
                        Double val = Utilities.randDouble(rand, 0.,
                                factor * initVal);
                        Double truncVal = BigDecimal.valueOf(val)
                                .setScale(2, RoundingMode.HALF_UP)
                                .doubleValue();
                        useDefault = true;
                        vals.add(truncVal);
                    }
                } catch (Exception e) {
                    // cannot use default value
                }
            }
            if (!useDefault) {
                for (int i = 0; i < TEST_NUM; i++) {
                    double val = MAX_DOUBLE * rand.nextDouble();
                    Double truncVal = BigDecimal.valueOf(val)
                            .setScale(2, RoundingMode.HALF_UP).doubleValue();
                    vals.add(truncVal);
                }
            }
            double val1_ = vals.get(rand.nextInt(vals.size()));
            double val2_ = vals.get(rand.nextInt(vals.size()));
            val1 = val1_ <= val2_ ? String.valueOf(val1_)
                    : String.valueOf(val2_);
            val2 = val1_ <= val2_ ? String.valueOf(val2_)
                    : String.valueOf(val1_);
            break;
        }
        default:
            return null;
        }
        return new Pair<>(val1, val2);
    }

    // for enum
    private String generateValue(List<String> constantMap) {
        assert !constantMap.isEmpty();

        int idx = rand.nextInt(constantMap.size());
        return constantMap.get(idx);
    }

}
