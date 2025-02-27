package org.zlab.upfuzz.fuzzingengine.testplan;

import java.io.Serializable;
import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.fuzzingengine.testplan.event.Event;
import org.zlab.upfuzz.fuzzingengine.testplan.event.fault.Fault;
import org.zlab.upfuzz.fuzzingengine.testplan.event.fault.FaultRecover;
import org.zlab.upfuzz.fuzzingengine.testplan.event.upgradeop.UpgradeOp;
import org.zlab.upfuzz.utils.Pair;
import org.zlab.upfuzz.utils.Utilities;

public class TestPlan implements Serializable {
    static Logger logger = LogManager.getLogger(TestPlan.class);

    public int nodeNum;
    public List<Event> events;

    // ----read results comparison----
    public List<String> validationCommands;
    public List<String> validationReadResultsOracle;

    public TestPlan(int nodeNum, List<Event> events,
            List<String> validationCommands,
            List<String> validationReadResultsOracle) {

        assert validationCommands != null;
        assert validationReadResultsOracle != null;

        this.nodeNum = nodeNum;
        this.events = events;
        this.validationCommands = validationCommands;
        this.validationReadResultsOracle = validationReadResultsOracle;
    }

    public List<Event> getEvents() {
        return events;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("test plan:\n");
        for (Event event : events) {
            sb.append(event).append("\n");
        }
        sb.append("test plan end\n");

        sb.append("validation commands: \n");
        for (String cmd : validationCommands) {
            sb.append(cmd).append("\n");
        }
        sb.append("\n");
        return sb.toString();
    }

    public void mutate() {
        // mutate a test plan
        // We only mutate the upgradeOp and fault
        // We still want to keep the entire test sequence
        // correct right?
        // How to we keep it right
        // Some constraints
        // crash1
        // shouldn't upgrade node1

        // Some options
        // Inject another fault

        Random rand = new Random();

        List<Integer> faultIdxes = getIdxes(events, Fault.class);
        List<Integer> faultRecoverIdxes = getIdxes(events, FaultRecover.class);
        List<Integer> upgradeOpIdxes = getIdxes(events, UpgradeOp.class);

        int mutateType;

        // TODO: re-interleave the commands and faults
        while (true) {
            if (Config.getConf().shuffleUpgradeOrder && nodeNum > 1) {
                mutateType = rand.nextInt(4);
            } else {
                mutateType = rand.nextInt(3);
            }
            if (mutateType == 0) {
                // Inject a fault
                Pair<Fault, FaultRecover> faultPair = Fault
                        .randomGenerateFault(Config.getConf().nodeNum);
                // Pick a position and inject it
                int pos1 = rand.nextInt(events.size() + 1);
                assert faultPair != null;
                events.add(pos1, faultPair.left);
                if (faultPair.right != null) {
                    int pos2 = Utilities.randWithRange(rand, pos1 + 1,
                            events.size() + 1);
                    events.add(pos2, faultPair.right);
                }
                return;
            } else if (mutateType == 1) {
                // Remove a fault
                if (faultIdxes.isEmpty())
                    continue;
                int pos = rand.nextInt(faultIdxes.size());
                events.remove((int) faultIdxes.get(pos));
                return;
            } else if (mutateType == 2) {
                // Inject a fault recover
                if (faultIdxes.isEmpty())
                    continue;
                int pos1 = rand.nextInt(faultIdxes.size());
                Fault fault = (Fault) events.get(faultIdxes.get(pos1));
                FaultRecover faultRecover = fault.generateRecover();
                int pos2 = Utilities.randWithRange(rand, faultIdxes.get(pos1),
                        events.size() + 1);
                events.add(pos2, faultRecover);
                return;
            } else if (mutateType == 3) {
                // Remove a fault recover
                if (faultRecoverIdxes.isEmpty())
                    continue;
                int pos = rand.nextInt(faultRecoverIdxes.size());
                events.remove((int) faultRecoverIdxes.get(pos));
                return;
            } else if (mutateType == 4) {
                logger.debug("Change the Upgrade Order");
                // Change the upgrade order
                // Find the upgrade operation
                // swap two of them
                Collections.shuffle(upgradeOpIdxes);
                int pos1 = upgradeOpIdxes.get(0);
                int pos2 = upgradeOpIdxes.get(1);
                int nodeIdx = ((UpgradeOp) events.get(pos1)).nodeIndex;
                ((UpgradeOp) events.get(pos1)).nodeIndex = ((UpgradeOp) events
                        .get(pos2)).nodeIndex;
                ((UpgradeOp) events.get(pos2)).nodeIndex = nodeIdx;
            }
            throw new RuntimeException(
                    "mutateType[%d] is out of range");
        }
    }

    public List<Integer> getIdxes(List<Event> events,
            Class<? extends Event> clazz) {
        List<Integer> idxes = new LinkedList<>();
        for (int i = 0; i < events.size(); i++) {
            if (clazz.isInstance(events.get(i)))
                idxes.add(i);
        }
        return idxes;
    }
}
