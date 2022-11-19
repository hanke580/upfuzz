package org.zlab.upfuzz.fuzzingengine.configgen;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.fuzzingengine.configgen.xml.XmlGenerator;
import org.zlab.upfuzz.fuzzingengine.configgen.yaml.YamlGenerator;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ConfigGen {
    Random rand = new Random();
    ConfigFileGenerator configFileGenerator;

    Set<String> commonConfig;
    Map<String, String> commonConfigName2Type;
    Map<String, String> commonConfig2Init;
    Map<String, List<String>> commonEnumName2ConstantMap;

    Set<String> addedConfig;
    Map<String, String> addedConfigName2Type;
    Map<String, String> addedConfig2Init;
    Map<String, List<String>> addedEnumName2ConstantMap;

    ConfigValGenerator commonConfigValGenerator;
    ConfigValGenerator addedConfigValGenerator;

    ObjectMapper mapper = new ObjectMapper();

    static Set<String> cassandraConfigBlackList = new HashSet<>();

    static {
        cassandraConfigBlackList
                .add("minimum_replication_factor_warn_threshold");
        cassandraConfigBlackList
                .add("minimum_replication_factor_fail_threshold");
        cassandraConfigBlackList
                .add("user_defined_functions_threads_enabled");
    }

    public ConfigGen() {
        Path oldVersionPath = Paths.get(System.getProperty("user.dir"),
                "prebuild", Config.getConf().system,
                Config.getConf().originalVersion);
        Path newVersionPath = Paths.get(System.getProperty("user.dir"),
                "prebuild", Config.getConf().system,
                Config.getConf().upgradedVersion);

        Path generateFolderPath = Paths.get(System.getProperty("user.dir"),
                Config.getConf().configDir,
                Config.getConf().originalVersion + "_"
                        + Config.getConf().upgradedVersion);
        Path configInfoPath = Paths.get(System.getProperty("user.dir"),
                "configInfo", Config.getConf().originalVersion + "_"
                        + Config.getConf().upgradedVersion);

        Path addedConfigPath = configInfoPath.resolve("addedClassConfig.json");
        Path addedConfig2typePath = configInfoPath
                .resolve("addedClassConfig2Type.json");
        Path addedConfig2initPath = configInfoPath
                .resolve("addedClassConfig2Init.json");
        Path addedEnum2constantPath = configInfoPath
                .resolve("addedClassEnum2Constant.json");

        Path commonConfigPath = configInfoPath.resolve("commonConfig.json");
        Path commonConfig2typePath = configInfoPath
                .resolve("commonConfig2Type.json");
        Path commonConfig2initPath = configInfoPath
                .resolve("commonConfig2Init.json");
        Path commonEnum2constantPath = configInfoPath
                .resolve("commonEnum2Constant.json");

        try {
            commonConfig = mapper.readValue(commonConfigPath.toFile(),
                    HashSet.class);
            commonConfigName2Type = mapper.readValue(
                    commonConfig2typePath.toFile(),
                    HashMap.class);
            commonConfig2Init = mapper.readValue(commonConfig2initPath.toFile(),
                    HashMap.class);
            commonEnumName2ConstantMap = mapper
                    .readValue(commonEnum2constantPath.toFile(), HashMap.class);

            addedConfig = mapper.readValue(addedConfigPath.toFile(),
                    HashSet.class);
            addedConfigName2Type = mapper.readValue(
                    addedConfig2typePath.toFile(),
                    HashMap.class);
            addedConfig2Init = mapper.readValue(addedConfig2initPath.toFile(),
                    HashMap.class);
            addedEnumName2ConstantMap = mapper
                    .readValue(addedEnum2constantPath.toFile(), HashMap.class);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("missing configuration test files!");
        }

        switch (Config.getConf().system) {
        case "cassandra": {
            Path defaultConfigPath = Paths.get(oldVersionPath.toString(),
                    "conf/cassandra.yaml");
            Path defaultNewConfigPath = Paths.get(newVersionPath.toString(),
                    "conf/cassandra.yaml");
            configFileGenerator = new YamlGenerator(defaultConfigPath,
                    defaultNewConfigPath, generateFolderPath);

            commonConfig = removeBlacklistConfig(commonConfig,
                    cassandraConfigBlackList);
            commonConfigValGenerator = new ConfigValGenerator(commonConfig,
                    commonConfigName2Type, commonConfig2Init,
                    commonEnumName2ConstantMap);

            addedConfig = removeBlacklistConfig(addedConfig,
                    cassandraConfigBlackList);
            addedConfigValGenerator = new ConfigValGenerator(addedConfig,
                    addedConfigName2Type, addedConfig2Init,
                    addedEnumName2ConstantMap);

            addedConfigValGenerator.constructPairConfig();
            commonConfigValGenerator.constructPairConfig();
            break;
        }
        case "hdfs": {
            Path defaultConfigPath = Paths.get(oldVersionPath.toString(),
                    "etc/hadoop/hdfs-site.xml");
            Path defaultNewConfigPath = Paths.get(newVersionPath.toString(),
                    "etc/hadoop/hdfs-site.xml");
            configFileGenerator = new XmlGenerator(defaultConfigPath,
                    defaultNewConfigPath, generateFolderPath);

            commonConfigValGenerator = new ConfigValGenerator(commonConfig,
                    commonConfigName2Type, commonConfig2Init,
                    commonEnumName2ConstantMap);

            addedConfigValGenerator = new ConfigValGenerator(addedConfig,
                    addedConfigName2Type, addedConfig2Init,
                    addedEnumName2ConstantMap);
            break;
        }
        default: {
            throw new RuntimeException(
                    "configuration is not support yet for system "
                            + Config.getConf().system);
        }
        }
    }

    public int generateConfig() {
        Map<String, String> oriConfigtest = new HashMap<>();
        Map<String, String> upConfigtest = new HashMap<>();
        Map<String, String> oriConfig2Type = new HashMap<>();
        Map<String, String> upConfig2Type = new HashMap<>();

        if (Config.getConf().testCommonConfig) {

            Map<String, String> commonConfigTest = commonConfigValGenerator
                    .generateValues(true);
            Map<String, String> commonPairConfigTest = commonConfigValGenerator
                    .generatePairValues(true);

            Map<String, String> filteredCommonConfigTest = new HashMap<>();
            for (String key : commonConfigTest.keySet()) {
                if (rand.nextDouble() < Config.getConf().testConfigRatio) {
                    filteredCommonConfigTest.put(key,
                            commonConfigTest.get(key));
                }
            }
            for (String key : commonConfigValGenerator.pairConfigs.keySet()) {
                if (commonPairConfigTest.containsKey(key)) {
                    if (rand.nextDouble() < Config.getConf().testConfigRatio) {
                        filteredCommonConfigTest.put(key,
                                commonPairConfigTest.get(key));
                        String pConfig = commonConfigValGenerator.pairConfigs
                                .get(key);
                        filteredCommonConfigTest.put(pConfig,
                                commonPairConfigTest.get(pConfig));
                    }
                }
            }

            oriConfigtest.putAll(filteredCommonConfigTest);
            upConfigtest.putAll(filteredCommonConfigTest);
            oriConfig2Type.putAll(commonConfigName2Type);
            upConfig2Type.putAll(commonConfigName2Type);
        }

        if (Config.getConf().testAddedConfig) {

            Map<String, String> addedConfigTest = addedConfigValGenerator
                    .generateValues(false);
            Map<String, String> addedConfigPairTest = addedConfigValGenerator
                    .generatePairValues(false);

            Map<String, String> filteredCommonConfigTest = new HashMap<>();

            for (String key : addedConfigTest.keySet()) {
                if (rand.nextDouble() < Config.getConf().testConfigRatio) {
                    filteredCommonConfigTest.put(key, addedConfigTest.get(key));
                }
            }
            for (String key : addedConfigValGenerator.pairConfigs.keySet()) {
                if (addedConfigPairTest.containsKey(key)) {
                    if (rand.nextDouble() < Config.getConf().testConfigRatio) {
                        filteredCommonConfigTest.put(key,
                                addedConfigPairTest.get(key));
                        String pConfig = addedConfigValGenerator.pairConfigs
                                .get(key);
                        filteredCommonConfigTest.put(pConfig,
                                addedConfigPairTest.get(pConfig));
                    }
                }
            }
            upConfigtest.putAll(filteredCommonConfigTest);
            upConfig2Type.putAll(addedConfigName2Type);
        }

        return configFileGenerator.generate(oriConfigtest, oriConfig2Type,
                upConfigtest, upConfig2Type);
    }

    static Set<String> removeBlacklistConfig(Set<String> configs,
            Set<String> configBlackList) {
        Set<String> ret = new HashSet<>();
        for (String config : configs) {
            if (!configBlackList.contains(config)) {
                ret.add(config);
            }
        }
        return ret;
    }

}
