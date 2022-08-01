package org.zlab.upfuzz.fuzzingengine.executor;

import java.lang.reflect.InvocationTargetException;
import java.rmi.UnexpectedException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jacoco.core.data.ExecutionDataStore;
import org.zlab.upfuzz.CommandPool;
import org.zlab.upfuzz.CommandSequence;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.docker.IDockerCluster;
import org.zlab.upfuzz.fuzzingengine.AgentServerHandler;
import org.zlab.upfuzz.fuzzingengine.AgentServerSocket;
import org.zlab.upfuzz.fuzzingengine.Packet.TestPacket;
import org.zlab.upfuzz.fuzzingengine.Server.Seed;
import org.zlab.upfuzz.utils.Pair;

public abstract class Executor implements IExecutor {
    protected final Logger logger = LogManager.getLogger(getClass());

    public int agentPort;

    public Long timestamp = 0L;

    public enum FailureType {
        UPGRADE_FAIL, RESULT_INCONSISTENCY
    }

    public String executorID;
    public String systemID = "UnknowDS";

    public Map<Integer, Pair<List<String>, List<String>>> testId2commandSequence;
    public Map<Integer, List<String>> testId2oldVersionResult;
    public Map<Integer, List<String>> testId2newVersionResult;

    public IDockerCluster dockerCluster;

    /**
     * key: String -> agentId value: Codecoverage for this agent
     */
    public Map<String, ExecutionDataStore> agentStore;

    /* key: String -> agent Id
     * value: ClientHandler -> the socket to a agent */
    public Map<String, AgentServerHandler> agentHandler;

    /* key: UUID String -> executor Id
     * value: List<String> -> list of all alive agents with the executor Id */
    public Map<String, List<String>> sessionGroup;

    /* socket for client and agents to communicate*/
    public AgentServerSocket agentSocket;

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

    public Map<Integer, List<String>> getTestId2newVersionResult() {
        return testId2newVersionResult;
    }

    public void setTestId2newVersionResult(
            Map<Integer, List<String>> testId2newVersionResult) {
        this.testId2newVersionResult = testId2newVersionResult;
    }

    public Map<Integer, List<String>> getTestId2oldVersionResult() {
        return testId2oldVersionResult;
    }

    public void setTestId2oldVersionResult(
            Map<Integer, List<String>> testId2oldVersionResult) {
        this.testId2oldVersionResult = testId2oldVersionResult;
    }

    public void clearState() {
        testId2commandSequence.clear();
        testId2oldVersionResult.clear();
        testId2newVersionResult.clear();
    }

    public String getSysExecID() {
        return systemID + "-" + executorID;
    }

    abstract public void startup() throws Exception;

    abstract public void teardown();

    abstract public void upgradeTeardown();

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

    public ExecutionDataStore collect(String version) {
        List<String> agentIdList = sessionGroup
                .get(executorID + "_" + version);
        if (agentIdList == null) {
            new UnexpectedException(
                    "No agent connection with executor " + executorID)
                            .printStackTrace();
            return null;
        } else {
            // Add to the original coverage
            for (String agentId : agentIdList) {
                if (agentId.split("-")[2].equals("null"))
                    continue;
                logger.info("collect conn " + agentId);
                AgentServerHandler conn = agentHandler.get(agentId);
                if (conn != null) {
                    agentStore.remove(agentId);
                    conn.collect();
                }
            }

            ExecutionDataStore execStore = new ExecutionDataStore();
            for (String agentId : agentIdList) {
                if (agentId.split("-")[2].equals("null"))
                    continue;
                logger.info("get coverage from " + agentId);
                ExecutionDataStore astore = agentStore.get(agentId);
                if (astore == null) {
                    logger.info("no data");
                } else {
                    // astore : classname -> int[]
                    execStore.merge(astore);
                    logger.trace("astore size: " + astore.getContents().size());
                }
            }
            logger.info("codecoverage size: " + execStore.getContents().size());
            // Send coverage back

            return execStore;
        }
    }

    abstract public int saveSnapshot();

    abstract public int moveSnapShot();

    abstract public void upgrade() throws Exception;

    public Pair<Boolean, String> checkResultConsistency(List<String> oriResult,
            List<String> upResult) {
        return new Pair<>(true, "");
    }

    public String getSubnet() {
        return dockerCluster.getNetworkIP();
    }
}
