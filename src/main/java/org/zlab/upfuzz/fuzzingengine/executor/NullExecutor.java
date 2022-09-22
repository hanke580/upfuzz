package org.zlab.upfuzz.fuzzingengine.executor;

import java.util.List;

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
    public void startup() {
    }

    @Override
    public void teardown() {
    }

    @Override
    public void upgradeTeardown() {
    }

    @Override
    public int saveSnapshot() {
        return 0;
    }

    @Override
    public int moveSnapShot() {
        return 0;
    }

    @Override
    public boolean upgradeTest() {
        return true;
    }

    @Override
    public List<String> executeCommands(List<String> commandList) {
        return null;
    }

    @Override
    public String execShellCommand(ShellCommand command) {
        return null;

    }

    @Override
    public void execNormalCommand(Command command) {

    }

    @Override
    public void upgrade() throws Exception {
    }
}
