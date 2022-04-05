package org.zlab.upfuzz.fuzzingengine.executor;

import java.util.UUID;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.zlab.upfuzz.CommandSequence;
import org.zlab.upfuzz.fuzzingengine.Config;

public abstract class Executor implements IExecutor {

    public String executorID;
    public String systemID = "UnknowDS";
    public Config conf;
    protected CommandSequence commandSequence;

    protected Executor(CommandSequence testSeq) {
        executorID = RandomStringUtils.randomAlphanumeric(8);
        commandSequence = testSeq;
    }

    protected Executor(CommandSequence testSeq, String systemID) {
        this(testSeq);
        this.systemID = systemID;
    }

    public String getSysExecID() {
        return systemID + "-" + executorID;
    }

    public void startup() {
    }

    public void teardown() {
    }

    public int executeCommands(CommandSequence commandSequence) {
        return 0;
    }

    public int execute(CommandSequence commandSequence) {
        startup();
        executeCommands(commandSequence);
        teardown();
        return 0;
    }
}
