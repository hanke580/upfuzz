package org.zlab.upfuzz.fuzzingengine.executor;

import org.zlab.upfuzz.CommandSequence;

import java.util.List;

public interface IExecutor {

    public void startup();

    public void teardown();

    public List<String> executeCommands(CommandSequence commandSequence);

    public List<String>  execute();

    /**
     * Given the generated snapshot, upgrade it to the new version.
     * Check whether any exception happens.
     * @return
     */
    public boolean upgradeTest();
}
