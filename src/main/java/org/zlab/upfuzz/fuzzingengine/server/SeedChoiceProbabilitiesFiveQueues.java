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
}
