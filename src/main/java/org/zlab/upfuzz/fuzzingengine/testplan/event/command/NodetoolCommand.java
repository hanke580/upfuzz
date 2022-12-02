package org.zlab.upfuzz.fuzzingengine.testplan.event.command;

import org.zlab.upfuzz.fuzzingengine.testplan.event.Event;

public class NodetoolCommand extends Event {
    String command;

    public NodetoolCommand(String command) {
        super("NodetoolCommand");
        this.command = command;
    }

    public String getCommand() {
        return command;
    }

    @Override
    public String toString() {
        return String.format("[Nodetool] Execute {%s}", command);
    }

}
