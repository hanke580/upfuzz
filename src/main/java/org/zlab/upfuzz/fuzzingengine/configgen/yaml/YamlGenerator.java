package org.zlab.upfuzz.fuzzingengine.configgen.yaml;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.zlab.upfuzz.fuzzingengine.configgen.ConfigFileGenerator;

public class YamlGenerator extends ConfigFileGenerator {
    private static final Logger logger = LogManager
            .getLogger(YamlGenerator.class);

    public Path defaultYAMLPath;
    public Path defaultNewYAMLPath;

    private final Yaml yaml;

    public YamlGenerator(
            Path defaultYAMLPath,
            Path defaultNewYAMLPath,
            Path generateFolderPath) {
        super(generateFolderPath);

        this.defaultYAMLPath = defaultYAMLPath;
        this.defaultNewYAMLPath = defaultNewYAMLPath;

        DumperOptions options = new DumperOptions();
        options.setIndent(2);
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        yaml = new Yaml(options);
    }

    public YamlGenerator(
            Path defaultYAMLPath,
            Path generateFolderPath) {
        super(generateFolderPath);

        this.defaultYAMLPath = defaultYAMLPath;

        DumperOptions options = new DumperOptions();
        options.setIndent(2);
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        yaml = new Yaml(options);
    }

    @Override
    public int generate(Map<String, String> key2vals,
            Map<String, String> key2type,
            Map<String, String> newkey2vals,
            Map<String, String> newkey2type) {
        Map<String, Object> key2valObj = computeKey2ValObj(key2vals, key2type);
        Map<String, Object> newkey2valObj = computeKey2ValObj(newkey2vals,
                newkey2type);

        Path savePath = generateFolderPath
                .resolve(String.format("test%d", fileNameIdx));
        Path oriConfig = savePath.resolve("oriconfig");
        Path upConfig = savePath.resolve("upconfig");
        oriConfig.toFile().mkdirs();
        upConfig.toFile().mkdirs();

        Path oriSavePath = oriConfig.resolve(defaultYAMLPath.getFileName());
        writeYAMLFile(defaultYAMLPath, oriSavePath, key2valObj);

        Path upSavePath = upConfig.resolve(defaultNewYAMLPath.getFileName());
        writeYAMLFile(defaultNewYAMLPath, upSavePath, newkey2valObj);

        return fileNameIdx++;
    }

    @Override
    public int generate(Map<String, String> key2vals,
            Map<String, String> key2type) {
        Map<String, Object> key2valObj = computeKey2ValObj(key2vals, key2type);

        Path savePath = generateFolderPath
                .resolve(String.format("test%d", fileNameIdx));
        Path oriConfig = savePath.resolve("oriconfig");
        oriConfig.toFile().mkdirs();
        Path oriSavePath = oriConfig.resolve(defaultYAMLPath.getFileName());
        writeYAMLFile(defaultYAMLPath, oriSavePath, key2valObj);
        return fileNameIdx++;
    }

    @Override
    public int generate(boolean isUpgrade) {
        Path savePath = generateFolderPath
                .resolve(String.format("test%d", fileNameIdx));
        Path oriConfig = savePath.resolve("oriconfig");
        oriConfig.toFile().mkdirs();
        Path oriSavePath = oriConfig.resolve(defaultYAMLPath.getFileName());
        writeYAMLFile(defaultYAMLPath, oriSavePath);

        if (isUpgrade) {
            Path upConfig = savePath.resolve("upconfig");
            upConfig.toFile().mkdirs();
            Path upSavePath = upConfig
                    .resolve(defaultNewYAMLPath.getFileName());
            writeYAMLFile(defaultNewYAMLPath, upSavePath);
        }
        return fileNameIdx++;
    }

    public void writeYAMLFile(
            Path srcPath, Path savePath, Map<String, Object> key2valObj) {
        // logger.info("parsing yaml file: " + srcPath);

        List<Object> data = new LinkedList<>();
        Iterable<Object> maps = null;
        try {
            maps = yaml.loadAll(new FileInputStream(String.valueOf(srcPath)));
        } catch (IOException e) {
            logger.error("writeYAMLFile failed", e);
        }

        if (maps != null) {

            for (Object o : maps) {
                LinkedHashMap<Object, Object> propertyList = (LinkedHashMap<Object, Object>) o;
                for (Map.Entry<String, Object> entry : key2valObj.entrySet()) {
                    boolean status = iterateYAML(propertyList, entry.getKey(),
                            entry.getValue());
                    if (!status) {
                        propertyList.put(entry.getKey(), entry.getValue());
                    }
                }
                data.add(propertyList);
            }
        }
        dumpYAMLFile(savePath, data);
    }

    public void writeYAMLFile(Path srcPath, Path savePath) {
        // logger.info("parsing yaml file: " + srcPath);

        List<Object> data = new LinkedList<>();
        Iterable<Object> maps = null;
        try {
            maps = yaml.loadAll(new FileInputStream(String.valueOf(srcPath)));
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (maps != null) {
            for (Object o : maps) {
                data.add(o);
            }
        }
        dumpYAMLFile(savePath, data);
    }

    public void dumpYAMLFile(Path savePath, List<Object> data) {
        FileWriter writer;
        try {
            writer = new FileWriter(savePath.toFile());
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("cannot write new configuration files");
        }
        if (data.size() == 1) {
            yaml.dump(data.get(0), writer);
        } else {
            yaml.dump(data, writer);
        }
    }

    public static Object createConfigValObj(String type, String val) {

        if (type.contains(".")) {
            type = type.substring(type.lastIndexOf(".") + 1);
        }

        switch (type) {
        case "Integer":
        case "int": {
            return Integer.parseInt(val);
        }
        case "Double":
        case "double": {
            return Double.parseDouble(val);
        }
        case "Boolean":
        case "boolean": {
            return Boolean.parseBoolean(val);
        }
        case "Long":
        case "long": {
            return Long.parseLong(val);
        }
        default: {
            // logger.info("special type " + type);
            return val;
        }
        }
    }

    public boolean iterateYAML(
            LinkedHashMap<Object, Object> map, String configKey,
            Object configVal) {
        for (Map.Entry<Object, Object> property : map.entrySet()) {
            if (property.getKey() != null) {
                String key = property.getKey().toString();
                if (key.equals(configKey)) {
                    map.put(property.getKey(), configVal);
                    return true;
                }

                if (property.getValue() instanceof List) {
                    List<Object> items = (List<Object>) property.getValue();
                    for (Object item : items) {
                        if (iterateYAML((LinkedHashMap<Object, Object>) item,
                                configKey, configVal))
                            return true;
                    }
                }
            }
        }
        return false;
    }

    public static Map<String, Object> computeKey2ValObj(
            Map<String, String> key2vals, Map<String, String> key2type) {
        Map<String, Object> key2valObj = new HashMap<>();
        if (key2vals != null) {
            for (String key : key2vals.keySet()) {
                if (!key2type.containsKey(key)) {
                    throw new RuntimeException(
                            String.format("key %s do not have a type", key));
                }
            }

            for (String testConfigKey : key2vals.keySet()) {
                String type = key2type.get(testConfigKey);
                String testConfigVal = key2vals.get(testConfigKey);
                Object testConfigValObj = createConfigValObj(type,
                        testConfigVal);
                key2valObj.put(testConfigKey, testConfigValObj);
            }
        }
        return key2valObj;
    }
}
