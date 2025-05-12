package org.zlab.upfuzz.cassandra;

import org.junit.jupiter.api.Test;
import org.zlab.upfuzz.AbstractTest;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.fuzzingengine.server.Seed;
import org.zlab.upfuzz.fuzzingengine.testplan.event.Event;
import org.zlab.upfuzz.fuzzingengine.testplan.event.command.ShellCommand;

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
}
