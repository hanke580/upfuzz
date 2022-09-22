package org.zlab.upfuzz.fuzzingengine;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import org.zlab.upfuzz.cassandra.CassandraCommandPool;
import org.zlab.upfuzz.cassandra.CassandraState;
import org.zlab.upfuzz.fuzzingengine.Server.FuzzingServer;
import org.zlab.upfuzz.fuzzingengine.Server.Seed;
import org.zlab.upfuzz.fuzzingengine.executor.Executor;
import org.zlab.upfuzz.fuzzingengine.testplan.TestPlan;
import org.zlab.upfuzz.fuzzingengine.testplan.event.Event;

public class FuzzingServerTest {
    @Test
    public void testTestPlanGeneration() {

        CassandraCommandPool commandPool = new CassandraCommandPool();
        Class stateClass = CassandraState.class;
        Seed seed = Executor.generateSeed(commandPool, stateClass);
        Config config = new Config();

        FuzzingServer fuzzingServer = new FuzzingServer();
        TestPlan testPlan = fuzzingServer.generateTestPlan(seed);

        for (Event event : testPlan.getEvents()) {
            if (event == null) {
                System.out.println("null event!");
            }
            System.out.println(event);
        }
    }
}
