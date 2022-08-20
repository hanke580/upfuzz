package org.zlab.upfuzz.fuzzingengine;

import com.google.gson.Gson;
import junit.framework.TestCase;
import org.junit.jupiter.api.Test;
import org.zlab.upfuzz.fuzzingengine.Config.Configuration;

public class ConfigTest extends TestCase {
    protected void setUp() {
    }

    @Test
    public void testConfigFromJson() {
        String configStr = "{\"clientPort\": 12345, \"jacocoAgentPath\": \"../dependencies/org.jacoco.agent.runtime/org.jacoco.agent.runtime.jar\"}";

        Configuration cfg = new Gson().fromJson(configStr, Configuration.class);
        System.out.println(cfg.toString());
    }
}
