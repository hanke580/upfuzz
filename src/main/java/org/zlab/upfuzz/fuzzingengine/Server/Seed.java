package org.zlab.upfuzz.fuzzingengine.Server;

import java.io.Serializable;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.CommandSequence;
import org.zlab.upfuzz.fuzzingengine.Packet.StackedTestPacket;
import org.zlab.upfuzz.utils.Utilities;

public class Seed implements Serializable {
    static Logger logger = LogManager.getLogger(Seed.class);

    private final int MUTATE_RETRY_TIME = 10;
    private final Random rand;

    // Write commands
    public CommandSequence originalCommandSequence;
    public CommandSequence upgradedCommandSequence; // No use for now
    // Read Commands
    public CommandSequence validationCommandSequnece;

    public Seed(CommandSequence originalCommandSequence,
            CommandSequence validationCommandSequnece) {
        rand = new Random();
        this.originalCommandSequence = originalCommandSequence;
        this.validationCommandSequnece = validationCommandSequnece;
    }

    public boolean mutate() {
        try {
            if (mutateImpl(originalCommandSequence)) {
                validationCommandSequnece = originalCommandSequence
                        .generateRelatedReadSequence();
                return true;
            }
        } catch (Exception e) {
            logger.error("Generate related read sequence error", e);
            return false;
        }
        return false;
    }

    private boolean mutateImpl(CommandSequence commandSequence) {
        int i = 0;
        for (; i < MUTATE_RETRY_TIME; i++) {
            try {
                if (commandSequence.mutate() && Utilities.oneOf(rand, 3)) {
                    break;
                }
            } catch (Exception e) {
                logger.error("Mutation error", e);
                return false;
            }
        }
        return i != MUTATE_RETRY_TIME;

    }

    public StackedTestPacket toPacket() {
        return null;
    }
}
