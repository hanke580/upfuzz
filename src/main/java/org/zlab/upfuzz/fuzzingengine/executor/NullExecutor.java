package org.zlab.upfuzz.fuzzingengine.executor;

import org.zlab.upfuzz.fuzzingengine.testplan.event.command.ShellCommand;

/* NullExecutor
 * DO NOTHING
 * */
public class NullExecutor extends Executor {
    public NullExecutor() {
        super();
    }

    @Override
    public boolean startup() {
        return false;
    }

    @Override
    public String execShellCommand(ShellCommand command) {
        return null;
    }
}
