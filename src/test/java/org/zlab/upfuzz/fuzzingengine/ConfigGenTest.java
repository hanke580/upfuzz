package org.zlab.upfuzz.fuzzingengine;

import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.zlab.upfuzz.cassandra.CassandraConfigGen;
import org.zlab.upfuzz.fuzzingengine.configgen.ConfigGen;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigGenTest {

    @BeforeAll
    public static void setUp() throws FileNotFoundException {
        Path configPath = Paths.get("config.json");
        File configFile = configPath.toFile();
        Config.Configuration cfg = new Gson().fromJson(
                new FileReader(configFile), Config.Configuration.class);
        Config.setInstance(cfg);

        Config.instance.testAddedConfig = true;
        Config.instance.testCommonConfig = true;
        Config.instance.testDeletedConfig = true;
    }

    @Test
    public void test() {
        Config.instance.testSingleVersion = true;
        ConfigGen configGen = new CassandraConfigGen();
        configGen.generateConfig();
    }
}
