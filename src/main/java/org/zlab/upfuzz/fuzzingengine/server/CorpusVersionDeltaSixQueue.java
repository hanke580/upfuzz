package org.zlab.upfuzz.fuzzingengine.server;

import org.zlab.upfuzz.fuzzingengine.Config;

public class CorpusVersionDeltaSixQueue extends Corpus {

    public CorpusVersionDeltaSixQueue() {
        super(6,
                new double[] {
                        Config.getConf().formatDeltaSeedChoiceProb,
                        Config.getConf().branchDeltaSeedChoiceProb,
                        Config.getConf().formatCovSeedChoiceProb,
                        Config.getConf().branchCovSeedChoiceProb,
                        Config.getConf().branchCovAfterUpgSeedChoiceProb,
                        Config.getConf().branchCovAfterDowngSeedChoiceProb
                });
    }

    public enum QueueType {
        FC_VD, BC_VD, FC, BC, BC_After_Upgrade, BC_After_Downgrade
    }

    public Seed getSeed(QueueType type) {
        return cycleQueues[type.ordinal()].getNextSeed();
    }

    public Seed peekSeed(QueueType type) {
        return cycleQueues[type.ordinal()].peekSeed();
    }

    public void addSeed(Seed seed, QueueType type) {
        seed.setTimestamp(timestampGenerator.getAndIncrement());
        cycleQueues[type.ordinal()].addSeed(seed);
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
            boolean newBCAfterDowngrade) {
        // TODO
        // Currently, this class is not well wrapped
    }

    @Override
    public void printInfo() {
        // Print all six queues
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
