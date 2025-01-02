package org.zlab.upfuzz.fuzzingengine.server;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.*;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.utils.Utilities;

public class Seed implements Serializable, Comparable<Seed> {
    static Logger logger = LogManager.getLogger(Seed.class);

    private static final int MAX_STACK_MUTATION = 10;

    public int score = 0;
    public Random rand = new Random();

    // Write commands
    public CommandSequence originalCommandSequence;
    public CommandSequence upgradedCommandSequence; // No use for now
    // Read Commands
    public CommandSequence validationCommandSequence;

    // Configuration filename
    public int configIdx;
    public int testID;

    public int mutationDepth;

    // timestamp: when the seed is added to the corpus
    private long timestamp = -1;

    public Seed(CommandSequence originalCommandSequence,
            CommandSequence validationCommandSequence, int configIdx,
            int testID, int mutationDepth) {
        this.originalCommandSequence = originalCommandSequence;
        this.validationCommandSequence = validationCommandSequence;
        this.configIdx = configIdx;
        this.testID = testID;
        this.mutationDepth = mutationDepth;
    }

    public static void postProcessValidationCommands(
            CommandSequence validationCommandSequence) {
        if (Config.getConf().system.equals("hdfs")) {
            validationCommandSequence.commands.remove(0);
        }
        // For evaluation purpose: remove SELECT without ORDER BY DESC
        if (Config.getConf().system.equals("cassandra")
                && Config.getConf().eval_14803_filter_forward_read) {
            Utilities.filterForwardReadFor14803(
                    validationCommandSequence);
        }
        // Remove duplicated read commands
        Set<String> readCommandSet = new HashSet<>();
        List<Command> deDupCommands = new LinkedList<>();
        for (Command command : validationCommandSequence.commands) {
            if (readCommandSet.contains(command.constructCommandString())) {
                continue;
            }
            readCommandSet.add(command.constructCommandString());
            deDupCommands.add(command);
        }
        validationCommandSequence.commands = deDupCommands;
    }

    public static Seed generateSeed(CommandPool commandPool,
            Class<? extends State> stateClass, int configIdx, int testID) {
        CommandSequence originalCommandSequence;
        CommandSequence validationCommandSequence;
        try {
            ParameterType.BasicConcreteType.clearPool();
            originalCommandSequence = CommandSequence.generateSequence(
                    commandPool.commandClassList,
                    commandPool.createCommandClassList, stateClass, null,
                    false);
            validationCommandSequence = CommandSequence.generateSequence(
                    commandPool.readCommandClassList, null, stateClass,
                    originalCommandSequence.state, true);
            postProcessValidationCommands(validationCommandSequence);

            if (Config.getConf().debug) {
                System.out.println("Original Command Sequence:");
                Utilities.printCommandSequence(originalCommandSequence);
            }

            return new Seed(originalCommandSequence, validationCommandSequence,
                    configIdx, testID, 0);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean mutate(CommandPool commandPool,
            Class<? extends State> stateClass) {
        try {
            if (mutateImpl(originalCommandSequence)) {
                originalCommandSequence.initializeTypePool();
                validationCommandSequence = CommandSequence.generateSequence(
                        commandPool.readCommandClassList,
                        null,
                        stateClass, originalCommandSequence.state, true);
                postProcessValidationCommands(validationCommandSequence);
                return true;
            }
        } catch (Exception e) {
            logger.error("Mutation error", e);
            for (StackTraceElement ste : e.getStackTrace()) {
                logger.error(ste);
            }
            return false;
        }
        return false;
    }

    private boolean mutateImpl(CommandSequence commandSequence) {
        boolean ret = false;
        for (int i = 0; i < MAX_STACK_MUTATION; i++) {
            if (mutateWithTimeoutCheck(commandSequence))
                ret = true;
            else
                continue;
            // 2/3 prob stop stacking mutation
            if (!Utilities.oneOf(rand, 3))
                break;
        }
        return ret;
    }

    private boolean mutateWithTimeoutCheck(CommandSequence commandSequence) {
        Boolean ret = false;
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Callable<Boolean> task = commandSequence::mutate;
        Future<Boolean> future = null;
        try {
            future = executor.submit(task);
            ret = future.get(2, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            logger.error("Mutation did not complete within the time limit.");
            future.cancel(true);
        } catch (Exception e) {
            logger.error("Mutation error", e);
        } finally {
            executor.shutdownNow();
        }
        return ret;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public int compareTo(Seed o) {
        if (score < o.score) {
            return -1;
        } else if (score > o.score) {
            return 1;
        } else {
            // equal, compare the timestamp
            // If seed is older, return 1
            if (timestamp < o.timestamp) {
                return 1;
            } else if (timestamp > o.timestamp) {
                return -1;
            }
            return 0;
        }
    }
}
