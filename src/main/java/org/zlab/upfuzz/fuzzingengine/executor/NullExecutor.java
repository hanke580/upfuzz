package org.zlab.upfuzz.fuzzingengine.executor;

import java.util.List;
import java.util.Map;

import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.CommandSequence;
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
    public void teardown() {
    }

    @Override
    public void upgradeTeardown() {
    }

    @Override
    public String execShellCommand(ShellCommand command) {
        return null;
    }

}
