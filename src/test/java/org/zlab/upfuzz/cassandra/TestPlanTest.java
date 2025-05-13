package org.zlab.upfuzz.cassandra;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.jupiter.api.Test;
import org.zlab.upfuzz.AbstractTest;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.fuzzingengine.server.FullStopSeed;
import org.zlab.upfuzz.fuzzingengine.server.FuzzingServer;
import org.zlab.upfuzz.fuzzingengine.server.Seed;
import org.zlab.upfuzz.fuzzingengine.testplan.TestPlan;
import org.zlab.upfuzz.fuzzingengine.testplan.event.Event;
import org.zlab.upfuzz.fuzzingengine.testplan.event.command.ShellCommand;

import java.util.LinkedList;
import java.util.List;

public class TestPlanTest extends AbstractTest {

    @Test
    public void testPlan() {
        CassandraCommandPool cassandraCommandPool = new CassandraCommandPool();
        Config.getConf().system = "cassandra";
        Seed seed = generateSeed(cassandraCommandPool, CassandraState.class,
                -1);

        List<Event> shellCommands = ShellCommand.seedWriteCmd2Events(seed, 3);

        for (Event event : shellCommands) {
            if (event instanceof ShellCommand) {
                ShellCommand shellCommand = (ShellCommand) event;
                System.out.println("Node Index: " + shellCommand.getCommand());
                System.out.println("Command: " + shellCommand.getNodeIndex());
                System.out.println("Interval: " + shellCommand.interval);
            }
        }
    }

    @Test
    public void testTestPlanMutation() {
        CassandraCommandPool cassandraCommandPool = new CassandraCommandPool();
        Config.getConf().system = "cassandra";
        Seed seed = generateSeed(cassandraCommandPool, CassandraState.class,
                -1);

        FullStopSeed fullStopSeed = new FullStopSeed(seed, new LinkedList<>());

        TestPlan testPlan = null;
        for (int i = 0; i < 20; i++) {
            testPlan = FuzzingServer.generateTestPlan(fullStopSeed);
            if (testPlan != null)
                break;
        }

        if (testPlan == null) {
            System.out.println("Test plan is null");
            return;
        }

        testPlan.print();

        System.out.println();

        TestPlan mutateTestPlan = SerializationUtils.clone(testPlan);

        if (!mutateTestPlan.mutate(cassandraCommandPool,
                CassandraState.class)) {
            System.out.println("Testplan mutation failed");
            return;
        }

        for (Event event : mutateTestPlan.getEvents())
            assert event != null;

        System.out.println("Testplan mutated successfully");
        mutateTestPlan.print();
    }
}
