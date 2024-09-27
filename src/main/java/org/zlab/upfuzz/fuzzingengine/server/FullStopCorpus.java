package org.zlab.upfuzz.fuzzingengine.server;

import java.util.PriorityQueue;

public class FullStopCorpus {

    PriorityQueue<FullStopSeed> queue = new PriorityQueue<>();

    public FullStopSeed getSeed() {
        if (queue.isEmpty())
            return null;
        return queue.poll();
    }

    public FullStopSeed peekSeed() {
        if (queue.isEmpty())
            return null;
        return queue.peek();
    }

    // Add one interesting seed to corpus
    public boolean addSeed(FullStopSeed seed) {
        queue.add(seed);
        return true;
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }
}
