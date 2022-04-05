package org.zlab.upfuzz.fuzzingengine.executor;

import org.zlab.upfuzz.CommandSequence;

public interface IExecutor {
    public void startup();

    public void teardown();

    public int executeCommands(CommandSequence commandSequence);

    public int execute(CommandSequence commandSequence);

    /**
     * Given the generated snapshot, upgrade it to the new version.
     * Check whether any exception happens.
     */
    public int upgradeTest();
}
