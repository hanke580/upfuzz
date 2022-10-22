package org.zlab.upfuzz.fuzzingengine.executor;

import java.util.List;

public interface IExecutor {

    void startup();

    void teardown();

    List<String> executeCommands(List<String> commandList);

    boolean rollingUpgrade();

    boolean fullStopUpgrade();
}
