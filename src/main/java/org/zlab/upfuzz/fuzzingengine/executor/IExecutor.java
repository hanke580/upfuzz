package org.zlab.upfuzz.fuzzingengine.executor;

import java.util.List;
import org.zlab.upfuzz.CommandSequence;

public interface IExecutor {

    void startup() throws Exception;

    void teardown();

    List<String> executeCommands(List<String> commandList);

    /**
     * Given the generated snapshot, upgrade it to the new version.
     * Check whether any exception happens.
     * @return
     */
    boolean upgradeTest();
}
