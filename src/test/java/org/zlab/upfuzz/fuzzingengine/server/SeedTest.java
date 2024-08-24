package org.zlab.upfuzz.fuzzingengine.server;

import org.junit.jupiter.api.Test;
import org.zlab.upfuzz.AbstractTest;
import org.zlab.upfuzz.CommandPool;
import org.zlab.upfuzz.cassandra.CassandraCommandPool;
import org.zlab.upfuzz.cassandra.CassandraState;
import org.zlab.upfuzz.fuzzingengine.Config;

public class SeedTest extends AbstractTest {
    /**
     * Be careful. Any tests that added to seed could mess up the disk corpus.
     */
    // @Test
    public void testComparator() {
        Config.instance.system = "cassandra";
        CommandPool commandPool = new CassandraCommandPool();
        Seed seed1 = generateSeed(commandPool, CassandraState.class, -1);
        Seed seed2 = generateSeed(commandPool, CassandraState.class, -1);

        CorpusDefault corpus = new CorpusDefault();
        Config.instance.saveCorpusToDisk = false;
        corpus.addSeed(seed1, false, false, true, false, false);
        corpus.addSeed(seed2, false, false, true, false, false);

        assert corpus.getSeed().equals(seed1);
        assert corpus.getSeed().equals(seed2);
    }
}
