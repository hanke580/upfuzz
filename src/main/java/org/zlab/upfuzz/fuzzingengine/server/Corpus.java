package org.zlab.upfuzz.fuzzingengine.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.CommandSequence;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.utils.Pair;
import org.zlab.upfuzz.utils.Utilities;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public class Corpus {
    static Logger logger = LogManager.getLogger(Corpus.class);
    AtomicLong timestampGenerator = new AtomicLong(0); // To track enqueue order

    // private final CycleQueue formatCoverageQueue = new CycleQueue();
    // private final CycleQueue branchCoverageQueue = new CycleQueue();
    // private final CycleQueue branchCoverageVersionDeltaQueue = new
    // CycleQueue();
    // private final CycleQueue formatCoverageVersionDeltaQueue = new
    // CycleQueue();
    // private final CycleQueue newCoverageOldVersionAfterVersionChangeQueue =
    // new CycleQueue();
    // private final CycleQueue newCoverageNewVersionAfterVersionChangeQueue =
    // new CycleQueue();

    private final CycleQueue[] cycleQueues = new CycleQueue[6];
    {
        for (int i = 0; i < cycleQueues.length; i++) {
            cycleQueues[i] = new CycleQueue();
        }
    }

    private Mode mode;

    public enum QueueType {
        FORMAT_COVERAGE_VERSION_DELTA, BRANCH_COVERAGE_VERSION_DELTA, FORMAT_COVERAGE, BRANCH_COVERAGE_BEFORE_VERSION_CHANGE, NEW_BRANCH_COVERAGE_NEW_VERSION_AFTER_UPGRADE, NEW_BRANCH_COVERAGE_OLD_VERSION_AFTER_DOWNGRADE
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
                    for (CycleQueue cycleQueue : cycleQueues) {
                        cycleQueue.addSeed(seed);
                    }
                    // formatCoverageQueue.addSeed(seed);
                    // branchCoverageQueue.addSeed(seed);
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
            return getSeed(QueueType.BRANCH_COVERAGE_BEFORE_VERSION_CHANGE);
        case BOTH:
            double randomValue = FuzzingServer.rand.nextDouble();
            if (randomValue < Config.getConf().formatCoverageChoiceProb) {
                logger.debug("Pick format coverage seed");
                return getSeed(QueueType.FORMAT_COVERAGE);
            } else {
                logger.debug("Pick branch coverage seed");
                return getSeed(QueueType.BRANCH_COVERAGE_BEFORE_VERSION_CHANGE);
            }
        }
        // shouldn't run here
        throw new IllegalArgumentException("Invalid mode");
    }

    // private Seed getSeed(QueueType type) {
    // switch (type) {
    // case FORMAT_COVERAGE:
    // return formatCoverageQueue.getNextSeed();
    // case BRANCH_COVERAGE:
    // return branchCoverageQueue.getNextSeed();
    // default:
    // throw new IllegalArgumentException("Invalid queue type");
    // }
    // }

    // public Seed peekSeed(QueueType type) {
    // switch (type) {
    // case FORMAT_COVERAGE:
    // return formatCoverageQueue.peekSeed();
    // case BRANCH_COVERAGE:
    // return branchCoverageQueue.peekSeed();
    // default:
    // throw new IllegalArgumentException("Invalid queue type");
    // }
    // }

    // public void addSeed(Seed seed, QueueType type) {
    // seed.setTimestamp(timestampGenerator.getAndIncrement());
    // switch (type) {
    // case FORMAT_COVERAGE:
    // formatCoverageQueue.addSeed(seed);
    // break;
    // case BRANCH_COVERAGE:
    // branchCoverageQueue.addSeed(seed);
    // break;
    // default:
    // throw new IllegalArgumentException("Invalid queue type");
    // }
    // }

    // public boolean isEmpty(QueueType type) {
    // switch (type) {
    // case FORMAT_COVERAGE:
    // return formatCoverageQueue.isEmpty();
    // case BRANCH_COVERAGE:
    // return branchCoverageQueue.isEmpty();
    // default:
    // throw new IllegalArgumentException("Invalid queue type");
    // }
    // }

    // public int getSize(QueueType type) {
    // switch (type) {
    // case FORMAT_COVERAGE:
    // return formatCoverageQueue.size();
    // case BRANCH_COVERAGE:
    // return branchCoverageQueue.size();
    // default:
    // throw new IllegalArgumentException("Invalid queue type");
    // }
    // }

    // public int getIndex(QueueType type) {
    // switch (type) {
    // case FORMAT_COVERAGE:
    // return formatCoverageQueue.getCurrentIndex();
    // case BRANCH_COVERAGE:
    // return branchCoverageQueue.getCurrentIndex();
    // default:
    // throw new IllegalArgumentException("Invalid queue type");
    // }
    // }

    public Seed getSeed(QueueType type) {
        return cycleQueues[type.ordinal()].getNextSeed();
    }

    public Seed peekSeed(QueueType type) {
        return cycleQueues[type.ordinal()].peekSeed();
    }

    public void addSeed(Seed seed, QueueType type) {
        seed.setTimestamp(timestampGenerator.getAndIncrement());
        cycleQueues[type.ordinal()].addSeed(seed);
    }

    public boolean isEmpty(QueueType type) {
        return cycleQueues[type.ordinal()].isEmpty();
    }

    public int getSize(QueueType type) {
        return cycleQueues[type.ordinal()].size();
    }

    public int getIndex(QueueType type) {
        return cycleQueues[type.ordinal()].getCurrentIndex();
    }

    public void printCorpus() {
        for (int i = 0; i < cycleQueues.length; i++) {
            logger.info(
                    "[HKLOG] Corpus Queue " + i + " size: "
                            + cycleQueues[i].size());
        }
    }

    public boolean areAllQueuesEmpty() {
        for (CycleQueue queue : cycleQueues) {
            if (!queue.isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
