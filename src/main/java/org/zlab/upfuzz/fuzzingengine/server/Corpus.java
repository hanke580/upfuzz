package org.zlab.upfuzz.fuzzingengine.server;

import org.zlab.upfuzz.CommandSequence;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.utils.Pair;
import org.zlab.upfuzz.utils.Utilities;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicLong;

public class Corpus {
    AtomicLong timestampGenerator = new AtomicLong(0); // To track enqueue order

    private final CycleQueue formatCoverageQueue = new CycleQueue();
    private final CycleQueue branchCoverageQueue = new CycleQueue();

    private Mode mode;

    enum QueueType {
        FORMAT_COVERAGE, BRANCH_COVERAGE
    }

    private enum Mode {
        FC_ONLY, BC_ONLY, BOTH
    }

    public Corpus() {
        if (Config.getConf().useFormatCoverage
                && Config.getConf().useBranchCoverage) {
            mode = Mode.BOTH;
        } else if (Config.getConf().useFormatCoverage) {
            mode = Mode.FC_ONLY;
        } else if (Config.getConf().useBranchCoverage) {
            mode = Mode.BC_ONLY;
        } else {
            throw new IllegalArgumentException("No coverage mode selected");
        }
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
                    formatCoverageQueue.addSeed(seed);
                    branchCoverageQueue.addSeed(seed);
                }
            }
        }
        return true;
    }

    public Seed getSeed() {
        switch (mode) {
        case FC_ONLY:
            return getSeed(QueueType.FORMAT_COVERAGE);
        case BC_ONLY:
            return getSeed(QueueType.BRANCH_COVERAGE);
        case BOTH:
            double randomValue = FuzzingServer.rand.nextDouble();
            if (randomValue < Config.getConf().formatCoverageChoiceProb) {
                return getSeed(QueueType.FORMAT_COVERAGE);
            } else {
                return getSeed(QueueType.BRANCH_COVERAGE);
            }
        }
        // shouldn't run here
        throw new IllegalArgumentException("Invalid mode");
    }

    private Seed getSeed(QueueType type) {
        switch (type) {
        case FORMAT_COVERAGE:
            return formatCoverageQueue.getNextSeed();
        case BRANCH_COVERAGE:
            return branchCoverageQueue.getNextSeed();
        default:
            throw new IllegalArgumentException("Invalid queue type");
        }
    }

    public Seed peekSeed(QueueType type) {
        switch (type) {
        case FORMAT_COVERAGE:
            return formatCoverageQueue.peekSeed();
        case BRANCH_COVERAGE:
            return branchCoverageQueue.peekSeed();
        default:
            throw new IllegalArgumentException("Invalid queue type");
        }
    }

    public void addSeed(Seed seed, QueueType type) {
        seed.setTimestamp(timestampGenerator.getAndIncrement());
        switch (type) {
        case FORMAT_COVERAGE:
            formatCoverageQueue.addSeed(seed);
            break;
        case BRANCH_COVERAGE:
            branchCoverageQueue.addSeed(seed);
            break;
        default:
            throw new IllegalArgumentException("Invalid queue type");
        }
    }

    public boolean isEmpty(QueueType type) {
        switch (type) {
        case FORMAT_COVERAGE:
            return formatCoverageQueue.isEmpty();
        case BRANCH_COVERAGE:
            return branchCoverageQueue.isEmpty();
        default:
            throw new IllegalArgumentException("Invalid queue type");
        }
    }

    public int getSize(QueueType type) {
        switch (type) {
        case FORMAT_COVERAGE:
            return formatCoverageQueue.size();
        case BRANCH_COVERAGE:
            return branchCoverageQueue.size();
        default:
            throw new IllegalArgumentException("Invalid queue type");
        }
    }
}
