package org.zlab.upfuzz.fuzzingengine;

import java.util.List;

import org.zlab.upfuzz.CommandSequence;
import org.zlab.upfuzz.fuzzingengine.executor.Executor;

public class NullExecutor extends Executor {

    public NullExecutor(CommandSequence commandSequence,
            CommandSequence validationCommandSequence) {
        super(commandSequence, validationCommandSequence, "NULL");
    }

    @Override
    public boolean upgradeTest() {
        return true;
    }

    @Override
    public void startup() throws Exception {
    }

    @Override
    public void teardown() {
    }

    @Override
    public List<String> executeCommands(CommandSequence commandSequence) {
        return null;
    }

    @Override
    public void upgrade() throws Exception {
    }

}
