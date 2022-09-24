package org.zlab.upfuzz.fuzzingengine.executor;

import java.lang.reflect.InvocationTargetException;
import java.rmi.UnexpectedException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.util.JsonUtils;
import org.jacoco.core.data.ExecutionDataStore;
import org.zlab.upfuzz.*;
import org.zlab.upfuzz.docker.DockerCluster;
import org.zlab.upfuzz.docker.DockerMeta;
import org.zlab.upfuzz.fuzzingengine.AgentServerHandler;
import org.zlab.upfuzz.fuzzingengine.AgentServerSocket;
import org.zlab.upfuzz.fuzzingengine.Packet.TestPacket;
import org.zlab.upfuzz.fuzzingengine.Server.Seed;
import org.zlab.upfuzz.fuzzingengine.testplan.TestPlan;
import org.zlab.upfuzz.fuzzingengine.testplan.event.Event;
import org.zlab.upfuzz.fuzzingengine.testplan.event.command.ShellCommand;
import org.zlab.upfuzz.fuzzingengine.testplan.event.fault.*;
import org.zlab.upfuzz.fuzzingengine.testplan.event.upgradeop.UpgradeOp;
import org.zlab.upfuzz.utils.Pair;

public abstract class Executor implements IExecutor {
    protected final Logger logger = LogManager.getLogger(getClass());

    public int agentPort;
    public Long timestamp = 0L;

    public int eventIdx;

    public String executorID;
    public String systemID = "UnknowDS";

    public Map<Integer, Pair<List<String>, List<String>>> testId2commandSequence;
    public Map<Integer, List<String>> testId2oldVersionResult;
    public Map<Integer, List<String>> testId2newVersionResult;

    public DockerCluster dockerCluster;

    /**
     * key: String -> agentId value: Codecoverage for this agent
     */
    public Map<String, ExecutionDataStore> agentStore;

    /* key: String -> agent Id
     * value: ClientHandler -> the socket to a agent */
    public Map<String, AgentServerHandler> agentHandler;

