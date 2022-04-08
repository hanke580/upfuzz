package org.zlab.upfuzz.fuzzingengine.executor;

import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.zlab.upfuzz.CommandSequence;
import org.zlab.upfuzz.fuzzingengine.Config;

public abstract class Executor implements IExecutor {

    public enum FailureType {
        UPGRADE_FAIL,
        RESULT_INCONSISTENCY
    }

    public String executorID;
    public String systemID = "UnknowDS";
    public Config conf;
    protected CommandSequence commandSequence;
    protected CommandSequence validationCommandSequence;
    public List<String> oldVersionResult;
    public List<String> newVersionResult;

    public FailureType failureType;
    public String failureInfo;

    protected Executor(CommandSequence commandSequence, CommandSequence validationCommandSequence) {
        executorID = RandomStringUtils.randomAlphanumeric(8);
        this.commandSequence = commandSequence;
        this.validationCommandSequence = validationCommandSequence;

        oldVersionResult = null;
        newVersionResult = null;

        failureType = null;
        failureInfo = null;
    }

    protected Executor(CommandSequence commandSequence, CommandSequence validationCommandSequence, String systemID) {
        this(commandSequence, validationCommandSequence);
        this.systemID = systemID;
    }

    public String getSysExecID() {
        return systemID + "-" + executorID;
    }

    public void startup() {
    }

    public void teardown() {
    }

    public List<String> executeCommands(CommandSequence commandSequence) {
        return null;
    }

    public List<String> execute() {
        startup();
        executeCommands(commandSequence);
        saveSnapshot(); // Flush, and keep the data folder

        oldVersionResult = executeCommands(validationCommandSequence);
        // execute the second commands
        teardown();
        return oldVersionResult;
    }

    public int saveSnapshot() {return 0; }

}
