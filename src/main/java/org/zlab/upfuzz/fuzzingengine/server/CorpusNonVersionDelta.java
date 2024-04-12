package org.zlab.upfuzz.fuzzingengine.server;

import org.zlab.upfuzz.fuzzingengine.Config;

import java.nio.file.Path;
import java.nio.file.Paths;

public class CorpusNonVersionDelta extends Corpus {

    private static final String queueNameBC = "CorpusNonVersionDelta_BC";
    private static final String queueNameFC = "CorpusNonVersionDelta_FC";

    private static final Path queuePathBC = Paths.get(Config.getConf().corpus)
            .resolve(queueNameBC);
    private static final Path queuePathFC = Paths.get(Config.getConf().corpus)
            .resolve(queueNameFC);
    private int diskSeedIdBC = 0;
    private int diskSeedIdFC = 0;

    /**
     * Suppose FC is enabled
     * 1: FC: 80%
     * 3: BC: 20%
     */
    public CorpusNonVersionDelta() {
        super(2, new double[] { Config.getConf().FC_CorpusNonVersionDelta,
                1 - Config.getConf().FC_CorpusNonVersionDelta });
        assert Config.getConf().useFormatCoverage;
    }

    private enum QueueType {
        FC, BC
    }

    @Override
    public void addSeed(Seed seed, boolean newOriBC, boolean newUpBC,
            boolean newOriFC, boolean newUpFC, boolean newBCAfterUpgrade,
            boolean newBCAfterDowngrade) {
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
