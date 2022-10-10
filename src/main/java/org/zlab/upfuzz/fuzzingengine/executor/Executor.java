package org.zlab.upfuzz.fuzzingengine.executor;

import java.lang.reflect.InvocationTargetException;
import java.rmi.UnexpectedException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jacoco.core.data.ExecutionDataStore;
import org.zlab.upfuzz.*;
import org.zlab.upfuzz.docker.DockerCluster;
import org.zlab.upfuzz.docker.DockerMeta;
import org.zlab.upfuzz.fuzzingengine.AgentServerHandler;
import org.zlab.upfuzz.fuzzingengine.AgentServerSocket;
import org.zlab.upfuzz.fuzzingengine.Server.Seed;
import org.zlab.upfuzz.fuzzingengine.testplan.TestPlan;
import org.zlab.upfuzz.fuzzingengine.testplan.event.Event;
import org.zlab.upfuzz.fuzzingengine.testplan.event.command.ShellCommand;
import org.zlab.upfuzz.fuzzingengine.testplan.event.fault.*;
import org.zlab.upfuzz.fuzzingengine.testplan.event.upgradeop.PrepareUpgrade;
import org.zlab.upfuzz.fuzzingengine.testplan.event.upgradeop.UpgradeOp;
import org.zlab.upfuzz.utils.Pair;

public abstract class Executor implements IExecutor {
    protected final Logger logger = LogManager.getLogger(getClass());

    public int agentPort;
    public Long timestamp = 0L;

    public int eventIdx;

    public String executorID;
    public String systemID = "UnknowDS";
    public int nodeNum;

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
    public ConcurrentHashMap<String, Set<String>> sessionGroup;

    /* socket for client and agents to communicate*/
    public AgentServerSocket agentSocket;

    protected Executor() {
        executorID = RandomStringUtils.randomAlphanumeric(8);
    }

    protected Executor(String systemID) {
        this();
        this.systemID = systemID;
    }

    public void clearState() {
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
        CommandSequence originalCommandSequence;
        CommandSequence validationCommandSequence;
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

    public List<String> execute(List<String> commands) {
        return executeCommands(commands);
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
                    // If fault injection fails, keep executing
                    logger.error(
                            String.format("Cannot Inject {%s} here", event));

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
            } else if (event instanceof PrepareUpgrade) {
                if (!handlePrepareUpgrade((PrepareUpgrade) event)) {
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

    // We should also support collecting the code coverage of a single node

    public ExecutionDataStore collect(String version) {
        // TODO: Separate the coverage here
        Set<String> agentIdList = sessionGroup.get(executorID + "_" + version);
        logger.info("agentIdList: " + agentIdList);
        logger.info("executorID = " + executorID);
        if (agentIdList == null) {
            new UnexpectedException("No agent connection with executor " +
                    executorID)
                            .printStackTrace();
            return null;
        } else {
            // Clear the code coverage
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

    public ExecutionDataStore[] collectCoverageSeparate(String version) {
        // TODO: Separate the coverage here
        Set<String> agentIdList = sessionGroup.get(executorID + "_" + version);
        // logger.info("agentIdList: " + agentIdList);
        // logger.info("executorID = " + executorID);
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

            ExecutionDataStore[] executionDataStores = new ExecutionDataStore[nodeNum];
            for (int i = 0; i < executionDataStores.length; i++) {
                executionDataStores[i] = new ExecutionDataStore();
            }

            for (String agentId : agentIdList) {
                if (agentId.split("-")[3].equals("null"))
                    continue;
                logger.info("get coverage from " + agentId);
                ExecutionDataStore astore = agentStore.get(agentId);
                if (astore == null) {
                    logger.info("no data");
                } else {
                    executionDataStores[Integer.valueOf(agentId.split("-")[2])]
                            .merge(astore);
                    logger.trace("astore size: " + astore.getContents().size());
                }
            }
            return executionDataStores;
        }
    }

    abstract public int saveSnapshot();

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
            return false;
        }
        execShellCommand(command);
        return true;
    }

    public boolean handleUpgradeOp(UpgradeOp upgradeOp) {
        try {
            dockerCluster.upgrade(upgradeOp.nodeIndex);
        } catch (Exception e) {
            logger.error("upgrade failed due to an exception " + e);
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean handlePrepareUpgrade(PrepareUpgrade prepareUpgrade) {
        try {
            dockerCluster.prepareUpgrade();
        } catch (Exception e) {
            logger.error("upgrade prepare upgrade due to an exception " + e);
            e.printStackTrace();
            return false;
        }
        return true;
    }

}
