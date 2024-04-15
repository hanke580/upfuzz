package org.zlab.upfuzz.fuzzingengine.server;

import org.zlab.upfuzz.fuzzingengine.Config;

import java.nio.file.Path;
import java.nio.file.Paths;

public class CorpusVersionDeltaEightQueueWithBoundary extends Corpus {

    private static final String queueNameFC_VD = "CorpusVersionDeltaEightQueueWithBoundary_"
            + QueueType.FC_VD;
    private static final String queueNameBC_VD = "CorpusVersionDeltaEightQueueWithBoundary_"
            + QueueType.BC_VD;
    private static final String queueNameFC = "CorpusVersionDeltaEightQueueWithBoundary_"
            + QueueType.FC;
    private static final String queueNameBC = "CorpusVersionDeltaEightQueueWithBoundary_"
            + QueueType.BC;
    private static final String queueNameBC_After_Upgrade = "CorpusVersionDeltaEightQueueWithBoundary_"
            + QueueType.BC_After_Upgrade;
    private static final String queueNameBC_After_Downgrade = "CorpusVersionDeltaEightQueueWithBoundary_"
            + QueueType.BC_After_Downgrade;
    private static final String queueNameBoundaryChange_VD = "CorpusVersionDeltaEightQueueWithBoundary_"
            + QueueType.BoundaryChange_VD;
    private static final String queueNameBoundaryChange = "CorpusVersionDeltaEightQueueWithBoundary_"
            + QueueType.BoundaryChange;

    private static final Path queuePathBC = Paths.get(Config.getConf().corpus)
            .resolve(queueNameBC);
    private static final Path queuePathBC_VD = Paths
            .get(Config.getConf().corpus).resolve(queueNameBC_VD);
    private static final Path queuePathBC_After_Upgrade = Paths
            .get(Config.getConf().corpus).resolve(queueNameBC_After_Upgrade);
    private static final Path queuePathBC_After_Downgrade = Paths
            .get(Config.getConf().corpus).resolve(queueNameBC_After_Downgrade);
    private static final Path queuePathFC = Paths.get(Config.getConf().corpus)
            .resolve(queueNameFC);
    private static final Path queuePathFC_VD = Paths
            .get(Config.getConf().corpus).resolve(queueNameFC_VD);
    private static final Path queuePathBoundaryChange_VD = Paths
            .get(Config.getConf().corpus).resolve(queueNameBoundaryChange_VD);
    private static final Path queuePathBoundaryChange = Paths
            .get(Config.getConf().corpus).resolve(queueNameBoundaryChange);

    private int diskSeedIdBC = 0;
    private int diskSeedIdBC_VD = 0;
    private int diskSeedIdBC_After_Upgrade = 0;
    private int diskSeedIdBC_After_Downgrade = 0;
    private int diskSeedIdFC = 0;
    private int diskSeedIdFC_VD = 0;
    private int diskSeedIdBoundaryChange = 0;
    private int diskSeedIdBoundaryChange_VD = 0;

    public CorpusVersionDeltaEightQueueWithBoundary() {
        super(8,
                new double[] {
                        Config.getConf().FC_VD_PROB_CorpusVersionDeltaEightQueueWithBoundary_G2,
                        Config.getConf().FC_PROB_CorpusVersionDeltaEightQueueWithBoundary_G2,
                        Config.getConf().BC_VD_PROB_CorpusVersionDeltaEightQueueWithBoundary_G2,
                        Config.getConf().BC_PROB_CorpusVersionDeltaEightQueueWithBoundary_G2,
                        Config.getConf().BC_After_Upgrade_PROB_CorpusVersionDeltaEightQueueWithBoundary_G2,
                        Config.getConf().BC_After_Downgrade_PROB_CorpusVersionDeltaEightQueueWithBoundary_G2,
                        Config.getConf().BoundaryChange_VD_PROB_CorpusVersionDeltaEightQueueWithBoundary_G2,
                        Config.getConf().BoundaryChange_PROB_CorpusVersionDeltaEightQueueWithBoundary_G2,
                });
    }

    public enum QueueType {
        FC_VD, BC_VD, FC, BC, BC_After_Upgrade, BC_After_Downgrade, BoundaryChange_VD, BoundaryChange
    }

