package org.zlab.upfuzz.fuzzingengine.server;

public class CorpusVersionDeltaFourQueue extends Corpus {
    /**
     * 0: FC-VD: 40%
     * 1: FC: 20%
     * 2: BC-VD: 25%
     * 3: BC: 15%
     */
    public CorpusVersionDeltaFourQueue() {
        super(4, new double[] { 0.4, 0.2, 0.25, 0.15 });
    }

    private enum QueueType {
        FC_VD, FC, BC_VD, BC
    }

    @Override
    public void addSeed(Seed seed, boolean newOriBC, boolean newUpBC,
            boolean newOriFC, boolean newUpFC, boolean newBCAfterUpgrade,
            boolean newBCAfterDowngrade) {
        // One seed can occur in multiple queues (representing higher energy)
        // However, for one coverage, it can only exist in either vd or non-vd
        // queue
        if (newOriFC ^ newUpFC) {
            cycleQueues[0].addSeed(seed);
        } else {
            if (newOriFC && newUpFC) {
                cycleQueues[1].addSeed(seed);
            }
        }

        if (newOriBC ^ newUpBC) {
            cycleQueues[2].addSeed(seed);
        } else {
            // examine whether any new coverage is reached
            if (newOriBC || newUpBC || newBCAfterUpgrade
                    || newBCAfterDowngrade) {
                cycleQueues[3].addSeed(seed);
            }
        }
    }

    @Override
    public void printInfo() {
        for (int i = 0; i < cycleQueues.length; i++) {
            System.out.format("|%30s|%30s|%30s|%30s|\n",
                    QueueType.values()[i],
                    "queue size : "
                            + cycleQueues[i].size(),
                    "index : "
                            + cycleQueues[i].getCurrentIndex(),
                    "");
        }
    }
}
