package org.zlab.upfuzz.fuzzingengine.server;

import org.zlab.upfuzz.CommandSequence;
import org.zlab.upfuzz.utils.Pair;
import org.zlab.upfuzz.utils.Utilities;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public abstract class Corpus implements ICorpus {
    Random rand = new Random();
    AtomicLong timestampGenerator = new AtomicLong(0); // To track enqueue order
    double[] cumulativeProbabilities;
    CycleQueue[] cycleQueues;

    public Corpus(int queueSize, double[] probabilities) {
        cumulativeProbabilities = new double[probabilities.length];
        cycleQueues = new CycleQueue[probabilities.length];
        for (int i = 0; i < cycleQueues.length; i++) {
            cycleQueues[i] = new CycleQueue();
        }

        cumulativeProbabilities[0] = probabilities[0];
        for (int i = 1; i < probabilities.length; i++) {
            cumulativeProbabilities[i] = cumulativeProbabilities[i - 1]
                    + probabilities[i];
        }
    }

    public abstract void addSeed(Seed seed, boolean newOriBC, boolean newUpBC,
            boolean newOriFC, boolean newUpFC, boolean newBCAfterUpgrade,
            boolean newBCAfterDowngrade);

    public void addSeed(Seed seed, boolean newOriBC, boolean newOriFC,
            boolean newBCAfterUpgrade) {
        addSeed(seed, newOriBC, false, newOriFC, false, newBCAfterUpgrade,
                false);
    }

    public Seed getSeed() {
        if (cumulativeProbabilities.length != cycleQueues.length) {
            throw new RuntimeException(
                    "cumulativeProbabilities and cycleQueues have different lengths");
        }
        int i = Utilities.pickWeightedRandomChoice(cumulativeProbabilities,
                rand.nextDouble());
        return cycleQueues[i].getNextSeed();
    }

    public boolean initCorpus(Path initSeedDirPath) {
        // Add to both queue
        if (!initSeedDirPath.toFile().exists())
            return true;
        File initSeedDir = initSeedDirPath.toFile();
        if (!initSeedDir.isDirectory()) {
            throw new IllegalArgumentException(
                    "initSeedDirPath is not a directory");
        }
        for (File seedFile : Objects.requireNonNull(initSeedDir.listFiles())) {
            if (!seedFile.isDirectory()) {
                Pair<CommandSequence, CommandSequence> commandSequencePair = Utilities
                        .deserializeCommandSequence(seedFile.toPath());
                if (commandSequencePair != null) {
                    Seed seed = new Seed(commandSequencePair.left,
                            commandSequencePair.right, -1, -1);
                    seed.score = 10;
                    for (CycleQueue cycleQueue : cycleQueues) {
                        cycleQueue.addSeed(seed);
                    }
                }
            }
        }
        return true;
    }
}
