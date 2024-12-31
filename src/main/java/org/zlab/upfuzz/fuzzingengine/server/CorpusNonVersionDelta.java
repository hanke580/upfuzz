package org.zlab.upfuzz.fuzzingengine.server;

import org.zlab.upfuzz.fuzzingengine.Config;

import java.nio.file.Path;
import java.nio.file.Paths;

public class CorpusNonVersionDelta extends Corpus {

    private static final String queueNameBC = "CorpusNonVersionDelta_BC";
    private static final String queueNameFC = "CorpusNonVersionDelta_FC";
    private static final String queueNameFCMOD = "CorpusNonVersionDelta_FC_MOD";
    private static final String queueNameBoundaryChange = "CorpusNonVersionDelta_BoundaryChange";

    private static final Path queuePathBC = Paths.get(Config.getConf().corpus)
            .resolve(Config.getConf().system)
            .resolve(queueNameBC);
    private static final Path queuePathFC = Paths.get(Config.getConf().corpus)
            .resolve(Config.getConf().system)
            .resolve(queueNameFC);
    private static final Path queuePathFCMOD = Paths
            .get(Config.getConf().corpus)
            .resolve(Config.getConf().system)
            .resolve(queueNameFCMOD);
    private static final Path queuePathBoundaryChange = Paths
            .get(Config.getConf().corpus).resolve(Config.getConf().system)
            .resolve(queueNameBoundaryChange);

    private int diskSeedIdBC = 0;
    private int diskSeedIdFC = 0;
    private int diskSeedIdFCMOD = 0;
    private int diskSeedIdBoundaryChange = 0;

    public CorpusNonVersionDelta() {
        super(4, new double[] { Config.getConf().FC_CorpusNonVersionDelta,
                Config.getConf().BC_CorpusNonVersionDelta,
                Config.getConf().FC_MOD_CorpusNonVersionDelta,
                Config.getConf().BoundaryChange_CorpusNonVersionDelta });
        // sum of probabilities should be 1
        double sum = Config.getConf().FC_CorpusNonVersionDelta
                + Config.getConf().FC_MOD_CorpusNonVersionDelta
                + Config.getConf().BC_CorpusNonVersionDelta
                + Config.getConf().BoundaryChange_CorpusNonVersionDelta;
        if (Math.abs(sum - 1.0) > 1e-9) // Allow a small error margin (epsilon)
            throw new RuntimeException(
                    "Sum of probabilities should be approximately 1");
        assert Config.getConf().useFormatCoverage;
    }

    private enum QueueType {
        FC, BC, FC_MOD, BoundaryChange
    }

    @Override
    public void addSeed(Seed seed, boolean newOriBC, boolean newUpBC,
            boolean newOriFC, boolean newUpFC, boolean newBCAfterUpgrade,
            boolean newBCAfterDowngrade, boolean newOriBoundaryChange,
            boolean newUpBoundaryChange, boolean newNonMatchableFC,
            boolean newBCVD, boolean newFCVD) {
        if (newNonMatchableFC) {
            cycleQueues[2].addSeed(seed);

            if (Config.getConf().saveCorpusToDisk) {
                while (queuePathBoundaryChange
                        .resolve("seed_" + diskSeedIdFCMOD).toFile()
                        .exists()) {
                    diskSeedIdFCMOD++;
                }
                Corpus.saveSeedQueueOnDisk(seed, queueNameFCMOD,
                        diskSeedIdFCMOD);
            }
        }
        if (!newNonMatchableFC || Config.getConf().addTestToBothFCandVD) {
            if (newOriFC) {
                cycleQueues[0].addSeed(seed);

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
        if (newOriBC || newBCAfterUpgrade) {
            cycleQueues[1].addSeed(seed);

            if (Config.getConf().saveCorpusToDisk) {
                while (queuePathBC
                        .resolve("seed_" + diskSeedIdBC).toFile().exists()) {
                    diskSeedIdBC++;
                }
                Corpus.saveSeedQueueOnDisk(seed, queueNameBC, diskSeedIdBC);
            }
        }
        if (newOriBoundaryChange) {
            cycleQueues[3].addSeed(seed);

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
        testID = Corpus.loadSeedIntoQueue(cycleQueues[QueueType.FC.ordinal()],
                queuePathFC.toFile(), testID);
        testID = Corpus.loadSeedIntoQueue(cycleQueues[QueueType.BC.ordinal()],
                queuePathBC.toFile(), testID);
        testID = Corpus.loadSeedIntoQueue(
                cycleQueues[QueueType.FC_MOD.ordinal()],
                queuePathFCMOD.toFile(), testID);
        testID = Corpus.loadSeedIntoQueue(
                cycleQueues[QueueType.BoundaryChange.ordinal()],
                queuePathBoundaryChange.toFile(), testID);
        return testID;
    }

    @Override
    public void printInfo() {
        // Print all queues
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
