package org.zlab.upfuzz.fuzzingengine.executor;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.RandomStringUtils;
import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.CommandPool;
import org.zlab.upfuzz.CommandSequence;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.utils.Pair;

public abstract class Executor implements IExecutor {

    public enum FailureType {
        UPGRADE_FAIL, RESULT_INCONSISTENCY
    }

    public String executorID;
    public String systemID = "UnknowDS";
    public Config conf;
    public CommandSequence commandSequence;
    public CommandSequence validationCommandSequence;
    public List<String> oldVersionResult;
    public List<String> newVersionResult;

    public FailureType failureType;
    public String failureInfo;

    protected Executor(CommandSequence commandSequence,
            CommandSequence validationCommandSequence) {
        executorID = RandomStringUtils.randomAlphanumeric(8);
        this.commandSequence = commandSequence;
        this.validationCommandSequence = validationCommandSequence;

        oldVersionResult = null;
        newVersionResult = null;

        failureType = null;
        failureInfo = null;
    }

    protected Executor(CommandSequence commandSequence,
            CommandSequence validationCommandSequence, String systemID) {
        this(commandSequence, validationCommandSequence);
        this.systemID = systemID;
    }

    public void reset(CommandSequence commandSequence,
            CommandSequence validationCommandSequence) {
        executorID = RandomStringUtils.randomAlphanumeric(8);
        this.commandSequence = commandSequence;
        this.validationCommandSequence = validationCommandSequence;

        oldVersionResult = null;
        newVersionResult = null;

        failureType = null;
        failureInfo = null;
    }

    public String getSysExecID() {
        return systemID + "-" + executorID;
    }

    abstract public void startup() throws Exception;

    abstract public void teardown();

    abstract public List<String> executeCommands(
            CommandSequence commandSequence);

    public static Pair<CommandSequence, CommandSequence> prepareCommandSequence(
            CommandPool commandPool, Class<? extends State> stateClass) {
        CommandSequence commandSequence = null;
        CommandSequence validationCommandSequence = null;
        try {
            commandSequence = CommandSequence.generateSequence(
                    commandPool.commandClassList,
                    commandPool.createCommandClassList, stateClass, null);
            // TODO: If it's generating read with a initial state, no need to generate with createTable...
            validationCommandSequence = CommandSequence.generateSequence(
                    commandPool.readCommandClassList, null, stateClass,
                    commandSequence.state);
        } catch (NoSuchMethodException | InvocationTargetException
                | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return new Pair<>(commandSequence, validationCommandSequence);
    }

    public static CommandSequence prepareValidationCommandSequence(
            CommandPool commandPool, State state) {
        CommandSequence validationCommandSequence = null;
        try {
            validationCommandSequence = CommandSequence.generateSequence(
                    commandPool.readCommandClassList, null, state.getClass(),
                    state);
        } catch (NoSuchMethodException | InvocationTargetException
                | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return validationCommandSequence;
    }

    public List<String> execute() {
        try {
            startup();
            executeCommands(commandSequence);
            // Upgrade preparation
            // also do flush, and keep the data folder
            upgrade();

            oldVersionResult = executeCommands(validationCommandSequence);
            // execute the second commands
            teardown();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return oldVersionResult;
    }

    abstract public void upgrade() throws Exception;
}
