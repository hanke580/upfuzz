package org.zlab.upfuzz.fuzzingengine.executor;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.RandomStringUtils;
import org.zlab.upfuzz.CommandPool;
import org.zlab.upfuzz.CommandSequence;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.fuzzingengine.Packet.TestPacket;
import org.zlab.upfuzz.fuzzingengine.Server.Seed;
import org.zlab.upfuzz.utils.Pair;

public abstract class Executor implements IExecutor {

    public enum FailureType {
        UPGRADE_FAIL, RESULT_INCONSISTENCY
    }

    public String executorID;
    public String systemID = "UnknowDS";

    public Map<Integer, Pair<List<String>, List<String>>> testId2commandSequence;
    public Map<Integer, List<String>> testId2oldVersionResult;
    public Map<Integer, List<String>> testId2newVersionResult;

    protected Executor() {
        testId2commandSequence = new HashMap<>();
        testId2oldVersionResult = new HashMap<>();
        testId2newVersionResult = new HashMap<>();
        executorID = RandomStringUtils.randomAlphanumeric(8);
    }

    protected Executor(String systemID) {
        this();
        this.systemID = systemID;
    }

    public void clearState() {
        testId2commandSequence.clear();
        testId2oldVersionResult.clear();
        testId2newVersionResult.clear();
    }

    public String getSysExecID() {
        return systemID + "-" + executorID;
    }

    abstract public void startup();

    abstract public void teardown();

    abstract public void upgradeteardown();

    abstract public List<String> executeCommands(List<String> commandList);

    public static Seed generateSeed(CommandPool commandPool,
            Class<? extends State> stateClass) {
        CommandSequence originalCommandSequence = null;
        CommandSequence validationCommandSequence = null;
        try {
            originalCommandSequence = CommandSequence.generateSequence(
                    commandPool.commandClassList,
                    commandPool.createCommandClassList, stateClass, null);
            validationCommandSequence = CommandSequence.generateSequence(
                    commandPool.readCommandClassList, null, stateClass,
                    originalCommandSequence.state);
            return new Seed(originalCommandSequence, validationCommandSequence);
        } catch (NoSuchMethodException | InvocationTargetException
                | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
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

    public void execute(CommandSequence commandSequence,
            CommandSequence validationCommandSequence, int testId) {
        // startup();
        testId2commandSequence.put(testId,
                new Pair<>(commandSequence.getCommandStringList(),
                        validationCommandSequence.getCommandStringList()));
        executeCommands(commandSequence.getCommandStringList());
        // saveSnapshot(); // Flush, only keep the data folder
    }

    public void execute(TestPacket testPacket) {
        testId2commandSequence.put(testPacket.testPacketID,
                new Pair<>(testPacket.originalCommandSequenceList,
                        testPacket.validationCommandSequneceList));
        executeCommands(testPacket.originalCommandSequenceList);
    }

    public List<String> executeRead(int testId) {
        List<String> oldVersionResult = executeCommands(
                testId2commandSequence.get(testId).right);

        testId2oldVersionResult.put(testId, oldVersionResult);
        return oldVersionResult;
    }

    abstract public int saveSnapshot();

    abstract public int moveSnapShot();

    abstract public void upgrade() throws Exception;

    public Pair<Boolean, String> checkResultConsistency(List<String> oriResult,
            List<String> upResult) {
        return new Pair<>(true, "");
    }

}
