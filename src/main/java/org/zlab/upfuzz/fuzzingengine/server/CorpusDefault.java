package org.zlab.upfuzz.fuzzingengine.server;

public class CorpusDefault extends Corpus {
    /**
     * 1: FC: 80%
     * 3: BC: 20%
     */
    public CorpusDefault() {
        super(1, new double[] { 1 });
    }

    public enum QueueType {
        FORMAT_COVERAGE, BRANCH_COVERAGE
    }

    @Override
    public void addSeed(Seed seed, boolean newOriBC, boolean newUpBC,
            boolean newOriFC, boolean newUpFC, boolean newBCAfterUpgrade,
            boolean newBCAfterDowngrade) {
        if (newOriBC || newBCAfterUpgrade) {
            cycleQueues[0].addSeed(seed);
        }
    }

    @Override
    public void printInfo() {
        for (int i = 0; i < cycleQueues.length; i++) {
            System.out.format("|%30s|%30s|%30s|%30s|\n",
                    CorpusVersionDeltaSixQueue.QueueType.values()[i],
                    "queue size : "
                            + cycleQueues[i].size(),
                    "index : "
                            + cycleQueues[i].getCurrentIndex(),
                    "");
        }
    }
}
