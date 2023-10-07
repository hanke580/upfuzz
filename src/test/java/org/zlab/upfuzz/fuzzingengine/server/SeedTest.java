package org.zlab.upfuzz.fuzzingengine.server;

import org.junit.jupiter.api.Test;
import org.zlab.upfuzz.AbstractTest;
import org.zlab.upfuzz.CommandPool;
import org.zlab.upfuzz.cassandra.CassandraCommandPool;
import org.zlab.upfuzz.cassandra.CassandraState;
import org.zlab.upfuzz.fuzzingengine.Config;

public class SeedTest extends AbstractTest {
    @Test
    public void testComparator() {
        Config.instance.system = "cassandra";
        CommandPool commandPool = new CassandraCommandPool();
        Seed seed1 = generateSeed(commandPool, CassandraState.class, -1);
        Seed seed2 = generateSeed(commandPool, CassandraState.class, -1);

        PriorityCorpus priorityCorpus = new PriorityCorpus();
        priorityCorpus.addSeed(seed1);
        priorityCorpus.addSeed(seed2);

        assert priorityCorpus.getSeed().equals(seed1);
        assert priorityCorpus.getSeed().equals(seed2);
    }
}
