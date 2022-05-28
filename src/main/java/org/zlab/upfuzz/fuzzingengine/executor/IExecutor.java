/* (C)2022 */
package org.zlab.upfuzz.fuzzingengine.executor;

import java.util.List;
import org.zlab.upfuzz.CommandSequence;

public interface IExecutor {

    public void startup();

    void teardown();

    List<String> executeCommands(CommandSequence commandSequence);

    List<String> execute(
            CommandSequence commandSequence, CommandSequence validationCommandSequence, int testId);

    /**
     * Given the generated snapshot, upgrade it to the new version. Check whether any exception
     * happens.
     *
     * @return
     */
    boolean upgradeTest();
}
