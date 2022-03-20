package org.zlab.upfuzz.fuzzingengine.executor;


import java.util.UUID;

import org.zlab.upfuzz.CommandSequence;
import org.zlab.upfuzz.fuzzingengine.Config;

public abstract class Executor implements IExecutor {

    public UUID executorID;
    public Config conf;
    protected CommandSequence commandSequence;

    protected Executor(Config conf, CommandSequence testSeq) {
        this.conf = conf;
        executorID = UUID.randomUUID();
        commandSequence = testSeq;
    }

    public void startup() {
    }

    public void teardown() {
    }

    public int executeCommands() {
        return 0;
    }

    public int execute() {
        startup();
        executeCommands();
        teardown();
        return 0;
    }
}
