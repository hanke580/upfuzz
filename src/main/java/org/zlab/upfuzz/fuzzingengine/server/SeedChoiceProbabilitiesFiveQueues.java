package org.zlab.upfuzz.fuzzingengine.server;

import org.zlab.upfuzz.fuzzingengine.Config;
import java.util.Map;
import java.util.HashMap;

public class SeedChoiceProbabilitiesFiveQueues extends Probabilities {

    public SeedChoiceProbabilitiesFiveQueues() {
        super(5);
        probabilitiesHashMap.put(0, Config
                .getConf().FC_VD_PROB_CorpusVersionDeltaFiveQueueWithBoundary);
        probabilitiesHashMap.put(1, Config
                .getConf().FC_PROB_CorpusVersionDeltaFiveQueueWithBoundary);
        probabilitiesHashMap.put(2, Config
                .getConf().BC_VD_PROB_CorpusVersionDeltaFiveQueueWithBoundary);
        probabilitiesHashMap.put(3, Config
                .getConf().BC_PROB_CorpusVersionDeltaFiveQueueWithBoundary);
        probabilitiesHashMap.put(4, Config
                .getConf().BoundaryChange_PROB_CorpusVersionDeltaFiveQueueWithBoundary);
    }

    // @Override
    // public double[] getCumulativeProbabilities() {
    // double[] cumulativeSeedChoiceProbabilities = new
    // double[probabilitiesCount];
    // cumulativeSeedChoiceProbabilities[0] = probabilitiesHashMap.get(0);

    // System.out.println("probabilities hashmap size: " +
    // probabilitiesHashMap.keySet().size());
    // for (int i = 1; i < probabilitiesCount; i++) {
    // cumulativeSeedChoiceProbabilities[i] =
    // cumulativeSeedChoiceProbabilities[i
    // - 1]
    // + probabilitiesHashMap.get(i);
    // }

    // return cumulativeSeedChoiceProbabilities;
    // }
}
