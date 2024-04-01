package org.zlab.upfuzz.fuzzingengine.server;

import org.junit.jupiter.api.Test;
import org.zlab.upfuzz.AbstractTest;
import org.zlab.upfuzz.CommandPool;
import org.zlab.upfuzz.cassandra.CassandraCommandPool;
import org.zlab.upfuzz.cassandra.CassandraState;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.fuzzingengine.server.Corpus.QueueType;

public class SeedTest extends AbstractTest {
    @Test
    public void testComparator() {
        Config.instance.system = "cassandra";
        CommandPool commandPool = new CassandraCommandPool();
        Seed seed1 = generateSeed(commandPool, CassandraState.class, -1);
        Seed seed2 = generateSeed(commandPool, CassandraState.class, -1);

        Corpus corpus = new Corpus();
        corpus.addSeed(seed1, QueueType.BRANCH_COVERAGE);
        corpus.addSeed(seed2, QueueType.BRANCH_COVERAGE);

        assert corpus.getSeed().equals(seed1);
        assert corpus.getSeed().equals(seed2);
    }
}
