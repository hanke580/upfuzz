package org.zlab.upfuzz.fuzzingengine.executor;

import java.util.List;

import org.zlab.upfuzz.CommandSequence;

/* NullExecutor
 * DO NOTHING
 * */
public class NullExecutor extends Executor {
    public NullExecutor(CommandSequence commandSequence, CommandSequence validationCommandSequence) {
        super(commandSequence, validationCommandSequence);
    }

    @Override
    public List<String> execute() {
        return null;
    }

    @Override
    public boolean upgradeTest() {
        return true;
    }

    @Override
    public void startup() {
    }

    @Override
    public void teardown() {
    }

    @Override
    public List<String> executeCommands(CommandSequence commandSequence) {
        return null;
    }
}
