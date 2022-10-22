package org.zlab.upfuzz.fuzzingengine.testplan;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.fuzzingengine.testplan.event.command.ShellCommand;

import java.io.Serializable;
import java.util.List;

public class FullStopUpgrade implements Serializable {

    static Logger logger = LogManager.getLogger(FullStopUpgrade.class);

    // We might also execute read in the middle and compare the results
    public List<ShellCommand> commands;
    public List<ShellCommand> validCommands;
}
