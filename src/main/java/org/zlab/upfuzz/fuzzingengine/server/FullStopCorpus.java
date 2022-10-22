package org.zlab.upfuzz.fuzzingengine.server;

import org.zlab.upfuzz.fuzzingengine.testplan.FullStopUpgrade;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class FullStopCorpus {

    Queue<FullStopSeed> queue = new LinkedList();

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
