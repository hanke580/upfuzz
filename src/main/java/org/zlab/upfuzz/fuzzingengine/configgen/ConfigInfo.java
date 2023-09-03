package org.zlab.upfuzz.fuzzingengine.configgen;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigInfo {
    static ObjectMapper mapper = new ObjectMapper();

    public Map<String, String> config2type = new HashMap<>();
    public Map<String, String> config2init = new HashMap<>();
    public Map<String, List<String>> enum2constants = new HashMap<>();

    public static ConfigInfo constructConfigInfo(Path config2typePath,
            Path config2InitPath,
            Path enum2constantsPath) throws IOException {
        ConfigInfo configInfo = new ConfigInfo();
        configInfo.config2type = mapper.readValue(
                config2typePath.toFile(),
                HashMap.class);
        configInfo.config2init = mapper.readValue(config2InitPath.toFile(),
                HashMap.class);
        configInfo.enum2constants = mapper
                .readValue(enum2constantsPath.toFile(), HashMap.class);
        return configInfo;
    }
}