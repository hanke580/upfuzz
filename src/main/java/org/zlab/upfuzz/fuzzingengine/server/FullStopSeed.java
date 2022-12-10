package org.zlab.upfuzz.fuzzingengine.server;

import java.util.List;
import java.util.Map;

public class FullStopSeed implements Comparable<FullStopSeed> {
    public Seed seed;
    public int nodeNum;
    public Map<Integer, Map<String, String>> targetSystemStateResults;

    public List<String> validationReadResults; // only save old version read
                                               // results

    public FullStopSeed(Seed seed, int nodeNum,
            Map<Integer, Map<String, String>> targetSystemStateResults,
            List<String> validationReadResults) {
        this.seed = seed;
        this.nodeNum = nodeNum;
        this.targetSystemStateResults = targetSystemStateResults;
        this.validationReadResults = validationReadResults;
    }

    @Override
    public int compareTo(FullStopSeed o) {
        if (o.seed == null)
            return 1;
        return seed.compareTo(o.seed);
    }
}
