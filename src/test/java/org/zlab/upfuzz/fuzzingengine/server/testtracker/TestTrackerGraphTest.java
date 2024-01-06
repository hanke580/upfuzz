package org.zlab.upfuzz.fuzzingengine.server.testtracker;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.jupiter.api.Test;
import org.zlab.upfuzz.AbstractTest;
import org.zlab.upfuzz.CommandPool;
import org.zlab.upfuzz.cassandra.CassandraCommandPool;
import org.zlab.upfuzz.cassandra.CassandraState;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.fuzzingengine.server.Seed;

public class TestTrackerGraphTest extends AbstractTest {

    public Seed mutateSeed(Seed seed, CommandPool commandPool,
            Class<? extends CassandraState> stateClass) {
        Seed mutatedSeed = SerializationUtils.clone(seed);
        mutatedSeed.mutate(commandPool, stateClass);
        return mutatedSeed;
    }

    // @Test
    public void testSerialize() {
        TestTrackerGraph graph = new TestTrackerGraph();

        CassandraCommandPool cassandraCommandPool = new CassandraCommandPool();
        Config.getConf().system = "cassandra";
        // Ori seed
        Seed seed = generateSeed(cassandraCommandPool, CassandraState.class,
                -1);
        assert seed != null;
        seed.testID = 0;
        graph.addNode(-1, seed);
        graph.updateNodeCoverage(0, true, false, true);

        Seed mutatedSeed = mutateSeed(seed, cassandraCommandPool,
                CassandraState.class);
        mutatedSeed.testID = 1;
        graph.addNode(seed.testID, mutatedSeed);
        graph.updateNodeCoverage(1, true, false, true);

        mutatedSeed = mutateSeed(seed, cassandraCommandPool,
                CassandraState.class);
        mutatedSeed.testID = 2;
        graph.addNode(seed.testID, mutatedSeed);
        graph.updateNodeCoverage(2, false, false, true);

        mutatedSeed = mutateSeed(seed, cassandraCommandPool,
                CassandraState.class);
        mutatedSeed.testID = 20000;
        graph.addNode(seed.testID, mutatedSeed);
        graph.updateNodeCoverage(20000, false, false, true);

        Seed seed1 = generateSeed(cassandraCommandPool, CassandraState.class,
                -1);
        assert seed1 != null;
        seed1.testID = 1000;
        graph.addNode(-1, seed1);
        graph.updateNodeCoverage(1000, true, false, true);

        mutatedSeed = SerializationUtils.clone(mutatedSeed);
        mutatedSeed.mutate(cassandraCommandPool, CassandraState.class);
        mutatedSeed.testID = 20001;
        graph.addNode(20000, mutatedSeed);
        graph.updateNodeCoverage(20001, false, false, true);
    }

}
