package org.zlab.upfuzz.fuzzingengine.server;

import java.util.Map;
import java.util.HashMap;

public abstract class Probabilities {
    int probabilitiesCount;
    Map<Integer, Double> probabilitiesHashMap;

    public Probabilities(int probabilitiesCount) {
        this.probabilitiesCount = probabilitiesCount;
        this.probabilitiesHashMap = new HashMap<>();
    }

    public double[] getCumulativeProbabilities() {
        double[] cumulativeSeedChoiceProbabilities = new double[probabilitiesCount];
        cumulativeSeedChoiceProbabilities[0] = probabilitiesHashMap.get(0);
        for (int i = 1; i < probabilitiesCount; i++) {
            cumulativeSeedChoiceProbabilities[i] = cumulativeSeedChoiceProbabilities[i
                    - 1]
                    + probabilitiesHashMap.get(i);
        }
        return cumulativeSeedChoiceProbabilities;
    }
}
