package org.zlab.upfuzz.fuzzingengine.testplan.event.fault;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.fuzzingengine.testplan.event.Event;
import org.zlab.upfuzz.utils.Pair;
import org.zlab.upfuzz.utils.Utilities;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public abstract class Fault extends Event {
    static Logger logger = LogManager.getLogger(Fault.class);

    public static Random rand = new Random();
    static FaultPool.FaultType[] faultTypes = FaultPool.FaultType.values();

    public Fault(String type) {
        super(type);
    }

    public static Pair<Fault, FaultRecover> randomGenerateFault(int nodeNum) {
        assert nodeNum > 0;

        if (nodeNum == 1) {
            // We can only do nodeFailure
            Fault nodeFailure = new NodeFailure(0);
            return new Pair<>(nodeFailure, nodeFailure.generateRecover());
        }

        int faultIdx = rand.nextInt(faultTypes.length);
        Fault fault = null;
        int nodeIndex;
        switch (faultTypes[faultIdx]) {
        case IsolateFailure:
            nodeIndex = rand.nextInt(nodeNum);
            fault = new IsolateFailure(nodeIndex);
            break;
        case LinkFailure:
            List<Integer> nodeIndexes = Utilities.pickKoutofN(2, nodeNum);
            if (nodeIndexes == null || nodeIndexes.isEmpty()) {
                logger.error("Problem with node indexes");
            }
            int nodeIndex1 = nodeIndexes.get(0);
            int nodeIndex2 = nodeIndexes.get(1);
            fault = new LinkFailure(nodeIndex1, nodeIndex2);
            break;

        case NodeFailure:
            // for hdfs, avoid crash NN
            if (Config.getConf().system.equals("hdfs")) {
                nodeIndex = Utilities.randWithRange(rand, 1, nodeNum);
            } else {
                nodeIndex = rand.nextInt(nodeNum);
            }
            fault = new NodeFailure(nodeIndex);
            break;
        case RestartFailure:
            nodeIndex = rand.nextInt(nodeNum);
            fault = new RestartFailure(nodeIndex);
            break;
        }

        // Debug Purpose: always generate restart failure
        // nodeIndex = rand.nextInt(nodeNum);
        // fault = new RestartFailure(nodeIndex);

        if (fault == null)
            return null;

        // TODO: Set a certain probability that the fault recover could be null?
        FaultRecover faultRecover = fault.generateRecover();
        if (!Config.getConf().alwaysRecoverFault) {
            if (rand.nextFloat() < Config.getConf().noRecoverProb) {
                faultRecover = null;
            }
        }

        return new Pair<>(fault, faultRecover);
    }

    public static List<Pair<Fault, FaultRecover>> randomGenerateFaults(
            int nodeNum, int faultNum) {
        List<Pair<Fault, FaultRecover>> faults = new LinkedList<>();
        for (int i = 0; i < faultNum; i++) {
            faults.add(randomGenerateFault(nodeNum));
        }
        return faults;
    }

    abstract public FaultRecover generateRecover();

}
