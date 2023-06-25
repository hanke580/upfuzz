package org.zlab.upfuzz.fuzzingengine.executor;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jacoco.core.data.ExecutionDataStore;
import org.zlab.upfuzz.*;
import org.zlab.upfuzz.docker.DockerCluster;
import org.zlab.upfuzz.docker.DockerMeta;
import org.zlab.upfuzz.fuzzingengine.AgentServerHandler;
import org.zlab.upfuzz.fuzzingengine.AgentServerSocket;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.fuzzingengine.LogInfo;
import org.zlab.upfuzz.fuzzingengine.server.Seed;
import org.zlab.upfuzz.fuzzingengine.testplan.TestPlan;
import org.zlab.upfuzz.fuzzingengine.testplan.event.Event;
import org.zlab.upfuzz.fuzzingengine.testplan.event.command.ShellCommand;
import org.zlab.upfuzz.fuzzingengine.testplan.event.downgradeop.DowngradeOp;
import org.zlab.upfuzz.fuzzingengine.testplan.event.fault.*;
import org.zlab.upfuzz.fuzzingengine.testplan.event.upgradeop.FinalizeUpgrade;
import org.zlab.upfuzz.fuzzingengine.testplan.event.upgradeop.HDFSStopSNN;
import org.zlab.upfuzz.fuzzingengine.testplan.event.upgradeop.PrepareUpgrade;
import org.zlab.upfuzz.fuzzingengine.testplan.event.upgradeop.UpgradeOp;
import org.zlab.upfuzz.hdfs.HdfsDockerCluster;
import org.zlab.upfuzz.utils.Pair;

public abstract class Executor implements IExecutor {
    protected static final Logger logger = LogManager.getLogger(Executor.class);

    public int agentPort;
    public Long timestamp = 0L;
    public int eventIdx;
    public String executorID;
    public String systemID = "UnknowDS";
    public int nodeNum;
    public Set<String> targetSystemStates;
    public Path configPath;

    // Use for test plan coverage collection
    public ExecutionDataStore[] oriCoverage;

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

    protected Executor(String systemID, int nodeNum) {
        this();
        this.systemID = systemID;
        this.nodeNum = nodeNum;
        this.oriCoverage = new ExecutionDataStore[nodeNum];
    }

    public void clearState() {
        executorID = RandomStringUtils.randomAlphanumeric(8);
    }

    public String getSysExecID() {
        return systemID + "-" + executorID;
    }

    public boolean freshStartNewVersion() {
        try {
            return dockerCluster.freshStartNewVersion();
        } catch (Exception e) {
            logger.error(String.format(
                    "new version cannot start up with exception ", e));
            return false;
        }
    }

    public boolean hasBrokenInv() {
        try {
            return dockerCluster.hasbrokenInv();
        } catch (Exception e) {
            logger.info(
                    "Problem occurs when retrieving invariant violations"
                            + e
                            + "assume no inv is broken");
            return false;
        }
    }

    public Map<Integer, Integer> getBrokenInv() {
        try {
            return dockerCluster.getBrokenInv();
        } catch (Exception e) {
            logger.info(
                    "Problem occurs when retrieving invariant violations"
                            + e
                            + "assume no inv is broken");
            return null;
        }
    }

