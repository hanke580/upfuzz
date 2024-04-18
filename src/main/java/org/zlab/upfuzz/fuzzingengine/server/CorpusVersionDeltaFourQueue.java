package org.zlab.upfuzz.fuzzingengine.server;

import org.zlab.upfuzz.fuzzingengine.Config;

import java.nio.file.Path;
import java.nio.file.Paths;

public class CorpusVersionDeltaFourQueue extends Corpus {

    private static final String queueNameBC = "CorpusNonVersionDelta_BC";
    private static final String queueNameBC_VD = "CorpusNonVersionDelta_BC_VD";
    private static final String queueNameFC = "CorpusNonVersionDelta_FC";
    private static final String queueNameFC_VD = "CorpusNonVersionDelta_FC_VD";

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

    private int diskSeedIdBC = 0;
    private int diskSeedIdBC_VD = 0;
    private int diskSeedIdFC = 0;
    private int diskSeedIdFC_VD = 0;

    /**
     * 0: FC-VD: 40%
     * 1: FC: 20%
     * 2: BC-VD: 25%
     * 3: BC: 15%
     */
    public CorpusVersionDeltaFourQueue() {
        super(4, new double[] {
                Config.getConf().FC_VD_PROB_CorpusVersionDeltaFourQueue,
                Config.getConf().FC_PROB_CorpusVersionDeltaFourQueue,
                Config.getConf().BC_VD_PROB_CorpusVersionDeltaFourQueue,
                Config.getConf().BC_PROB_CorpusVersionDeltaFourQueue });
    }

    private enum QueueType {
        FC_VD, FC, BC_VD, BC
    }

    @Override
    public void addSeed(Seed seed, boolean newOriBC, boolean newUpBC,
            boolean newOriFC, boolean newUpFC, boolean newBCAfterUpgrade,
            boolean newBCAfterDowngrade, boolean newOriBoundaryChange,
            boolean newUpBoundaryChange) {
        // One seed can occur in multiple queues (representing higher energy)
        // However, for one coverage, it can only exist in either vd or non-vd
        // queue
        if (newOriFC ^ newUpFC) {
            cycleQueues[0].addSeed(seed);

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
                cycleQueues[1].addSeed(seed);

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

        if (newOriBC ^ newUpBC) {
            cycleQueues[2].addSeed(seed);

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
                cycleQueues[3].addSeed(seed);

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
        // process each queues
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