    /* key: UUID String -> executor Id
     * value: List<String> -> list of all alive agents with the executor Id */
    public ConcurrentHashMap<String, List<String>> sessionGroup;

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
        executorID = RandomStringUtils.randomAlphanumeric(8);
    }

    public String getSysExecID() {
        return systemID + "-" + executorID;
    }

    abstract public void startup();

    abstract public void teardown();

    abstract public void upgradeTeardown();

    abstract public List<String> executeCommands(List<String> commandList);

    public static Seed generateSeed(CommandPool commandPool,
            Class<? extends State> stateClass) {
        CommandSequence originalCommandSequence = null;
        CommandSequence validationCommandSequence = null;
        try {
            ParameterType.BasicConcreteType.clearPool();
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

    // Dead code for now
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

    public void execute(TestPacket testPacket) {
        testId2commandSequence.put(
                testPacket.testPacketID,
                new Pair<>(testPacket.originalCommandSequenceList,
                        testPacket.validationCommandSequneceList));

        // testPacket should contain List<Event>
        // It can be (1) cqlsh command (2) a fault (3) admin command
        executeCommands(testPacket.originalCommandSequenceList);
    }

    public boolean execute(TestPlan testPlan) {
        // Base
        // - If there is any exception happen during this process, we should
        // report it.
        // - How about we monitor the output of each command
        // - We compare the results of commands between full-stop upgrade and
        // rolling upgrade
        eventIdx = 0;

        boolean status = true;
        for (eventIdx = 0; eventIdx < testPlan.getEvents().size(); eventIdx++) {
            Event event = testPlan.events.get(eventIdx);
            logger.info(String.format("\nhandle %s\n", event));
            if (event instanceof Fault) {
                if (!handleFault((Fault) event)) {
                    logger.error("fault problem");
                    status = false;
                    break;
                }
            } else if (event instanceof FaultRecover) {
                if (!handleFaultRecover((FaultRecover) event)) {
                    logger.error("FaultRecover execution problem");
                    status = false;
                    break;
                }
            } else if (event instanceof ShellCommand) {
                if (!handleCommand((ShellCommand) event)) {
                    logger.error("ShellCommand problem");
                    status = false;
                    break;
                }
            } else if (event instanceof UpgradeOp) {
                if (!handleUpgradeOp((UpgradeOp) event)) {
                    logger.error("UpgradeOp problem");
                    status = false;
                    break;
                }
            }
        }
        return status;
    }

    // Send to shell daemon to improve the performance
    abstract public String execShellCommand(ShellCommand command);

    // Execute with "docker exec" on the wordDir
    // Slower, but more general
    abstract public void execNormalCommand(Command command);

    public List<String> executeRead(int testId) {
        List<String> oldVersionResult = executeCommands(
                testId2commandSequence.get(testId).right);

        testId2oldVersionResult.put(testId, oldVersionResult);
        return oldVersionResult;
    }

    public ExecutionDataStore collect(String version) {
        List<String> agentIdList = sessionGroup.get(executorID + "_" + version);
        if (agentIdList == null) {
            new UnexpectedException("No agent connection with executor " +
                    executorID)
                            .printStackTrace();
            return null;
        } else {
            // Add to the original coverage
            for (String agentId : agentIdList) {
                if (agentId.split("-")[3].equals("null"))
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
                if (agentId.split("-")[3].equals("null"))
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
            logger.debug("codecoverage of " + executorID + "_" + version
                    + " size: " + execStore.getContents().size());
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

    public boolean handleFault(Fault fault) {
        if (fault instanceof LinkFailure) {
            // Link failure between two nodes
            LinkFailure linkFailure = (LinkFailure) fault;
            return dockerCluster.linkFailure(linkFailure.nodeIndex1,
                    linkFailure.nodeIndex2);
        } else if (fault instanceof NodeFailure) {
            // Crash a node
            NodeFailure nodeFailure = (NodeFailure) fault;
            dockerCluster.dockerStates[nodeFailure.nodeIndex].alive = false;
            return dockerCluster.killContainer(nodeFailure.nodeIndex);

        } else if (fault instanceof IsolateFailure) {
            // Isolate a single node from the rest nodes
            IsolateFailure isolateFailure = (IsolateFailure) fault;
            return dockerCluster.isolateNode(isolateFailure.nodeIndex);
        } else if (fault instanceof PartitionFailure) {
            // Partition two sets of nodes
            PartitionFailure partitionFailure = (PartitionFailure) fault;
            return dockerCluster.partition(partitionFailure.nodeSet1,
                    partitionFailure.nodeSet2);
        }
        return false;
    }

    public boolean handleFaultRecover(FaultRecover faultRecover) {
        if (faultRecover instanceof LinkFailureRecover) {
            // Link failure between two nodes
            LinkFailureRecover linkFailureRecover = (LinkFailureRecover) faultRecover;
            return dockerCluster.linkFailureRecover(
                    linkFailureRecover.nodeIndex1,
                    linkFailureRecover.nodeIndex2);
        } else if (faultRecover instanceof NodeFailureRecover) {
            // Crash a node
            NodeFailureRecover nodeFailureRecover = (NodeFailureRecover) faultRecover;
            dockerCluster.dockerStates[nodeFailureRecover.nodeIndex].alive = false;
            return dockerCluster
                    .killContainerRecover(nodeFailureRecover.nodeIndex);
        } else if (faultRecover instanceof IsolateFailureRecover) {
            // Isolate a single node from the rest nodes
            IsolateFailureRecover isolateFailureRecover = (IsolateFailureRecover) faultRecover;
            return dockerCluster
                    .isolateNodeRecover(isolateFailureRecover.nodeIndex);
        } else if (faultRecover instanceof PartitionFailureRecover) {
            // Partition two sets of nodes
            PartitionFailureRecover partitionFailureRecover = (PartitionFailureRecover) faultRecover;
            return dockerCluster.partitionRecover(
                    partitionFailureRecover.nodeSet1,
                    partitionFailureRecover.nodeSet2);
        }
        return false;
    }

    public boolean handleCommand(ShellCommand command) {
        // TODO: also handle normal commands

        // Some checks to make sure that at least one server
        // is up
        int liveContainers = 0;
        for (DockerMeta.DockerState dockerState : dockerCluster.dockerStates) {
            if (dockerState.alive)
                liveContainers++;
        }
        if (liveContainers == 0) {
            logger.error("All node is down, cannot execute shell commands!");
            // This shouldn't appear, but if it happens, we should report
            // TODO: report to server as a buggy case
            System.exit(1);
        }
        execShellCommand(command);
        return true;
    }

    public boolean handleUpgradeOp(UpgradeOp upgradeOp) {
        try {
            dockerCluster.upgrade(upgradeOp.nodeIndex);
        } catch (Exception e) {
            logger.error("upgrade failed due to an exception " + e);
            return false;
        }
        return true;
    }

}
