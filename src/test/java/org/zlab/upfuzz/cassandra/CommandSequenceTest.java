package org.zlab.upfuzz.cassandra;

import org.junit.jupiter.api.Test;
import org.zlab.upfuzz.AbstractTest;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.fuzzingengine.server.Seed;
import org.zlab.upfuzz.fuzzingengine.testplan.event.Event;
import org.zlab.upfuzz.fuzzingengine.testplan.event.command.ShellCommand;
import org.zlab.upfuzz.utils.Utilities;

import java.util.List;
import java.util.Random;

public class CommandSequenceTest extends AbstractTest {

    @Test
    public void testSequenceGeneration() {
        CassandraCommandPool cassandraCommandPool = new CassandraCommandPool();
        Config.getConf().system = "cassandra";
        Seed seed = generateSeed(cassandraCommandPool, CassandraState.class,
                -1);
        assert seed != null;
        printSeed(seed);
        boolean status = seed.mutate(cassandraCommandPool,
                CassandraState.class);
        assert status;
        printSeed(seed);
    }

    // @Test
    public void testCommandSequenceLen() {
        // int i =
        // Utilities.generateSkewedRandom(Config.getConf().MIN_CMD_SEQ_LEN,
        // Config.getConf().MAX_CMD_SEQ_LEN);
        // System.out.println("i = " + i);

        Random rand = new Random();
        double lambda = 0.1; // Rate parameter, adjust for skewness
        int lowerBound = 15;
        int upperBound = 100;

        // Generate a random number with exponential skew
        int randomValue = Utilities.generateExponentialRandom(rand, lambda,
                lowerBound, upperBound);

        // System.out.println("Randomly selected value: " + randomValue);
    }

    // @Test
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
            }
        }
    }
}
