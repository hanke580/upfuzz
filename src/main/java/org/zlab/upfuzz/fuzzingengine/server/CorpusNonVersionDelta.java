package org.zlab.upfuzz.fuzzingengine.server;

import org.zlab.upfuzz.fuzzingengine.Config;

public class CorpusNonVersionDelta extends Corpus {
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
        }
        if (newOriBC || newBCAfterUpgrade) {
            cycleQueues[1].addSeed(seed);
        }
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
