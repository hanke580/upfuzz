package org.zlab.upfuzz.fuzzingengine.server;

public class FullStopCorpus {

    CycleQueue<FullStopSeed> cycleQueue = new CycleQueue<>();

    public FullStopSeed getSeed() {
        return cycleQueue.getNextSeed();
    }

    public void addSeed(FullStopSeed seed) {
        cycleQueue.addSeed(seed);
    }

    public boolean isEmpty() {
        return cycleQueue.isEmpty();
    }

}
