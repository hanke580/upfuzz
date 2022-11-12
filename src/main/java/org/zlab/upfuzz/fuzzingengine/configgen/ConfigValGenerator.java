package org.zlab.upfuzz.fuzzingengine.configgen;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class ConfigValGenerator {

    private static final Logger logger = LogManager
            .getLogger(ConfigValGenerator.class);

    public static final Random rand = new Random();

    // Input: configName, configName2Type, configName2Init,
    // Enum: enumName2Constants

    // Output: configName: Set of values we want to test
    // Since the type varies, we need to generate string? or for different
    // types?
    // try string first
    public Set<String> configs;
    public Map<String, String> configName2Type;
    public Map<String, String> config2Init;
    public Map<String, List<String>> enumName2ConstantMap;

    public static final int MAX_INT = 10000;
    public static final double MAX_DOUBLE = 10000.;
    public static final double TEST_NUM = 5;
    public static final double SIZE_TEST_NUM = 5;

    public ConfigValGenerator(
            Set<String> configs,
            Map<String, String> configName2Type,
            Map<String, String> config2Init,
            Map<String, List<String>> enumName2ConstantMap) {
        // Generate values according to type
        this.configs = configs;
        this.configName2Type = configName2Type;
        this.config2Init = config2Init;
        this.enumName2ConstantMap = enumName2ConstantMap;
    }

    public Map<String, String> generateSizeValues() {
        Map<String, String> config2Value = new HashMap<>();

        for (String config : configs) {
            if (!config.toLowerCase().contains("size"))
                continue;

            if (configName2Type.containsKey(config)) {
                String val;
                String type = configName2Type.get(config);
                String initValue = config2Init.get(config);
                if (enumName2ConstantMap != null
                        && enumName2ConstantMap.containsKey(type)) {
                    val = generateValue(type, initValue,
                            enumName2ConstantMap.get(type));
                } else {
                    val = generateSizeValue(type, initValue);
                }
                if (val != null) {
                    config2Value.put(config, val);
                }
            }
        }
        return config2Value;
    }

    public Map<String, String> generateValues() {
        Map<String, String> config2Value = new HashMap<>();
        for (String config : configs) {
            if (configName2Type.containsKey(config)) {
                String val;
                String type = configName2Type.get(config);

                String initValue = config2Init.get(config);

                if (enumName2ConstantMap != null
                        && enumName2ConstantMap.containsKey(type)) {
                    val = generateValue(type, initValue,
                            enumName2ConstantMap.get(type));
                } else {
                    val = generateValue(type, initValue);
                }
                if (val != null) {
                    config2Value.put(config, val);
                }
            }
        }
        return config2Value;
    }

    public String generateSizeValue(String configType, String init) {
        if (configType == null) {
            return null;
        }

        if (configType.contains(".")) {
            configType = configType.substring(configType.lastIndexOf(".") + 1);
        }

        List<String> vals = new LinkedList<>();
        switch (configType) {
        case "int":
        case "Integer":
        case "long":
        case "Long": {
            // generate some int values
            vals.add("0");
            vals.add("1");
            if (init != null) {
                Integer initVal;
                try {
                    initVal = Integer.parseInt(init);
                    // can generate values using default
                    for (int i = 0; i < SIZE_TEST_NUM; i++) {
                        vals.add(String
                                .valueOf(rand.nextInt((int) (0.1 * initVal))));
                    }
                } catch (Exception e) {
                    // cannot use default value
                }
            }
            break;
        }
        case "double": {
            vals.add("0");
            vals.add("1");
            double val = rand.nextDouble();
            Double truncVal = BigDecimal.valueOf(val)
                    .setScale(2, RoundingMode.HALF_UP).doubleValue();
            vals.add(String.valueOf(truncVal));
            if (init != null) {
                Double initVal;
                try {
                    initVal = Double.parseDouble(init);
                    // can generate values using default
                    for (int i = 0; i < SIZE_TEST_NUM; i++) {
                        val = 0.1 * initVal * rand.nextDouble();
                        truncVal = BigDecimal.valueOf(val)
                                .setScale(2, RoundingMode.HALF_UP)
                                .doubleValue();
                        vals.add(String.valueOf(truncVal));
                    }
                } catch (Exception e) {
                    // cannot use default value
                }
            }
            break;
        }
        }

        if (vals.isEmpty()) {
            logger.error("cannot generate value");
            return null;
        }

        int idx = rand.nextInt(vals.size());
        return vals.get(idx);
    }

    private String generateValue(String configType, String init) {
        if (configType == null) {
            return null;
        }

        if (configType.contains(".")) {
            configType = configType.substring(configType.lastIndexOf(".") + 1);
        }

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
            vals.add("0");
            vals.add("1");
            if (init != null) {
                Integer initVal;
                try {
                    initVal = Integer.parseInt(init);
                    // can generate values using default
                    for (int i = 0; i < TEST_NUM; i++) {
                        vals.add(String.valueOf(rand.nextInt(2 * initVal)));
                    }
                } catch (Exception e) {
                    // cannot use default value
                }
            }
            for (int i = 0; i < TEST_NUM; i++) {
                vals.add(String.valueOf(rand.nextInt(MAX_INT)));
            }
            break;
        }
        case "double": {
            vals.add("0");
            vals.add("1");
            if (init != null) {
                Double initVal = null;
                try {
                    initVal = Double.parseDouble(init);
                    // can generate values using default
                    for (int i = 0; i < TEST_NUM; i++) {
                        Double val = 2 * initVal * rand.nextDouble();
                        Double truncVal = BigDecimal.valueOf(val)
                                .setScale(2, RoundingMode.HALF_UP)
                                .doubleValue();
                        vals.add(String.valueOf(truncVal));
                    }
                } catch (Exception e) {
                    // cannot use default value
                }
            }

            for (int i = 0; i < TEST_NUM; i++) {
                Double val = MAX_DOUBLE * rand.nextDouble();
                Double truncVal = BigDecimal.valueOf(val)
                        .setScale(2, RoundingMode.HALF_UP).doubleValue();
                vals.add(String.valueOf(truncVal));
            }
            break;
        }
        case "String": {
            // We might not want to mutate this part?
            // Omit. Randomly generate values for this
            // will likely be invalid.
            break;
        }
        }

        if (vals.isEmpty()) {
            logger.error("cannot gen type = " + configType);
            return null;
        }

        int idx = rand.nextInt(vals.size());
        return vals.get(idx);
    }

    // for enum
    private String generateValue(
            String configType, String Init, List<String> constantMap) {
        assert !constantMap.isEmpty();

        int idx = rand.nextInt(constantMap.size());
        return constantMap.get(idx);
    }
}
