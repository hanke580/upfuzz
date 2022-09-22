package org.zlab.upfuzz.fuzzingengine.testplan.event.fault;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.fuzzingengine.testplan.event.Event;
import org.zlab.upfuzz.utils.Pair;
import org.zlab.upfuzz.utils.Utilities;

import java.util.List;
import java.util.Random;

public abstract class Fault implements Event {
    static Logger logger = LogManager.getLogger(Fault.class);

    public static Random rand = new Random();
    static FaultPool.FaultType[] faultTypes = FaultPool.FaultType.values();

    public static Pair<Fault, FaultRecover> randomGenerateFault(int nodeNum) {
        assert nodeNum > 0;

        if (nodeNum == 1) {
            // We can only do nodeFailure
            Fault nodeFailure = new NodeFailure(0);
            return new Pair<>(nodeFailure, nodeFailure.generateRecover());
        }

        int faultIdx = rand.nextInt(faultTypes.length);
        Fault fault = null;
        switch (faultTypes[faultIdx]) {
        case IsolateFailure -> {
            int nodeIndex = rand.nextInt(nodeNum);
            fault = new IsolateFailure(nodeIndex);
            break;
        }
        case LinkFailure -> {
            List<Integer> nodeIndexes = Utilities.pickKoutofN(2, nodeNum);
            if (nodeIndexes == null || nodeIndexes.isEmpty()) {
                logger.error("Problem with node indexes");
            }
            int nodeIndex1 = nodeIndexes.get(0);
            int nodeIndex2 = nodeIndexes.get(1);
            fault = new LinkFailure(nodeIndex1, nodeIndex2);
            break;
        }
        case NodeFailure -> {
            int nodeIndex = rand.nextInt(nodeNum);
            fault = new NodeFailure(nodeIndex);
            break;
        }
        }
        if (fault == null)
            return null;
        return new Pair<>(fault, fault.generateRecover());
    }

    abstract public FaultRecover generateRecover();

}
