package org.zlab.upfuzz.fuzzingengine.server;

import org.zlab.upfuzz.fuzzingengine.Config;

import java.nio.file.Path;
import java.nio.file.Paths;

public class CorpusDefault extends Corpus {

    private static final String queueName = "CorpusDefault_BC";
    private static final Path queuePath = Paths.get(Config.getConf().corpus)
            .resolve(Config.getConf().system)
            .resolve(queueName);

    private int diskSeedId = 0;

    public CorpusDefault() {
        super(1, new double[] { 1 });
    }

    private enum QueueType {
        BC
    }

    @Override
    public void addSeed(Seed seed, boolean newOriBC, boolean newUpBC,
            boolean newOriFC, boolean newUpFC, boolean newBCAfterUpgrade,
            boolean newBCAfterDowngrade, boolean newOriBoundaryChange,
            boolean newUpBoundaryChange, boolean newModifiedFormatCoverage,
            boolean newBCVD, boolean newFCVD) {
        if (newOriBC || newBCAfterUpgrade) {
            cycleQueues[0].addSeed(seed);
            if (Config.getConf().saveCorpusToDisk) {
                while (queuePath
                        .resolve("seed_" + diskSeedId).toFile().exists()) {
                    diskSeedId++;
                }
                Corpus.saveSeedQueueOnDisk(seed, queueName, diskSeedId);
            }
        }
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

    @Override
    public int initCorpus() {
        Path corpusPath = Paths.get(Config.getConf().corpus)
                .resolve(Config.getConf().system);
        System.out.println("corpusPath: " + corpusPath);
        if (!corpusPath.toFile().exists())
            return 0;
        if (!corpusPath.toFile().isDirectory()) {
            throw new RuntimeException(
                    "corpusPath is not a directory: " + corpusPath);
        }
        // process each queues
        int testId = 0;
        testId = Corpus.loadSeedIntoQueue(cycleQueues[QueueType.BC.ordinal()],
                queuePath.toFile(), testId);
        System.out.println("init testId: " + testId);
        return testId;
    }
}
