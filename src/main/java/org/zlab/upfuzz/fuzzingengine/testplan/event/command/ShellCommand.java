package org.zlab.upfuzz.fuzzingengine.testplan.event.command;

import org.zlab.upfuzz.fuzzingengine.server.Seed;
import org.zlab.upfuzz.fuzzingengine.testplan.event.Event;

import java.util.LinkedList;
import java.util.List;

/**
 * commands which are executed in the shell of
 * system. Like cqlsh shell, hbase shell.
 */
public class ShellCommand extends Event {
    String command;

    public ShellCommand(String command) {
        super("ShellCommand");
        this.command = command;
    }

    public static List<Event> seedWriteCmd2Events(Seed seed) {
        if (seed == null)
            return null;
        List<Event> events = new LinkedList<>();
        for (String command : seed.originalCommandSequence
                .getCommandStringList()) {
            events.add(new ShellCommand(command));
        }
        return events;
    }

    public static List<Event> seedValidationCmd2Events(Seed seed) {
        if (seed == null)
            return null;
        List<Event> events = new LinkedList<>();
        for (String command : seed.validationCommandSequence
                .getCommandStringList()) {
            events.add(new ShellCommand(command));
        }
        return events;
    }

    public String getCommand() {
        return command;
    }

    @Override
    public String toString() {
        return String.format("[Command] Execute {%s}", command);
    }
}
