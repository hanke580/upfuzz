package org.zlab.upfuzz.fuzzingengine.server;

import org.zlab.upfuzz.fuzzingengine.Config;
import java.util.Map;
import java.util.HashMap;

public class TestChoiceProbabilitiesVersionDeltaTwoGroups
        extends Probabilities {

    public TestChoiceProbabilitiesVersionDeltaTwoGroups() {
        super(5);
        probabilitiesHashMap.put(0,
                Config.getConf().formatVersionDeltaChoiceProb);
        probabilitiesHashMap.put(1,
                Config.getConf().branchVersionDeltaChoiceProb);
        probabilitiesHashMap.put(2,
                Config.getConf().formatCoverageChoiceProb);
        probabilitiesHashMap.put(3,
                Config.getConf().branchCoverageChoiceProb);
        probabilitiesHashMap.put(4,
                Config.getConf().boundaryRelatedSeedsChoiceProb);
    }
}
