package org.zlab.upfuzz.fuzzingengine.configgen;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.zlab.upfuzz.cassandra.CassandraConfigGen;
import org.zlab.upfuzz.fuzzingengine.Config;

import java.io.FileNotFoundException;
import java.io.FileReader;

public class ConfigGenTest {
    // Enable testing only when the corresponding system
    // is set up in prebuild/

    // @BeforeAll
    static public void initAll() {
        String configFile = "./config.json";
        Config.Configuration cfg;
        try {
            cfg = new Gson().fromJson(new FileReader(configFile),
                    Config.Configuration.class);
            Config.setInstance(cfg);
        } catch (JsonSyntaxException | JsonIOException
                | FileNotFoundException e) {
            e.printStackTrace();
            assert false;
        }
    }

    // @Test
    public void testYamlGenerator() {
        Config.instance.testCommonConfig = true;
        Config.instance.testAddedConfig = true;
        Config.instance.testDeletedConfig = true;
        ConfigGen configGen = new CassandraConfigGen();
        configGen.generateConfig();
    }
}
