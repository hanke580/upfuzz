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
    static Random rand = new Random();
    ConfigFileGenerator[] configFileGenerator;
    ConfigFileGenerator[] extraGenerator;

    Set<String> commonConfig;
    Set<String> addedConfig;
    Set<String> deletedConfig;

    Set<String> testConfig; // For single version

    Map<String, String> upConfigName2Type;
    Map<String, String> upConfig2Init;
    Map<String, List<String>> upEnumName2ConstantMap;

    Map<String, String> oriConfigName2Type;
    Map<String, String> oriConfig2Init;
    Map<String, List<String>> oriEnumName2ConstantMap;

    ConfigValGenerator commonConfigValGenerator;
    ConfigValGenerator addedConfigValGenerator;
    ConfigValGenerator deletedConfigValGenerator;

    ConfigValGenerator testConfigValGenerator; // For single version

    String hostIP;
    int nodeNum;

    ObjectMapper mapper = new ObjectMapper();

    static Set<String> cassandraConfigBlackList = new HashSet<>();

    static {
        cassandraConfigBlackList
                .add("minimum_replication_factor_warn_threshold");
        cassandraConfigBlackList
                .add("minimum_replication_factor_fail_threshold");
        cassandraConfigBlackList
                .add("user_defined_functions_threads_enabled");

        cassandraConfigBlackList
                .add("concurrent_validations");

    }

    public void initSingleVersion() {
        Path oldVersionPath = Paths.get(System.getProperty("user.dir"),
                "prebuild", Config.getConf().system,
                Config.getConf().originalVersion);

        Path generateFolderPath = Paths.get(System.getProperty("user.dir"),
                Config.getConf().configDir,
                Config.getConf().originalVersion);

        switch (Config.getConf().system) {
        case "cassandra": {
            Path defaultConfigPath = Paths.get(oldVersionPath.toString(),
                    "conf/cassandra.yaml");
            configFileGenerator = new YamlGenerator[1];
            configFileGenerator[0] = new YamlGenerator(defaultConfigPath,
                    generateFolderPath);
            extraGenerator = new PlainTextGenerator[0];
            break;
        }
        case "hdfs": {
            assert false : "hdfs is not tested for single version mode";
            Path defaultConfigPath = Paths.get(oldVersionPath.toString(),
                    "etc/hadoop/hdfs-site.xml");
            configFileGenerator = new XmlGenerator[1];
            configFileGenerator[0] = new XmlGenerator(defaultConfigPath,
                    generateFolderPath);
            extraGenerator = new PlainTextGenerator[0];
            break;
        }
        default:
            assert false : Config.getConf().system +
                    " is not tested for single version mode";
        }

        Path configInfoPath = Paths.get(System.getProperty("user.dir"),
                "configInfo", Config.getConf().originalVersion);
        Path config2typePath = configInfoPath
                .resolve("config2Type.json");
        Path config2initPath = configInfoPath
                .resolve("config2Init.json");
        Path enum2constantPath = configInfoPath
                .resolve("enum2Constant.json");

        try {
            oriConfigName2Type = mapper.readValue(
                    config2typePath.toFile(),
                    HashMap.class);
            oriConfig2Init = mapper.readValue(config2initPath.toFile(),
                    HashMap.class);
            oriEnumName2ConstantMap = mapper
                    .readValue(enum2constantPath.toFile(), HashMap.class);
            testConfig = oriConfig2Init.keySet();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("missing configuration test files!");
        }

        switch (Config.getConf().system) {
        case "cassandra": {
            testConfig = removeBlacklistConfig(testConfig,
                    cassandraConfigBlackList);
            testConfigValGenerator = new ConfigValGenerator(testConfig,
                    oriConfigName2Type, oriConfig2Init,
                    oriEnumName2ConstantMap);
            testConfigValGenerator.constructPairConfig();
            break;
        }
        case "hdfs": {
            testConfigValGenerator = new ConfigValGenerator(testConfig,
                    oriConfigName2Type, oriConfig2Init,
                    oriEnumName2ConstantMap);
            break;
        }
        default: {
            throw new RuntimeException(
                    "configuration is not support yet for system "
                            + Config.getConf().system);
        }
        }
    }

    public void initUpgradeVersion() {
        Path oldVersionPath = Paths.get(System.getProperty("user.dir"),
                "prebuild", Config.getConf().system,
                Config.getConf().originalVersion);
        Path newVersionPath = Paths.get(System.getProperty("user.dir"),
                "prebuild", Config.getConf().system,
                Config.getConf().upgradedVersion);
        Path depSystemPath = null;
        if (Config.getConf().depSystem != null) {
            depSystemPath = Paths.get(System.getProperty("user.dir"),
                    "prebuild", Config.getConf().depSystem,
                    Config.getConf().depVersion);
        }

        Path generateFolderPath = Paths.get(System.getProperty("user.dir"),
                Config.getConf().configDir,
                Config.getConf().originalVersion + "_"
                        + Config.getConf().upgradedVersion);

        switch (Config.getConf().system) {
        case "cassandra": {
            Path defaultConfigPath = Paths.get(oldVersionPath.toString(),
                    "conf/cassandra.yaml");
            Path defaultNewConfigPath = Paths.get(newVersionPath.toString(),
                    "conf/cassandra.yaml");
            configFileGenerator = new YamlGenerator[1];
            configFileGenerator[0] = new YamlGenerator(defaultConfigPath,
                    defaultNewConfigPath, generateFolderPath);
            extraGenerator = new PlainTextGenerator[0];
            break;
        }
        case "hdfs": {
            Path defaultConfigPath = Paths.get(oldVersionPath.toString(),
                    "etc/hadoop/hdfs-site.xml");
            Path defaultNewConfigPath = Paths.get(newVersionPath.toString(),
                    "etc/hadoop/hdfs-site.xml");
            configFileGenerator = new XmlGenerator[1];
            configFileGenerator[0] = new XmlGenerator(defaultConfigPath,
                    defaultNewConfigPath, generateFolderPath);
            extraGenerator = new PlainTextGenerator[0];
            break;
        }
        case "hbase": {
            Path defaultConfigPath = Paths.get(oldVersionPath.toString(),
                    "conf/hbase-site.xml");
            Path defaultNewConfigPath = Paths.get(newVersionPath.toString(),
                    "conf/hbase-site.xml");
            assert depSystemPath != null : "Please provide depSystemPath";
            Path defaultHdfsConfigPath = Paths.get(depSystemPath.toString(),
                    "etc/hadoop/hdfs-site.xml");
            Path defaultHdfsNamenodeConfigPath = Paths.get(
                    depSystemPath.toString(),
                    "etc/hadoop/core-site.xml");
            Path defaultRegionserversPath = Paths.get(oldVersionPath.toString(),
                    "conf/regionservers");
            Path defaultNewRegionserversPath = Paths.get(
                    newVersionPath.toString(),
                    "conf/regionservers");
            configFileGenerator = new XmlGenerator[3];
            configFileGenerator[0] = new XmlGenerator(defaultConfigPath,
                    defaultNewConfigPath, generateFolderPath);
            configFileGenerator[1] = new XmlGenerator(defaultHdfsConfigPath,
                    defaultHdfsConfigPath, generateFolderPath);
            configFileGenerator[2] = new XmlGenerator(
                    defaultHdfsNamenodeConfigPath,
                    defaultHdfsNamenodeConfigPath, generateFolderPath);
            extraGenerator = new PlainTextGenerator[1];
            extraGenerator[0] = new PlainTextGenerator(defaultRegionserversPath,
                    defaultNewRegionserversPath, "regionservers",
                    generateFolderPath);
            break;
        }
        }

        if (!Config.getConf().testCommonConfig
                && !Config.getConf().testAddedConfig) {
            return;
        }

        Path configInfoPath = Paths.get(System.getProperty("user.dir"),
                "configInfo", Config.getConf().originalVersion + "_"
                        + Config.getConf().upgradedVersion);

        Path commonConfigPath = configInfoPath.resolve("commonConfig.json");
        Path addedConfigPath = configInfoPath.resolve("addedClassConfig.json");
        Path deletedConfigPath = configInfoPath
                .resolve("deletedClassConfig.json");

        Path oriConfig2typePath = configInfoPath
                .resolve("oriConfig2Type.json");
        Path oriConfig2initPath = configInfoPath
                .resolve("oriConfig2Init.json");
        Path oriEnum2constantPath = configInfoPath
                .resolve("oriEnum2Constant.json");

        Path upConfig2typePath = configInfoPath
                .resolve("upConfig2Type.json");
        Path upConfig2initPath = configInfoPath
                .resolve("upConfig2Init.json");
        Path upEnum2constantPath = configInfoPath
                .resolve("upEnum2Constant.json");

        try {
            commonConfig = mapper.readValue(commonConfigPath.toFile(),
                    HashSet.class);
            addedConfig = mapper.readValue(addedConfigPath.toFile(),
                    HashSet.class);
            deletedConfig = mapper.readValue(deletedConfigPath.toFile(),
                    HashSet.class);

            oriConfigName2Type = mapper.readValue(
                    oriConfig2typePath.toFile(),
                    HashMap.class);
            oriConfig2Init = mapper.readValue(oriConfig2initPath.toFile(),
                    HashMap.class);
            oriEnumName2ConstantMap = mapper
                    .readValue(oriEnum2constantPath.toFile(), HashMap.class);
            upConfigName2Type = mapper.readValue(
                    upConfig2typePath.toFile(),
                    HashMap.class);
            upConfig2Init = mapper.readValue(upConfig2initPath.toFile(),
                    HashMap.class);
            upEnumName2ConstantMap = mapper
                    .readValue(upEnum2constantPath.toFile(), HashMap.class);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("missing configuration test files!");
        }

        switch (Config.getConf().system) {
        case "cassandra": {
            commonConfig = removeBlacklistConfig(commonConfig,
                    cassandraConfigBlackList);
            addedConfig = removeBlacklistConfig(addedConfig,
                    cassandraConfigBlackList);
            deletedConfig = removeBlacklistConfig(deletedConfig,
                    cassandraConfigBlackList);
            // TODO: if it's common type, we should use init value that
            // are changed between versions
            commonConfigValGenerator = new ConfigValGenerator(commonConfig,
                    oriConfigName2Type, oriConfig2Init,
                    oriEnumName2ConstantMap);
            addedConfigValGenerator = new ConfigValGenerator(addedConfig,
                    upConfigName2Type, upConfig2Init,
                    upEnumName2ConstantMap);
            deletedConfigValGenerator = new ConfigValGenerator(deletedConfig,
                    oriConfigName2Type, oriConfig2Init,
                    oriEnumName2ConstantMap);
            addedConfigValGenerator.constructPairConfig();
            commonConfigValGenerator.constructPairConfig();
            deletedConfigValGenerator.constructPairConfig();
            break;
        }
        case "hdfs": {
            commonConfigValGenerator = new ConfigValGenerator(commonConfig,
                    oriConfigName2Type, oriConfig2Init,
                    oriEnumName2ConstantMap);
            addedConfigValGenerator = new ConfigValGenerator(addedConfig,
                    upConfigName2Type, upConfig2Init,
                    upEnumName2ConstantMap);
            deletedConfigValGenerator = new ConfigValGenerator(deletedConfig,
                    oriConfigName2Type, oriConfig2Init,
                    oriEnumName2ConstantMap);
            break;
        }
        case "hbase": {
            // TODO
            break;
        }
        default: {
            throw new RuntimeException(
                    "configuration is not support yet for system "
                            + Config.getConf().system);
        }
        }
    }

    public ConfigGen() {
        if (Config.getConf().testSingleVersion) {
            initSingleVersion();
        } else {
            initUpgradeVersion();
        }
    }

    public ConfigGen(int nodeNum, String IP) {
        this();
        SetConfig(nodeNum, IP);
    }

    public void SetConfig(int nodeNum, String IP) {
        this.nodeNum = nodeNum;
        this.hostIP = IP;
        for (int i = 0; i < configFileGenerator.length; i++) {
            configFileGenerator[i].SetConfig(nodeNum, IP);
        }
        for (int i = 0; i < extraGenerator.length; i++) {
            extraGenerator[i].SetConfig(nodeNum, IP);
        }
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
                oriConfigName2Type);
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
                oriConfigName2Type,
                upConfigtest, upConfigName2Type);
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
