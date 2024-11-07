package org.zlab.upfuzz.fuzzingengine.server;

import org.zlab.upfuzz.fuzzingengine.Config;

import java.nio.file.Path;
import java.nio.file.Paths;

public class CorpusVersionDeltaFiveQueueWithBoundary extends Corpus {

    private static final String queueNameBC = "CorpusVersionDeltaFiveQueueWithBoundary_BC";
    private static final String queueNameBC_VD = "CorpusVersionDeltaFiveQueueWithBoundary_BC_VD";
    private static final String queueNameFC = "CorpusVersionDeltaFiveQueueWithBoundary_FC";
    private static final String queueNameFC_VD = "CorpusVersionDeltaFiveQueueWithBoundary_FC_VD";
    private static final String queueNameBoundaryChange = "CorpusVersionDeltaFiveQueueWithBoundary_BoundaryChange";

    private static final Path queuePathBC = Paths.get(Config.getConf().corpus)
            .resolve(Config.getConf().system)
            .resolve(queueNameBC);
    private static final Path queuePathBC_VD = Paths
            .get(Config.getConf().corpus).resolve(Config.getConf().system)
            .resolve(queueNameBC_VD);
    private static final Path queuePathFC = Paths.get(Config.getConf().corpus)
            .resolve(Config.getConf().system)
            .resolve(queueNameFC);
    private static final Path queuePathFC_VD = Paths
            .get(Config.getConf().corpus).resolve(Config.getConf().system)
            .resolve(queueNameFC_VD);
    private static final Path queuePathBoundaryChange = Paths
            .get(Config.getConf().corpus).resolve(Config.getConf().system)
            .resolve(queueNameBoundaryChange);

    private int diskSeedIdBC = 0;
    private int diskSeedIdBC_VD = 0;
    private int diskSeedIdFC = 0;
    private int diskSeedIdFC_VD = 0;
    private int diskSeedIdBoundaryChange = 0;

    public CorpusVersionDeltaFiveQueueWithBoundary() {
        super(5, new double[] {
                Config.getConf().FC_VD_PROB_CorpusVersionDeltaFiveQueueWithBoundary,
                Config.getConf().FC_PROB_CorpusVersionDeltaFiveQueueWithBoundary,
                Config.getConf().BC_VD_PROB_CorpusVersionDeltaFiveQueueWithBoundary,
                Config.getConf().BC_PROB_CorpusVersionDeltaFiveQueueWithBoundary,
                Config.getConf().BoundaryChange_PROB_CorpusVersionDeltaFiveQueueWithBoundary
        });
    }

    private enum QueueType {
        FC_VD, FC, BC_VD, BC, BoundaryChange
    }

    @Override
    public void addSeed(Seed seed, boolean newOriBC, boolean newUpBC,
            boolean newOriFC, boolean newUpFC, boolean newBCAfterUpgrade,
            boolean newBCAfterDowngrade, boolean newOriBoundaryChange,
            boolean newUpBoundaryChange, boolean newModifiedFormatCoverage,
            boolean newBCVD, boolean newFCVD) {
        // One seed can occur in multiple queues (higher fuzzing energy)
        if (newFCVD) {
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
            if (newOriFC || newUpFC) {
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

        if (newBCVD) {
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
            // examine whether any new coverage is reached
            if (newOriBC || newUpBC || newBCAfterUpgrade
                    || newBCAfterDowngrade) {
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

        if (newOriBoundaryChange || newUpBoundaryChange) {
            cycleQueues[QueueType.BoundaryChange.ordinal()].addSeed(seed);

            if (Config.getConf().saveCorpusToDisk) {
                while (queuePathBoundaryChange
                        .resolve("seed_" + diskSeedIdBoundaryChange)
                        .toFile()
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
        Path corpusPath = Paths.get(Config.getConf().corpus)
                .resolve(Config.getConf().system);
        if (!corpusPath.toFile().exists())
            return 0;
        if (!corpusPath.toFile().isDirectory()) {
            throw new RuntimeException(
                    "corpusPath is not a directory: " + corpusPath);
        }
        // process each queues.resolve(Config.getConf().system)
        int testID = 0;
        testID = Corpus.loadSeedIntoQueue(
                cycleQueues[QueueType.FC_VD.ordinal()],
                queuePathFC_VD.toFile(), testID);
        testID = Corpus.loadSeedIntoQueue(cycleQueues[QueueType.FC.ordinal()],
                queuePathFC.toFile(), testID);
        testID = Corpus.loadSeedIntoQueue(
                cycleQueues[QueueType.BC_VD.ordinal()],
                queuePathBC_VD.toFile(), testID);
        testID = Corpus.loadSeedIntoQueue(cycleQueues[QueueType.BC.ordinal()],
                queuePathBC.toFile(), testID);
        testID = Corpus.loadSeedIntoQueue(
                cycleQueues[QueueType.BoundaryChange.ordinal()],
                queuePathBoundaryChange.toFile(), testID);
        return testID;
    }

    @Override
    public void printInfo() {
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
