package org.zlab.upfuzz.fuzzingengine.server;

import java.io.Serializable;
import java.util.Random;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.CommandPool;
import org.zlab.upfuzz.CommandSequence;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.fuzzingengine.packet.StackedTestPacket;

public class Seed implements Serializable, Comparable<Seed> {
    static Logger logger = LogManager.getLogger(Seed.class);

    private final int MAX_STACK_MUTATION = 10;

    public int score = 0;

    // Write commands
    public CommandSequence originalCommandSequence;
    public CommandSequence upgradedCommandSequence; // No use for now
    // Read Commands
    public CommandSequence validationCommandSequence;

    // Configuration filename
    public int configIdx = -1;
    public int testID = -1;

    // timestamp: when the seed is added to the corpus
    private long timestamp = -1;

    public Seed(CommandSequence originalCommandSequence,
            CommandSequence validationCommandSequence, int configIdx,
            int testID) {
        this.originalCommandSequence = originalCommandSequence;
        this.validationCommandSequence = validationCommandSequence;
        this.configIdx = configIdx;
        this.testID = testID;
    }

    public boolean mutate(CommandPool commandPool,
            Class<? extends State> stateClass) {
        try {
            if (mutateImpl(originalCommandSequence)) {
                originalCommandSequence.initializeTypePool();
                validationCommandSequence = CommandSequence.generateSequence(
                        commandPool.readCommandClassList,
                        null,
                        stateClass, originalCommandSequence.state);
                if (Config.getConf().system.equals("hdfs")) {
                    validationCommandSequence.commands.remove(0);
                }
                return true;
            }
        } catch (Exception e) {
            logger.error("Generate related read sequence error", e);
            return false;
        }
        return false;
    }

    private boolean mutateImpl(CommandSequence commandSequence) {
        boolean ret = false;
        for (int i = 0; i < MAX_STACK_MUTATION; i++) {
            try {
                // At least one mutation succeeds
                if (commandSequence.mutate())
                    ret = true;
                break;
                // FIXME: Enable the stacked mutation
                // 1/3 prob stop mutation, 2/3 prob keep stacking mutations
                // if (Utilities.oneOf(rand, 3))
                // break;
            } catch (Exception e) {
                logger.error("Mutation error", e);
                return false;
            }
        }
        return ret;
    }

    public StackedTestPacket toPacket() {
        return null;
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