    public Seed getSeed(QueueType type) {
        return cycleQueues[type.ordinal()].getNextSeed();
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

    public boolean areAllQueuesEmpty() {
        for (CycleQueue queue : cycleQueues) {
            if (!queue.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void addSeed(Seed seed, boolean newOriBC, boolean newUpBC,
            boolean newOriFC, boolean newUpFC, boolean newBCAfterUpgrade,
            boolean newBCAfterDowngrade, boolean newOriBoundaryChange,
            boolean newUpBoundaryChange) {
        // FIXME: boundary is not considered yet
        if (newOriBC ^ newUpBC) {
            cycleQueues[QueueType.BC_VD.ordinal()].addSeed(seed);

            if (Config.getConf().saveCorpusToDisk) {
                while (queuePathBC_VD
                        .resolve("seed_" + diskSeedIdBC_VD).toFile().exists()) {
                    diskSeedIdBC_VD++;
                }
                Corpus.saveSeedQueueOnDisk(seed, queueNameBC_VD,
                        diskSeedIdBC_VD);
            }

        } else {
            if (newOriBC && newUpBC) {
                cycleQueues[QueueType.BC.ordinal()].addSeed(seed);

                if (Config.getConf().saveCorpusToDisk) {
                    while (queuePathBC
                            .resolve("seed_" + diskSeedIdBC).toFile()
                            .exists()) {
                        diskSeedIdBC++;
                    }
                    Corpus.saveSeedQueueOnDisk(seed, queueNameBC, diskSeedIdBC);
                }

            }
        }

        if (newOriFC ^ newUpFC) {
            cycleQueues[QueueType.FC_VD.ordinal()].addSeed(seed);

            if (Config.getConf().saveCorpusToDisk) {
                while (queuePathFC_VD
                        .resolve("seed_" + diskSeedIdFC_VD).toFile().exists()) {
                    diskSeedIdFC_VD++;
                }
                Corpus.saveSeedQueueOnDisk(seed, queueNameFC_VD,
                        diskSeedIdFC_VD);
            }

        } else {
            if (newOriFC && newUpFC) {
                cycleQueues[QueueType.FC.ordinal()].addSeed(seed);

                if (Config.getConf().saveCorpusToDisk) {
                    while (queuePathFC
                            .resolve("seed_" + diskSeedIdFC).toFile()
                            .exists()) {
                        diskSeedIdFC++;
                    }
                    Corpus.saveSeedQueueOnDisk(seed, queueNameFC, diskSeedIdFC);
                }

            }
        }

        if (newBCAfterUpgrade) {
            cycleQueues[QueueType.BC_After_Upgrade.ordinal()].addSeed(seed);

            if (Config.getConf().saveCorpusToDisk) {
                while (queuePathBC_After_Upgrade
                        .resolve("seed_" + diskSeedIdBC_After_Upgrade).toFile()
                        .exists()) {
                    diskSeedIdBC_After_Upgrade++;
                }
                Corpus.saveSeedQueueOnDisk(seed, queueNameBC_After_Upgrade,
                        diskSeedIdBC_After_Upgrade);
            }

        }

        if (newBCAfterDowngrade) {
            cycleQueues[QueueType.BC_After_Downgrade.ordinal()].addSeed(seed);

            if (Config.getConf().saveCorpusToDisk) {
                while (queuePathBC_After_Downgrade
                        .resolve("seed_" + diskSeedIdBC_After_Downgrade)
                        .toFile().exists()) {
                    diskSeedIdBC_After_Downgrade++;
                }
                Corpus.saveSeedQueueOnDisk(seed, queueNameBC_After_Downgrade,
                        diskSeedIdBC_After_Downgrade);
            }
        }

        if (newOriBoundaryChange ^ newUpBoundaryChange) {
            cycleQueues[QueueType.BoundaryChange_VD.ordinal()].addSeed(seed);

            if (Config.getConf().saveCorpusToDisk) {
                while (queuePathBoundaryChange_VD
                        .resolve("seed_" + diskSeedIdBoundaryChange_VD).toFile()
                        .exists()) {
                    diskSeedIdBoundaryChange_VD++;
                }
                Corpus.saveSeedQueueOnDisk(seed, queueNameBoundaryChange_VD,
                        diskSeedIdBoundaryChange_VD);
            }
        } else {
            cycleQueues[QueueType.BoundaryChange.ordinal()].addSeed(seed);

            if (Config.getConf().saveCorpusToDisk) {
                while (queuePathBoundaryChange
                        .resolve("seed_" + diskSeedIdBoundaryChange).toFile()
                        .exists()) {
                    diskSeedIdBoundaryChange++;
                }
                Corpus.saveSeedQueueOnDisk(seed, queueNameBoundaryChange,
                        diskSeedIdBoundaryChange);
            }
        }
    }

    @Override
    public int initCorpus() {
        Path corpusPath = Paths.get(Config.getConf().corpus);
        if (!corpusPath.toFile().exists())
            return 0;
        if (!corpusPath.toFile().isDirectory()) {
            throw new RuntimeException(
                    "corpusPath is not a directory: " + corpusPath);
        }
        // process each queues

        int testID = 0;
        testID = Corpus.loadSeedIntoQueue(
                cycleQueues[QueueType.FC_VD.ordinal()],
                queuePathFC_VD.toFile(), testID);
        testID = Corpus.loadSeedIntoQueue(
                cycleQueues[QueueType.BC_VD.ordinal()],
                queuePathBC_VD.toFile(), testID);
        testID = Corpus.loadSeedIntoQueue(cycleQueues[QueueType.FC.ordinal()],
                queuePathFC.toFile(), testID);
        testID = Corpus.loadSeedIntoQueue(cycleQueues[QueueType.BC.ordinal()],
                queuePathBC.toFile(), testID);
        testID = Corpus.loadSeedIntoQueue(
                cycleQueues[QueueType.BC_After_Upgrade.ordinal()],
                queuePathBC_After_Upgrade.toFile(), testID);
        testID = Corpus.loadSeedIntoQueue(
                cycleQueues[QueueType.BC_After_Downgrade.ordinal()],
                queuePathBC_After_Downgrade.toFile(), testID);
        testID = Corpus.loadSeedIntoQueue(
                cycleQueues[QueueType.BoundaryChange_VD.ordinal()],
                queuePathBoundaryChange_VD.toFile(), testID);
        testID = Corpus.loadSeedIntoQueue(
                cycleQueues[QueueType.BoundaryChange.ordinal()],
                queuePathBoundaryChange.toFile(), testID);
        return testID;
    }

    @Override
    public void printInfo() {
        // Print all six queues
        for (int i = 0; i < cycleQueues.length; i++) {
            System.out.format("|%30s|%30s|%30s|%30s|\n",
                    "QueueType : " + QueueType.values()[i],
                    "queue size : "
                            + cycleQueues[i].size(),
                    "index : "
                            + cycleQueues[i].getCurrentIndex(),
                    "");
        }
    }
}
