package org.zlab.upfuzz.fuzzingengine.server;

import java.util.Map;

public class FullStopSeed {
    public Seed seed;
    public int nodeNum;
    public Map<Integer, Map<String, String>> targetSystemStateResults;

    public FullStopSeed(Seed seed, int nodeNum,
            Map<Integer, Map<String, String>> targetSystemStateResults) {
        this.seed = seed;
        this.nodeNum = nodeNum;
        this.targetSystemStateResults = targetSystemStateResults;
    }
}
