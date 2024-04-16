package org.zlab.upfuzz.fuzzingengine.server;

import org.zlab.upfuzz.fuzzingengine.Config;
import java.util.Map;
import java.util.HashMap;

public class TestChoiceProbabilitiesVersionDeltaTwoGroups
        extends Probabilities {

    public TestChoiceProbabilitiesVersionDeltaTwoGroups() {
        super(4);
        probabilitiesHashMap.put(0,
                Config.getConf().formatVersionDeltaChoiceProb);
        probabilitiesHashMap.put(1,
                Config.getConf().branchVersionDeltaChoiceProb);
        probabilitiesHashMap.put(2,
                Config.getConf().formatCoverageChoiceProb);
        probabilitiesHashMap.put(3,
                Config.getConf().branchCoverageChoiceProb);
    }

    // @Override
    // public double[] getCumulativeProbabilities() {
    // double[] cumulativeSeedChoiceProbabilities = new
    // double[probabilitiesCount];
    // cumulativeSeedChoiceProbabilities[0] = probabilitiesHashMap.get(0);

    // for (int i = 1; i < probabilitiesCount; i++) {
    // cumulativeSeedChoiceProbabilities[i] =
    // cumulativeSeedChoiceProbabilities[i
    // - 1]
    // + probabilitiesHashMap.get(i);
    // }

    // return cumulativeSeedChoiceProbabilities;
    // }
}
