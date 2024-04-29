package org.zlab.upfuzz.fuzzingengine.executor;

import java.util.List;

public interface IExecutor {

    boolean startup();

    void teardown();

    List<String> executeCommands(List<String> commandList);

    boolean rollingUpgrade();

    boolean fullStopUpgrade();

    boolean downgrade();

    void flush();
}
