package org.zlab.upfuzz.fuzzingengine.testplan;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.fuzzingengine.testplan.event.Event;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

public class FullStopUpgrade implements Serializable {

    static Logger logger = LogManager.getLogger(FullStopUpgrade.class);

    // We might also execute read in the middle and compare the results
    public int nodeNum;
    public List<String> commands;
    public List<String> validCommands;
    public Set<String> targetSystemStates;

    public FullStopUpgrade(int nodeNum, List<String> commands,
            List<String> validCommands,
            Set<String> targetSystemStates) {
        this.commands = commands;
        this.validCommands = validCommands;
        this.targetSystemStates = targetSystemStates;
        this.nodeNum = nodeNum;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("full stop upgrade:\n");
        sb.append("commands\n");
        for (String command : commands) {
            sb.append(command).append("\n");
        }
        sb.append("\n");
        sb.append("valid commands\n");
        for (String command : validCommands) {
            sb.append(command).append("\n");
        }
        sb.append("\n");
        sb.append("targetSystemStates commands\n");
        if (targetSystemStates != null) {
            for (String targetSystemState : targetSystemStates) {
                sb.append(targetSystemState).append("\n");
            }
        }
        sb.append("test plan end\n");
        return sb.toString();
    }

}
