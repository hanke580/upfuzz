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

    @Override
    public int generate(Map<String, String> key2vals,
            Map<String, String> key2type,
            Map<String, String> newkey2vals,
            Map<String, String> newkey2type) {
        Map<String, Object> key2valObj = new HashMap<>();
        if (key2vals != null) {
            for (String key : key2vals.keySet()) {
                if (!key2type.containsKey(key)) {
                    throw new RuntimeException(
                            String.format("key %s do not have a type"));
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

        Map<String, Object> newkey2valObj = new HashMap<>();
        if (newkey2vals != null) {
            for (String key : newkey2vals.keySet()) {
                if (!newkey2type.containsKey(key)) {
                    throw new RuntimeException(
                            String.format("key %s do not have a type"));
                }
            }

            for (String testConfigKey : newkey2vals.keySet()) {
                String type = newkey2type.get(testConfigKey);
                String testConfigVal = newkey2vals.get(testConfigKey);
                Object testConfigValObj = createConfigValObj(type,
                        testConfigVal);
                newkey2valObj.put(testConfigKey, testConfigValObj);
            }

        }

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

    public void writeYAMLFile(
            Path srcPath, Path savePath, Map<String, Object> key2valObj) {
        // logger.info("parsing yaml file: " + srcPath);

        List<Object> data = new LinkedList<>();
        Iterable<Object> maps = null;
        try {
            maps = yaml.loadAll(new FileInputStream(String.valueOf(srcPath)));
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (maps != null) {

            for (Map.Entry<String, Object> entry : key2valObj.entrySet()) {
                for (Object o : maps) {
                    LinkedHashMap<Object, Object> propertyList = (LinkedHashMap<Object, Object>) o;
                    boolean status = iterateYAML(propertyList, entry.getKey(),
                            entry.getValue());
                    if (!status) {
                        propertyList.put(entry.getKey(), entry.getValue());
                    }
                    data.add(o);
                }
            }
        }
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

    public void writeYAMLFile(Path srcPath, Path savePath) {
        logger.info("parsing yaml file: " + srcPath);

        List<Object> data = new LinkedList<>();
        Iterable<Object> maps = null;
        try {
            maps = yaml.loadAll(new FileInputStream(String.valueOf(srcPath)));
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (maps != null) {
            for (Object o : maps) {
                LinkedHashMap<Object, Object> propertyList = (LinkedHashMap<Object, Object>) o;
                data.add(o);
            }
        }

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

    public Object createConfigValObj(String type, String val) {

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
}