    // likely invariant support
    public boolean fullStopCluster() {
        try {
            return dockerCluster.fullStopCluster();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean upgradeCluster() {
        try {
            return dockerCluster.upgradeCluster();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean fullStopUpgrade() {
        try {
            return dockerCluster.fullStopUpgrade();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean rollingUpgrade() {
        try {
            return dockerCluster.rollingUpgrade();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean downgrade() {
        try {
            return dockerCluster.downgrade();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public abstract void upgradeTeardown();

    public List<String> executeCommands(List<String> commandList) {
        // TODO: Use Event here, since not all commands are executed
        List<String> ret = new LinkedList<>();
        for (String command : commandList) {
            if (command.isEmpty()) {
                ret.add("");
            } else {
                ret.add(execShellCommand(new ShellCommand(command)));
            }
        }
        return ret;
    }

    public static Seed generateSeed(CommandPool commandPool,
            Class<? extends State> stateClass, int configIdx) {
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
            if (Config.getConf().system.equals("hdfs")) {
                validationCommandSequence.commands.remove(0);
            }
            return new Seed(originalCommandSequence, validationCommandSequence,
                    configIdx);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean execute(TestPlan testPlan) {
        // Any exception happen during this process, we will report it.
        eventIdx = 0;

        boolean status = true;
        for (eventIdx = 0; eventIdx < testPlan.getEvents().size(); eventIdx++) {
            Event event = testPlan.getEvents().get(eventIdx);
            logger.info(String.format("\nhandle %s\n", event));
            if (event instanceof Fault) {
                if (!handleFault((Fault) event)) {
                    // If fault injection fails, keep executing
                    logger.error(
                            String.format("Cannot Inject {%s} here", event));
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
                UpgradeOp upgradeOp = (UpgradeOp) event;
                int nodeIdx = upgradeOp.nodeIndex;
                oriCoverage[nodeIdx] = collectSingleNodeCoverage(nodeIdx,
                        "original");

                if (!handleUpgradeOp((UpgradeOp) event)) {
                    logger.error("UpgradeOp problem");
                    status = false;
                    break;
                }
            } else if (event instanceof DowngradeOp) {
                if (!handleDowngradeOp((DowngradeOp) event)) {
                    logger.error("DowngradeOp problem");
                    status = false;
                    break;
                }
            } else if (event instanceof PrepareUpgrade) {
                if (!handlePrepareUpgrade((PrepareUpgrade) event)) {
                    logger.error("UpgradeOp problem");
                    status = false;
                    break;
                }
            } else if (event instanceof HDFSStopSNN) {
                if (!handleHDFSStopSNN((HDFSStopSNN) event)) {
                    logger.error("HDFS stop SNN problem");
                    status = false;
                    break;
                }
            } else if (event instanceof FinalizeUpgrade) {
                if (!handleFinalizeUpgrade((FinalizeUpgrade) event)) {
                    logger.error("FinalizeUpgrade problem");
                    status = false;
                    break;
                }
            }
        }
        return status;
    }

    abstract public String execShellCommand(ShellCommand command);

    public ExecutionDataStore collect(String version) {
        // TODO: Separate the coverage here
        Set<String> agentIdList = sessionGroup.get(executorID + "_" + version);
        logger.info("agentIdList: " + agentIdList);
        logger.info("executorID = " + executorID);
        if (agentIdList == null) {
            logger.error("No agent connection with executor " +
                    executorID);
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

    public ExecutionDataStore collectSingleNodeCoverage(int nodeIdx,
            String version) {
        Set<String> agentIdList = sessionGroup.get(executorID + "_" + version);
        ExecutionDataStore executionDataStore = null;
        if (agentIdList == null) {
            logger.error("No agent connection with executor " +
                    executorID);
        } else {
            for (String agentId : agentIdList) {
                if (agentId.split("-")[3].equals("null"))
                    continue;

                int idx = Integer.parseInt(agentId.split("-")[2]);
                if (nodeIdx == idx) {
                    logger.info("collect conn " + agentId);
                    AgentServerHandler conn = agentHandler.get(agentId);
                    if (conn != null) {
                        agentStore.remove(agentId);
                        conn.collect();
                    }
                    executionDataStore = agentStore.get(agentId);
                    break;
                }
            }
        }
        return executionDataStore;

    }

    public ExecutionDataStore[] collectCoverageSeparate(String version) {
        // TODO: Separate the coverage here
        Set<String> agentIdList = sessionGroup.get(executorID + "_" + version);
        // logger.info("agentIdList: " + agentIdList);
        // logger.info("executorID = " + executorID);
        if (agentIdList == null) {
            logger.error("No agent connection with executor " +
                    executorID);
            return null;
        } else {
            // Add to the original coverage
            for (String agentId : agentIdList) {
                if (agentId.split("-")[3].equals("null"))
                    continue;
                // logger.info("collect conn " + agentId);
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
                // logger.info("get coverage from " + agentId);
                ExecutionDataStore astore = agentStore.get(agentId);
                if (astore == null) {
                    // logger.info("no data");
                } else {
                    executionDataStores[Integer.parseInt(agentId.split("-")[2])]
                            .merge(astore);
                    // logger.trace("astore size: " +
                    // astore.getContents().size());
                }
            }
            return executionDataStores;
        }
    }

    public Pair<Boolean, String> checkResultConsistency(List<String> oriResult,
            List<String> upResult, boolean compareOldAndNew) {
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
        } else if (fault instanceof RestartFailure) {
            // Crash a node
            RestartFailure nodeFailure = (RestartFailure) fault;
            return dockerCluster.restartContainer(nodeFailure.nodeIndex);

        }
        return false;
    }

    public boolean handleFaultRecover(FaultRecover faultRecover) {
        if (faultRecover instanceof LinkFailureRecover) {
            // Link failure between two nodes
            LinkFailureRecover linkFailureRecover = (LinkFailureRecover) faultRecover;
            boolean ret = dockerCluster.linkFailureRecover(
                    linkFailureRecover.nodeIndex1,
                    linkFailureRecover.nodeIndex2);
            FaultRecover.waitToRebuildConnection();
            return ret;
        } else if (faultRecover instanceof NodeFailureRecover) {
            // recover from node crash
            NodeFailureRecover nodeFailureRecover = (NodeFailureRecover) faultRecover;
            return dockerCluster
                    .killContainerRecover(nodeFailureRecover.nodeIndex);
        } else if (faultRecover instanceof IsolateFailureRecover) {
            // Isolate a single node from the rest nodes
            IsolateFailureRecover isolateFailureRecover = (IsolateFailureRecover) faultRecover;
            boolean ret = dockerCluster
                    .isolateNodeRecover(isolateFailureRecover.nodeIndex);
            FaultRecover.waitToRebuildConnection();
            return ret;
        } else if (faultRecover instanceof PartitionFailureRecover) {
            // Partition two sets of nodes
            PartitionFailureRecover partitionFailureRecover = (PartitionFailureRecover) faultRecover;
            boolean ret = dockerCluster.partitionRecover(
                    partitionFailureRecover.nodeSet1,
                    partitionFailureRecover.nodeSet2);
            FaultRecover.waitToRebuildConnection();
            return ret;
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
        try {
            execShellCommand(command);
        } catch (Exception e) {
            logger.error("shell command execution failed " + e);
            e.printStackTrace();
            return false;
        }
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

    public boolean handleDowngradeOp(DowngradeOp downgradeOp) {
        try {
            dockerCluster.downgrade(downgradeOp.nodeIndex);
        } catch (Exception e) {
            logger.error("downgrade failed due to an exception " + e);
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

    public boolean handleHDFSStopSNN(HDFSStopSNN hdfsStopSNN) {
        try {
            assert dockerCluster instanceof HdfsDockerCluster;
            ((HdfsDockerCluster) dockerCluster).stopSNN();
        } catch (Exception e) {
            logger.error("hdfs cannot stop SNN due to an exception " + e);
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean handleFinalizeUpgrade(FinalizeUpgrade finalizeUpgrade) {
        try {
            dockerCluster.finalizeUpgrade();
        } catch (Exception e) {
            logger.error("hdfs cannot stop SNN due to an exception " + e);
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public Map<Integer, Map<String, String>> readSystemState() {
        return dockerCluster.readSystemState();
    }

    public Map<Integer, LogInfo> grepLogInfo() {
        return dockerCluster.grepLogInfo();
    }

}
