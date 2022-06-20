package org.zlab.upfuzz.cassandra;

import java.net.URL;
import java.net.URLClassLoader;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.fuzzingengine.Config.Configuration;

public class CassandraDockerTest {

    static CassandraExecutor executor;

    @BeforeAll
    static public void initAll() {
        Configuration config = new Configuration();
        Config.setInstance(config);
        executor = new CassandraExecutor();
    }

    @Test
    public void testBuildDocker() {
        executor.buildDocker("3.11.13");
    }
}
