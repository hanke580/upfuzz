package org.zlab.upfuzz.fuzzingengine;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import junit.framework.TestCase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.zlab.upfuzz.cassandra.CassandraConfigGen;
import org.zlab.upfuzz.fuzzingengine.Config.Configuration;
import org.zlab.upfuzz.fuzzingengine.configgen.ConfigGen;

import java.io.FileNotFoundException;
import java.io.FileReader;

public class ConfigTest extends TestCase {
    private static final Logger logger = LogManager.getLogger(ConfigTest.class);

    @BeforeAll
    static public void initAll() {
        String configFile = "./config.json";
        Configuration cfg;
        try {
            cfg = new Gson().fromJson(new FileReader(configFile),
                    Configuration.class);
            Config.setInstance(cfg);
        } catch (JsonSyntaxException | JsonIOException
                | FileNotFoundException e) {
            e.printStackTrace();
            assert false;
        }
    }

    @Test
    public void testConfigFromJson() {
        String configStr = "{\"clientPort\": 12345, \"jacocoAgentPath\": \"../dependencies/org.jacoco.agent.runtime/org.jacoco.agent.runtime.jar\"}";

        Configuration cfg = new Gson().fromJson(configStr, Configuration.class);
        System.out.println(cfg.toString());
    }

    @Test
    public void testConfigGen() {
        if (true)
            return;
        logger.info(Config.getConf().upgradedVersion);
        ConfigGen configGen = new CassandraConfigGen();
        configGen.generateConfig();
    }
}
