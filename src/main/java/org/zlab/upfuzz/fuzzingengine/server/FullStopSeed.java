package org.zlab.upfuzz.fuzzingengine.server;

import java.util.List;

public class FullStopSeed implements Comparable<FullStopSeed> {

    public Seed seed;
    public List<String> validationReadResults;

    public FullStopSeed(Seed seed,
            List<String> validationReadResults) {
        this.seed = seed;
        this.validationReadResults = validationReadResults;
    }

    @Override
    public int compareTo(FullStopSeed o) {
        if (o.seed == null)
            return 1;
        return seed.compareTo(o.seed);
    }
}
