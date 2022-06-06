package org.zlab.upfuzz.fuzzingengine.executor;

import org.zlab.upfuzz.CommandSequence;

import java.util.List;

public interface IExecutor {

    public void startup();

    void teardown();

    List<String> executeCommands(CommandSequence commandSequence);

    void execute(CommandSequence commandSequence,
            CommandSequence validationCommandSequence, int testId);

    /**
     * Given the generated snapshot, upgrade it to the new version.
     * Check whether any exception happens.
     * @return
     */
    boolean upgradeTest();
}
