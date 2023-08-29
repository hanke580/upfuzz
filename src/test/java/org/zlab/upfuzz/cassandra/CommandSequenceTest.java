package org.zlab.upfuzz.cassandra;

import org.junit.jupiter.api.Test;
import org.zlab.upfuzz.AbstractTest;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.fuzzingengine.server.Seed;

public class CommandSequenceTest extends AbstractTest {
    public static CassandraCommandPool cassandraCommandPool = new CassandraCommandPool();

    @Test
    public void testSequenceGeneration() {
        Config.getConf().system = "cassandra";
        Seed seed = generateSeed(cassandraCommandPool, CassandraState.class,
                -1);
        assert seed != null;
        printSeed(seed);
        boolean status = seed.mutate(cassandraCommandPool,
                CassandraState.class);
        System.out.println("mutate status = " + status);
        printSeed(seed);
    }
}
