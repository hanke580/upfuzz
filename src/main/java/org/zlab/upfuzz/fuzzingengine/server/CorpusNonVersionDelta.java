package org.zlab.upfuzz.fuzzingengine.server;

import org.zlab.upfuzz.fuzzingengine.Config;

import java.nio.file.Path;
import java.nio.file.Paths;

public class CorpusNonVersionDelta extends Corpus {

    private static final String queueNameBC = "CorpusNonVersionDelta_BC";
    private static final String queueNameFC = "CorpusNonVersionDelta_FC";
    private static final String queueNameBoundaryChange = "CorpusNonVersionDelta_BoundaryChange";

    private static final Path queuePathBC = Paths.get(Config.getConf().corpus)
            .resolve(queueNameBC);
    private static final Path queuePathFC = Paths.get(Config.getConf().corpus)
            .resolve(queueNameFC);
    private static final Path queuePathBoundaryChange = Paths
            .get(Config.getConf().corpus)
            .resolve(queueNameBoundaryChange);

    private int diskSeedIdBC = 0;
    private int diskSeedIdFC = 0;
    private int diskSeedIdBoundaryChange = 0;

    public CorpusNonVersionDelta() {
        super(3, new double[] { Config.getConf().FC_CorpusNonVersionDelta,
                Config.getConf().BC_CorpusNonVersionDelta,
                Config.getConf().BoundaryChange_CorpusNonVersionDelta });
        // sum of probabilities should be 1
        if (Config.getConf().FC_CorpusNonVersionDelta
                + Config.getConf().BC_CorpusNonVersionDelta
                + Config.getConf().BoundaryChange_CorpusNonVersionDelta != 1)
            throw new RuntimeException("Sum of probabilities should be 1");
        assert Config.getConf().useFormatCoverage;
    }

    private enum QueueType {
        FC, BC, BoundaryChange
    }

    @Override
    public void addSeed(Seed seed, boolean newOriBC, boolean newUpBC,
            boolean newOriFC, boolean newUpFC, boolean newBCAfterUpgrade,
            boolean newBCAfterDowngrade, boolean newOriBoundaryChange,
            boolean newUpBoundaryChange) {
        // one seed could exist in multiple queues
        if (newOriFC) {
            cycleQueues[0].addSeed(seed);

            if (Config.getConf().saveCorpusToDisk) {
                while (queuePathFC
                        .resolve("seed_" + diskSeedIdFC).toFile().exists()) {
                    diskSeedIdFC++;
                }
                Corpus.saveSeedQueueOnDisk(seed, queueNameFC, diskSeedIdFC);
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
            cycleQueues[2].addSeed(seed);

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
        testID = Corpus.loadSeedIntoQueue(cycleQueues[QueueType.FC.ordinal()],
                queuePathFC.toFile(), testID);
        testID = Corpus.loadSeedIntoQueue(cycleQueues[QueueType.BC.ordinal()],
                queuePathBC.toFile(), testID);
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
