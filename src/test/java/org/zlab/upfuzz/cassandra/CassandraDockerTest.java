package org.zlab.upfuzz.cassandra;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.zlab.upfuzz.AbstractTest;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.fuzzingengine.Config.Configuration;

public class CassandraDockerTest extends AbstractTest {

    static CassandraExecutor executor;

    @BeforeAll
    public static void initExecutor() {
        System.out.println("check2");
        executor = new CassandraExecutor();
    }

}
