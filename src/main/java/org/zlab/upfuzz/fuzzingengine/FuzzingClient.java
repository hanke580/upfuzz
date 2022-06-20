package org.zlab.upfuzz.fuzzingengine;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.UnexpectedException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.ExecutionDataWriter;
import org.zlab.upfuzz.CommandSequence;
import org.zlab.upfuzz.CustomExceptions;
import org.zlab.upfuzz.cassandra.CassandraExecutor;
import org.zlab.upfuzz.fuzzingengine.Packet.FeedbackPacket;
import org.zlab.upfuzz.fuzzingengine.Packet.StackedFeedbackPacket;
import org.zlab.upfuzz.fuzzingengine.Packet.StackedTestPacket;
import org.zlab.upfuzz.fuzzingengine.Packet.TestPacket;
import org.zlab.upfuzz.fuzzingengine.executor.Executor;
import org.zlab.upfuzz.hdfs.HdfsExecutor;
import org.zlab.upfuzz.utils.Pair;
import org.zlab.upfuzz.utils.Utilities;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FuzzingClient {
    static Logger logger = LogManager.getLogger(FuzzingClient.class);

    public static final int epochNum = 60; // Validation per epochNum

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
    public AgentServerSocket clientSocket;

    public static int epoch;
    public static int crashID;
    public static int epochStartTestId;

    public Executor executor;

    private Thread t; // new version stop + old version restart

    public static Map<Integer, Pair<List<String>, List<String>>> testId2Sequence;

    FuzzingClient() {
        init();
        if (Config.getConf().system.equals("cassandra")) {
            executor = new CassandraExecutor();
        } else if (Config.getConf().system.equals("hdfs")) {
            executor = new HdfsExecutor();
        }
        t = new Thread(() -> executor.startup()); // Startup before tests
        t.start();
    }

    private void init() {
        // TODO: GC the old coverage since we already get the overall coverage.
        agentStore = new HashMap<>();
        agentHandler = new HashMap<>();
        sessionGroup = new HashMap<>();

        epoch = 0;
        crashID = 0;
        epochStartTestId = 0; // FIXME: It might not be zero

        testId2Sequence = new HashMap<>();

        try {
            clientSocket = new AgentServerSocket(this);
            clientSocket.setDaemon(true);
            clientSocket.start();
        } catch (Exception e) {
            e.printStackTrace();
            // System.exit(1);
        }

        // FIX orphan process
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                executor.teardown();
                executor.upgradeTeardown();
            }
        });
    }

    public void start() throws InterruptedException {
        Thread clientThread = new Thread(new FuzzingClientSocket(this));
        clientThread.start();
        clientThread.join();
    }

    /**
     * start the old version system, execute and count the coverage of all
     * test cases of stackedFeedbackPacket, perform an upgrade process, check
     * the (1) upgrade process failed (2) result inconsistency
     * @param stackedTestPacket the stacked test packets from server
     */
    public StackedFeedbackPacket executeStackedTestPacket(
            StackedTestPacket stackedTestPacket) {
        // Run all the tests, collect (1) coverage and (2) old version read
        // results

        // Check whether system start up is successful
        assert t != null;

        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
            // clean system state, restart the executor
            // (1) clean system state
            // (2) restart
            executor.teardown();
            executor.clearState();
            executor.startup();
        }

        Map<Integer, FeedbackPacket> testID2FeedbackPacket = new HashMap<>();
        Map<Integer, List<String>> testID2oriResults = new HashMap<>();
        Map<Integer, List<String>> testID2upResults;

        FeedBack fb;
        for (TestPacket tp : stackedTestPacket.getTestPacketList()) {
            executor.execute(tp);
            fb = new FeedBack();
            fb.originalCodeCoverage = collect(executor, "original");
            if (fb.originalCodeCoverage == null) {
                logger.info("ERROR: null origin code coverage");
                System.exit(1);
            }
            testID2FeedbackPacket.put(tp.testPacketID,
                    new FeedbackPacket(tp.testPacketID, tp.systemID, fb));

            List<String> oriResult = executor.executeRead(tp.testPacketID);
            testID2oriResults.put(tp.testPacketID, oriResult);
        }

        // Perform upgrade (1) check whether upgrade succeeds (2) new version
        // read
        // results, compare
        executor.saveSnapshot();
        executor.moveSnapShot();
        executor.teardown();

        StackedFeedbackPacket stackedFeedbackPacket = new StackedFeedbackPacket();
        stackedFeedbackPacket.stackedCommandSequenceStr = recordAllStackedTests(
                stackedTestPacket);

        boolean ret = executor.upgradeTest();

        t = new Thread(() -> {
            executor.upgradeTeardown();
            executor.clearState();
            executor.startup();
        });
        t.start();

        if (!ret) {
            // upgrade process failed
            stackedFeedbackPacket.isUpgradeProcessFailed = true;
        } else {
            // upgrade process succeeds, compare results here
            testID2upResults = executor.testId2newVersionResult;
            for (TestPacket tp : stackedTestPacket.getTestPacketList()) {
                Pair<Boolean, String> compareRes = executor
                        .checkResultConsistency(
                                testID2oriResults.get(tp.testPacketID),
                                testID2upResults.get(tp.testPacketID));

                if (!compareRes.left) {
                    // Log the failure info into feedback packet
                    StringBuilder failureReport = new StringBuilder();
                    failureReport.append(
                            "Results are inconsistent between two versions\n");
                    failureReport.append(compareRes.right);

                    failureReport.append("Original Command Sequence\n");
                    for (String commandStr : tp.originalCommandSequenceList) {
                        failureReport.append(commandStr + "\n");
                    }
                    failureReport.append("\n\n");
                    failureReport.append("Read Command Sequence\n");
                    for (String commandStr : tp.validationCommandSequneceList) {
                        failureReport.append(commandStr + "\n");
                    }
                    FeedbackPacket feedbackPacket = testID2FeedbackPacket
                            .get(tp.testPacketID);
                    feedbackPacket.isInconsistent = true;
                    feedbackPacket.inconsistencyReport = failureReport
                            .toString();
                    stackedFeedbackPacket.addFeedbackPacket(feedbackPacket);
                }
            }
        }
        return stackedFeedbackPacket;
    }

    private String recordAllStackedTests(StackedTestPacket stackedTestPacket) {
        StringBuilder sb = new StringBuilder();
        for (TestPacket tp : stackedTestPacket.getTestPacketList()) {
            for (String cmdStr : tp.originalCommandSequenceList) {
                sb.append(cmdStr + "\n");
            }
            sb.append("\n");
            for (String cmdStr : tp.validationCommandSequneceList) {
                sb.append(cmdStr + "\n");
            }
            sb.append("\n\n");
        }
        return sb.toString();
    }

    public ExecutionDataStore collect(Executor executor, String version) {
        List<String> agentIdList = sessionGroup
                .get(executor.executorID + "_" + version);
        if (agentIdList == null) {
            new UnexpectedException(
                    "No agent connection with executor " + executor.executorID)
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

    enum FuzzingClientActions {
        start, collect;
    }
}
