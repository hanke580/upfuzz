package org.zlab.upfuzz.fuzzingengine.configgen;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.zlab.upfuzz.cassandra.CassandraConfigValGenerator;
import org.zlab.upfuzz.fuzzingengine.Config;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public abstract class ConfigGen {
    static Random rand = new Random();
    ObjectMapper mapper = new ObjectMapper();

    public ConfigFileGenerator[] configFileGenerator;
    public ConfigFileGenerator[] extraGenerator;

    public Path oldVersionPath;
    public Path newVersionPath;
    public Path depSystemPath;
    public Path generateFolderPath;
    public final Set<String> configBlackList = new HashSet<>();

    // upgrade version testing
    public Set<String> commonConfig;
    public ConfigValGenerator commonConfigValGenerator;
    public Set<String> addedConfig;
    public ConfigValGenerator addedConfigValGenerator;
    public Set<String> deletedConfig;
    public ConfigValGenerator deletedConfigValGenerator;
    public ConfigInfo oriConfigInfo;
    public ConfigInfo upConfigInfo;

    // single version testing
    public Set<String> testConfig;
    public ConfigValGenerator testConfigValGenerator;

    public ConfigGen() {
        initConfigInfo();
        initFileGenerator();
        initValGenerator();
    }

    public abstract void updateConfigBlackList();

    public abstract void initFileGenerator();

    public void initValGenerator() {
        commonConfigValGenerator = new ConfigValGenerator(commonConfig,
                oriConfigInfo);
        addedConfigValGenerator = new ConfigValGenerator(addedConfig,
                upConfigInfo);
        deletedConfigValGenerator = new ConfigValGenerator(
                deletedConfig,
                oriConfigInfo);
    }

    // public void initSingleVersion() {
    // Path oldVersionPath = Paths.get(System.getProperty("user.dir"),
    // "prebuild", Config.getConf().system,
    // Config.getConf().originalVersion);
    //
    // Path generateFolderPath = Paths.get(System.getProperty("user.dir"),
    // Config.getConf().configDir,
    // Config.getConf().originalVersion);
    //
    // switch (Config.getConf().system) {
    // case "cassandra": {
    // Path defaultConfigPath = Paths.get(oldVersionPath.toString(),
    // "conf/cassandra.yaml");
    // configFileGenerator = new YamlGenerator[1];
    // configFileGenerator[0] = new YamlGenerator(defaultConfigPath,
    // generateFolderPath);
    // extraGenerator = new PlainTextGenerator[0];
    // break;
    // }
    // default:
    // assert false : Config.getConf().system +
    // " is not tested for single version mode";
    // }
    //
    // Path configInfoPath = Paths.get(System.getProperty("user.dir"),
    // "configInfo", Config.getConf().originalVersion);
    // Path config2typePath = configInfoPath
    // .resolve("config2Type.json");
    // Path config2initPath = configInfoPath
    // .resolve("config2Init.json");
    // Path enum2constantPath = configInfoPath
    // .resolve("enum2Constant.json");
    //
    // try {
    // oriConfigName2Type = mapper.readValue(
    // config2typePath.toFile(),
    // HashMap.class);
    // oriConfig2Init = mapper.readValue(config2initPath.toFile(),
    // HashMap.class);
    // oriEnumName2ConstantMap = mapper
    // .readValue(enum2constantPath.toFile(), HashMap.class);
    // testConfig = oriConfig2Init.keySet();
    // } catch (IOException e) {
    // e.printStackTrace();
    // throw new RuntimeException("missing configuration test files!");
    // }
    //
    // switch (Config.getConf().system) {
    // case "cassandra": {
    // testConfig = removeBlacklistConfig(testConfig,
    // cassandraConfigBlackList);
    // testConfigValGenerator = new ConfigValGenerator(testConfig,
    // oriConfigName2Type, oriConfig2Init,
    // oriEnumName2ConstantMap);
    // testConfigValGenerator.constructPairConfig();
    // break;
    // }
    // case "hdfs": {
    // testConfigValGenerator = new ConfigValGenerator(testConfig,
    // oriConfigName2Type, oriConfig2Init,
    // oriEnumName2ConstantMap);
    // break;
    // }
    // default: {
    // throw new RuntimeException(
    // "configuration is not support yet for system "
    // + Config.getConf().system);
    // }
    // }
    // }

    public void initConfigInfo() {
        oldVersionPath = Paths.get(System.getProperty("user.dir"),
                "prebuild", Config.getConf().system,
                Config.getConf().originalVersion);
        newVersionPath = Paths.get(System.getProperty("user.dir"),
                "prebuild", Config.getConf().system,
                Config.getConf().upgradedVersion);
        depSystemPath = null;
        if (Config.getConf().depSystem != null) {
            depSystemPath = Paths.get(System.getProperty("user.dir"),
                    "prebuild", Config.getConf().depSystem,
                    Config.getConf().depVersion);
        }
        generateFolderPath = Paths.get(System.getProperty("user.dir"),
                Config.getConf().configDir,
                Config.getConf().originalVersion + "_"
                        + Config.getConf().upgradedVersion);
        Path configInfoPath = Paths.get(System.getProperty("user.dir"),
                "configInfo", Config.getConf().originalVersion + "_"
                        + Config.getConf().upgradedVersion);
        Path commonConfigPath = configInfoPath.resolve("commonConfig.json");
        Path addedConfigPath = configInfoPath.resolve("addedClassConfig.json");
        Path deletedConfigPath = configInfoPath
                .resolve("deletedClassConfig.json");
        try {
            commonConfig = mapper.readValue(commonConfigPath.toFile(),
                    HashSet.class);
            addedConfig = mapper.readValue(addedConfigPath.toFile(),
                    HashSet.class);
            deletedConfig = mapper.readValue(deletedConfigPath.toFile(),
                    HashSet.class);
            oriConfigInfo = ConfigInfo.constructConfigInfo(
                    configInfoPath.resolve("oriConfig2Type.json"),
                    configInfoPath.resolve("oriConfig2Init.json"),
                    configInfoPath.resolve("oriEnum2Constant.json"));
            upConfigInfo = ConfigInfo.constructConfigInfo(
                    configInfoPath.resolve("upConfig2Type.json"),
                    configInfoPath.resolve("upConfig2Init.json"),
                    configInfoPath.resolve("upEnum2Constant.json"));
        } catch (IOException e) {
            throw new RuntimeException("missing configuration test files!" + e);
        }
        // update config black list
        removeBlackListConfigs();
    }

    public void removeBlackListConfigs() {
        commonConfig = removeBlacklistConfig(commonConfig,
                configBlackList);
        addedConfig = removeBlacklistConfig(addedConfig,
                configBlackList);
        deletedConfig = removeBlacklistConfig(deletedConfig,
                configBlackList);
    }

    public static Set<String> removeBlacklistConfig(Set<String> configs,
            Set<String> configBlackList) {
        Set<String> ret = new HashSet<>();
        for (String config : configs) {
            if (!configBlackList.contains(config)) {
                ret.add(config);
            }
        }
        return ret;
    }

    public int generateConfig() {
        if (Config.getConf().testSingleVersion) {
            return generateSingleVersionConfig();
        } else {
            return generateUpgradeVersionConfig();
        }
    }

    public int generateSingleVersionConfig() {
        Map<String, String> oriConfigtest = new HashMap<>();
        if (Config.getConf().testConfig) {
            Map<String, String> filteredConfigTest = filteredConfigTestGen(
                    testConfigValGenerator, true,
                    Config.getConf().testSingleVersionConfigRatio);
            oriConfigtest.putAll(filteredConfigTest);
        }

        for (int i = 1; i < configFileGenerator.length; i++) {
            configFileGenerator[i].generate(
                    new LinkedHashMap<>(), new LinkedHashMap<>(),
                    new LinkedHashMap<>(), new LinkedHashMap<>());
        }
        for (int i = 0; i < extraGenerator.length; i++) {
            extraGenerator[i].generate(
                    new LinkedHashMap<>(), new LinkedHashMap<>(),
                    new LinkedHashMap<>(), new LinkedHashMap<>());
        }
        return configFileGenerator[0].generate(oriConfigtest,
                oriConfigInfo.config2type);
    }

    public int generateUpgradeVersionConfig() {
        Map<String, String> oriConfigtest = new HashMap<>();
        Map<String, String> upConfigtest = new HashMap<>();

        if (Config.getConf().testCommonConfig) {
            Map<String, String> filteredConfigTest = filteredConfigTestGen(
                    commonConfigValGenerator, true,
                    Config.getConf().testUpgradeConfigRatio);
            oriConfigtest.putAll(filteredConfigTest);
            upConfigtest.putAll(filteredConfigTest);
        }

        if (Config.getConf().testAddedConfig) {
            Map<String, String> filteredConfigTest = filteredConfigTestGen(
                    addedConfigValGenerator, true,
                    Config.getConf().testUpgradeConfigRatio);
            upConfigtest.putAll(filteredConfigTest);
        }

        if (Config.getConf().testDeletedConfig) {
            Map<String, String> filteredConfigTest = filteredConfigTestGen(
                    deletedConfigValGenerator, true,
                    Config.getConf().testUpgradeConfigRatio);
            oriConfigtest.putAll(filteredConfigTest);
        }

        for (int i = 1; i < configFileGenerator.length; i++) {
            configFileGenerator[i].generate(
                    new LinkedHashMap<>(), new LinkedHashMap<>(),
                    new LinkedHashMap<>(), new LinkedHashMap<>());
        }
        for (int i = 0; i < extraGenerator.length; i++) {
            extraGenerator[i].generate(
                    new LinkedHashMap<>(), new LinkedHashMap<>(),
                    new LinkedHashMap<>(), new LinkedHashMap<>());
        }
        return configFileGenerator[0].generate(oriConfigtest,
                oriConfigInfo.config2type,
                upConfigtest, upConfigInfo.config2type);
    }

    static Map<String, String> filteredConfigTestGen(
            ConfigValGenerator configValGenerator,
            boolean shrinkSize, double filterRatio) {
        Map<String, String> filteredConfigTest = new HashMap<>();
        Map<String, String> configTest = configValGenerator
                .generateValues(shrinkSize);
        Map<String, String> addedConfigPairTest = configValGenerator
                .generatePairValues(shrinkSize);
        for (String key : configTest.keySet()) {
            if (rand.nextDouble() < filterRatio) {
                filteredConfigTest.put(key, configTest.get(key));
            }
        }
        for (String key : configValGenerator.pairConfigs.keySet()) {
            if (addedConfigPairTest.containsKey(key)) {
                if (rand.nextDouble() < filterRatio) {
                    filteredConfigTest.put(key,
                            addedConfigPairTest.get(key));
                    String pairConfig = configValGenerator.pairConfigs
                            .get(key);
                    filteredConfigTest.put(pairConfig,
                            addedConfigPairTest.get(pairConfig));
                }
            }
        }
        return filteredConfigTest;
    }
}
