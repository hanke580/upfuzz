package org.zlab.upfuzz.fuzzingengine.executor;

public interface IExecutor {
    public void startup();

    public void teardown();

    public int executeCommands();

    public int execute();
}
