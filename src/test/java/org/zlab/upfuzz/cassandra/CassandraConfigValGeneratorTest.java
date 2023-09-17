package org.zlab.upfuzz.cassandra;

import org.junit.Before;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.zlab.upfuzz.fuzzingengine.configgen.ConfigInfo;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CassandraConfigValGeneratorTest {
    private static ConfigInfo configInfo;

    @BeforeAll
    public static void setUp() throws IOException {
        // Make sure we maintain the configInfo only for testing
        // NOTE: Update this test if configInfo is also updated
        Path infoPath = Paths.get(
                "configInfo/apache-cassandra-3.11.16_apache-cassandra-4.1.3");
        configInfo = ConfigInfo.constructConfigInfo(
                infoPath.resolve("oriConfig2Type.json"),
                infoPath.resolve("oriConfig2Init.json"),
                infoPath.resolve("oriEnum2Constant.json"));
    }

    @Test
    public void testTimeoutPair() {
        String warnConfig = "user_defined_function_warn_timeout";
        String failConfig = "user_defined_function_fail_timeout";
        testPair(warnConfig, failConfig);
    }

    @Test
    public void testThresholdPair() {
        String warnConfig = "tombstone_warn_threshold";
        String failConfig = "tombstone_failure_threshold";
        testPair(warnConfig, failConfig);
    }

    public void testPair(String warnConfig, String failConfig) {
        Set<String> configs = new HashSet<>();
        configs.add(warnConfig);
        configs.add(failConfig);

        CassandraConfigValGenerator cassandraConfigValGenerator = new CassandraConfigValGenerator(
                configs, configInfo);
        for (int i = 0; i < 10; i++) {
            Map<String, String> config2value = cassandraConfigValGenerator
                    .generatePairValues(true);
            int warnVal = Integer.parseInt(
                    config2value.get(warnConfig));
            int failVal = Integer.parseInt(
                    config2value.get(failConfig));
            assert warnVal <= failVal;
        }
    }
}
