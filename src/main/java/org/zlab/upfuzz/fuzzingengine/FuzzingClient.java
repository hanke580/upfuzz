package org.zlab.upfuzz.fuzzingengine;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jacoco.core.data.ExecutionDataStore;
import org.zlab.upfuzz.cassandra.CassandraExecutor;
import org.zlab.upfuzz.fuzzingengine.packet.*;
import org.zlab.upfuzz.fuzzingengine.executor.Executor;
import org.zlab.upfuzz.hdfs.HdfsExecutor;
import org.zlab.upfuzz.utils.Pair;
import org.zlab.upfuzz.utils.Utilities;

import static org.zlab.upfuzz.fuzzingengine.server.FuzzingServer.readState;

public class FuzzingClient {
    static Logger logger = LogManager.getLogger(FuzzingClient.class);

    public Executor executor;
    public Path configDirPath;

    // If the cluster cannot start up for 3 times, it's serious
    int CLUSTER_START_RETRY = 1; // stop retry for now

    FuzzingClient() {
        // FIX orphan process
        configDirPath = Paths.get(System.getProperty("user.dir"),
                Config.getConf().configDir, Config.getConf().originalVersion
                        + "_" + Config.getConf().upgradedVersion);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            executor.teardown();
            executor.upgradeTeardown();
        }));
    }

    public void start() throws InterruptedException {
        Thread clientThread = new Thread(new FuzzingClientSocket(this));
        clientThread.start();
        clientThread.join();
    }

    public void initExecutor(int nodeNum, Set<String> targetSystemStates,
            Path configPath) {
        if (Config.getConf().system.equals("cassandra")) {
            executor = new CassandraExecutor(nodeNum, targetSystemStates,
                    configPath);
        } else if (Config.getConf().system.equals("hdfs")) {
            executor = new HdfsExecutor(nodeNum, targetSystemStates,
                    configPath);
        }
    }

    public boolean startUpExecutor() {
        for (int i = 0; i < CLUSTER_START_RETRY; i++) {
            try {
                if (executor.startup())
                    return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
            executor.teardown();
        }
        logger.error("original version cluster cannot start up");
        return false;
    }

    public void tearDownExecutor() {
        executor.upgradeTeardown();
        executor.clearState();
        executor.teardown();
    }

    /**
     * start the old version system, execute and count the coverage of all
     * test cases of stackedFeedbackPacket, perform an upgrade process, check
     * the (1) upgrade process failed (2) result inconsistency
     * @param stackedTestPacket the stacked test packets from server
     */
    public StackedFeedbackPacket executeStackedTestPacket(
            StackedTestPacket stackedTestPacket) {
        String stackedTestPacketStr = null;

        Path configPath = Paths.get(configDirPath.toString(),
                stackedTestPacket.configIdx);
        logger.info("[HKLOG] configPath = " + configPath);

        initExecutor(stackedTestPacket.nodeNum, null, configPath);

        boolean startUpStatus = startUpExecutor();
        if (!startUpStatus) {
            return null;
        }

        if (Config.getConf().startUpClusterForDebugging) {
            logger.info(
                    "Start up a cluster and leave it for debugging: This is not testing mode! Please set this startUpClusterForDebugging to false for real testing mode");
            try {
                Thread.sleep(1800 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.exit(1);

        }

        Map<Integer, FeedbackPacket> testID2FeedbackPacket = new HashMap<>();
        Map<Integer, List<String>> testID2oriResults = new HashMap<>();
        Map<Integer, List<String>> testID2upResults = new HashMap<>();

        for (TestPacket tp : stackedTestPacket.getTestPacketList()) {
            // logger.trace("Execute testpacket " + tp.systemID + " " +
            // tp.testPacketID);
            // logger.debug("\nWRITE CMD SEQUENCE");
            // for (String cmd : tp.originalCommandSequenceList) {
            // logger.debug(cmd);
            // }
            // logger.debug("\nREAD CMD SEQUENCE");
            // for (String cmd : tp.validationCommandSequneceList) {
            // logger.debug(cmd);
            // }
            executor.executeCommands(tp.originalCommandSequenceList);

            FeedBack[] feedBacks = new FeedBack[stackedTestPacket.nodeNum];
            for (int i = 0; i < stackedTestPacket.nodeNum; i++) {
                feedBacks[i] = new FeedBack();
            }
            ExecutionDataStore[] oriCoverages = executor
                    .collectCoverageSeparate("original");
            if (oriCoverages != null) {
                for (int nodeIdx = 0; nodeIdx < stackedTestPacket.nodeNum; nodeIdx++) {
                    feedBacks[nodeIdx].originalCodeCoverage = oriCoverages[nodeIdx];
                }
            }
            testID2FeedbackPacket.put(
                    tp.testPacketID,
                    new FeedbackPacket(tp.systemID, stackedTestPacket.nodeNum,
                            tp.testPacketID, feedBacks));

            List<String> oriResult = executor
                    .executeCommands(tp.validationCommandSequneceList);
            testID2oriResults.put(tp.testPacketID, oriResult);
        }

        StackedFeedbackPacket stackedFeedbackPacket = new StackedFeedbackPacket();
        stackedFeedbackPacket.stackedCommandSequenceStr = recordStackedTestPacket(
                stackedTestPacket);

        boolean ret = executor.fullStopUpgrade();

        // collect system state here
        if (!ret) {
            // upgrade process failed
            logger.info("upgrade failed");
            stackedFeedbackPacket.isUpgradeProcessFailed = true;
            StringBuilder sb = new StringBuilder();
            sb.append("[upgrade failed]\n");
            sb.append("ConfigIdx = " + stackedTestPacket.configIdx + "\n\n");
            sb.append("executionId = " + executor.executorID + "\n");

            sb.append("[Full Command Sequence]\n");
            stackedTestPacketStr = recordStackedTestPacket(
                    stackedTestPacket);
            sb.append(stackedTestPacketStr);
            stackedFeedbackPacket.upgradeFailureReport = sb.toString();
        } else {
            logger.info("upgrade succeed");
            stackedFeedbackPacket.isUpgradeProcessFailed = false;

            for (TestPacket tp : stackedTestPacket.getTestPacketList()) {
                List<String> upResult = executor
                        .executeCommands(tp.validationCommandSequneceList);
                testID2upResults.put(tp.testPacketID, upResult);
                if (Config.getConf().collUpFeedBack) {
                    ExecutionDataStore[] upCoverages = executor
                            .collectCoverageSeparate("upgraded");
                    if (upCoverages != null) {
                        for (int nodeIdx = 0; nodeIdx < stackedTestPacket.nodeNum; nodeIdx++) {
                            testID2FeedbackPacket.get(
                                    tp.testPacketID).feedBacks[nodeIdx].upgradedCodeCoverage = upCoverages[nodeIdx];
                        }
                    }
                }
            }

            // Check read results consistency
            for (TestPacket tp : stackedTestPacket.getTestPacketList()) {
                Pair<Boolean, String> compareRes = executor
                        .checkResultConsistency(
                                testID2oriResults.get(tp.testPacketID),
                                testID2upResults.get(tp.testPacketID));

                FeedbackPacket feedbackPacket = testID2FeedbackPacket
                        .get(tp.testPacketID);

                if (!compareRes.left) {
                    StringBuilder failureReport = new StringBuilder();
                    failureReport.append(
                            "Results are inconsistent between two versions\n");

                    failureReport.append("ConfigIdx = "
                            + stackedTestPacket.configIdx + "\n\n");
                    failureReport.append(
                            "executionId = " + executor.executorID + "\n");

                    failureReport.append(compareRes.right);
                    failureReport.append(recordSingleTestPacket(tp));
                    failureReport.append("\n[Full Command Sequence]\n");
                    if (stackedTestPacketStr == null)
                        stackedTestPacketStr = recordStackedTestPacket(
                                stackedTestPacket);
                    failureReport.append(stackedTestPacketStr);
                    // Create the feedback packet
                    feedbackPacket.isInconsistent = true;
                    feedbackPacket.inconsistencyReport = failureReport
                            .toString();
                } else {
                    feedbackPacket.isInconsistent = false;
                }
                stackedFeedbackPacket.addFeedbackPacket(feedbackPacket);
            }
        }
        logger.info(executor.systemID + " executor: " + executor.executorID
                + " finished execution");
        tearDownExecutor();
        return stackedFeedbackPacket;
    }

    public FullStopFeedbackPacket executeFullStopPacket(
            FullStopPacket fullStopPacket) {
        int nodeNum = fullStopPacket.getNodeNum();

        logger.debug("full stop: \n");
        logger.debug(fullStopPacket.fullStopUpgrade);

        FeedBack[] feedBacks = new FeedBack[nodeNum];
        for (int i = 0; i < nodeNum; i++) {
            feedBacks[i] = new FeedBack();
        }
        FullStopFeedbackPacket fullStopFeedbackPacket = new FullStopFeedbackPacket(
                fullStopPacket.systemID, nodeNum,
                fullStopPacket.testPacketID, feedBacks, null);

        logger.info("[HKLOG] configPath = " + fullStopPacket.configFileName);
        Path configPath = Paths.get(configDirPath.toString(),
                fullStopPacket.configFileName);
        // Start up
        initExecutor(nodeNum,
                fullStopPacket.fullStopUpgrade.targetSystemStates, configPath);
        boolean startUpStatus = startUpExecutor();
        if (!startUpStatus) {
            return null;
        }

        // Execute
        executor.executeCommands(fullStopPacket.fullStopUpgrade.commands);
        List<String> oriResult = executor.executeCommands(
                fullStopPacket.fullStopUpgrade.validCommands);

        // Coverage collection
        try {
            ExecutionDataStore[] oriCoverages = executor
                    .collectCoverageSeparate("original");
            if (oriCoverages != null) {
                for (int nodeIdx = 0; nodeIdx < nodeNum; nodeIdx++) {
                    fullStopFeedbackPacket.feedBacks[nodeIdx].originalCodeCoverage = oriCoverages[nodeIdx];
                }
            }
        } catch (Exception e) {
            fullStopFeedbackPacket.isEventFailed = true;
            StringBuilder eventFailedReport = new StringBuilder();
            logger.error("cannot collect coverage " + e);
            eventFailedReport.append(
                    "TestPlan execution failed, cannot collect original coverage\n")
                    .append(e);
            eventFailedReport.append(recordFullStopPacket(fullStopPacket));
            fullStopFeedbackPacket.eventFailedReport = eventFailedReport
                    .toString();
            tearDownExecutor();
            return fullStopFeedbackPacket;
        }

        boolean upgradeStatus = executor.fullStopUpgrade();

        if (!upgradeStatus) {
            // Cannot upgrade
            fullStopFeedbackPacket.isEventFailed = true;
            fullStopFeedbackPacket.eventFailedReport = "[upgrade failed]\n" +
                    recordFullStopPacket(fullStopPacket);
        } else {
            // Upgrade is done successfully, collect coverage and check results
            fullStopFeedbackPacket.isEventFailed = false;

            // Collect new version coverage
            try {
                ExecutionDataStore[] upCoverages = executor
                        .collectCoverageSeparate("upgraded");
                if (upCoverages != null) {
                    for (int nodeIdx = 0; nodeIdx < nodeNum; nodeIdx++) {
                        fullStopFeedbackPacket.feedBacks[nodeIdx].upgradedCodeCoverage = upCoverages[nodeIdx];
                    }
                }
            } catch (Exception e) {
                // Cannot collect code coverage in the upgraded version
                // Report it as the situations that "No nodes are upgraded"
                fullStopFeedbackPacket.isEventFailed = true;
                StringBuilder eventFailedReport = new StringBuilder();
                eventFailedReport.append(
                        "TestPlan execution failed, cannot collect upgrade coverage\n")
                        .append(e);
                eventFailedReport.append(recordFullStopPacket(fullStopPacket));
                fullStopFeedbackPacket.eventFailedReport = eventFailedReport
                        .toString();
                tearDownExecutor();
                return fullStopFeedbackPacket;
            }

            Map<Integer, Map<String, String>> states = executor
                    .readSystemState();
            fullStopFeedbackPacket.systemStates = states;
            logger.info("collected system states = " + states);

            List<String> upResult = executor.executeCommands(
                    fullStopPacket.fullStopUpgrade.validCommands);

            logger.info("upResult = " + upResult);

            if (Config.getConf().startUpClusterForDebugging) {
                logger.info(
                        "Start up a cluster and leave it for debugging: This is not testing mode! Please set this startUpClusterForDebugging to false for real testing mode");
                try {
                    Thread.sleep(1800 * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.exit(1);

            }

            // Compare results
            Pair<Boolean, String> compareRes = executor
                    .checkResultConsistency(oriResult, upResult);
            if (!compareRes.left) {
                fullStopFeedbackPacket.isInconsistent = true;
                fullStopFeedbackPacket.inconsistencyReport = "Results are inconsistent between two versions\n"
                        +
                        compareRes.right +
                        recordFullStopPacket(fullStopPacket);
            } else {
                fullStopFeedbackPacket.isInconsistent = false;
            }
        }
        logger.info(executor.systemID + " executor: " + executor.executorID
                + " finished execution");
        tearDownExecutor();
        return fullStopFeedbackPacket;
    }

    public TestPlanFeedbackPacket executeTestPlanPacket(
            TestPlanPacket testPlanPacket) {

        logger.debug("test plan: \n");
        logger.debug(testPlanPacket.testPlan);

        String testPlanPacketStr;
        int nodeNum = testPlanPacket.getNodeNum();

        // read states
        Path targetSystemStatesPath = Paths.get(System.getProperty("user.dir"),
                Config.getConf().targetSystemStateFile);
        Set<String> targetSystemStates = null;
        try {
            targetSystemStates = readState(targetSystemStatesPath);
        } catch (IOException e) {
            logger.error("Not tracking system state");
            e.printStackTrace();
            System.exit(1);
        }

        Path configPath = Paths.get(configDirPath.toString(), "test21");
        logger.info("[HKLOG] configPath = " + configPath);

        initExecutor(testPlanPacket.getNodeNum(), targetSystemStates,
                configPath);
        boolean startUpStatus = startUpExecutor();
        if (!startUpStatus) {
            return null;
        }

        // TestPlan only contains one test sequence
        // We need to compare the results between two versions for once
        // Then we return the feedback packet
        boolean status = executor.execute(testPlanPacket.getTestPlan());

        if (Config.getConf().startUpClusterForDebugging) {
            logger.info(
                    "Start up a cluster and leave it for debugging: This is not testing mode! Please set this startUpClusterForDebugging to false for real testing mode");
            try {
                Thread.sleep(1800 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.exit(1);

        }

        FeedBack[] feedBacks = new FeedBack[nodeNum];
        for (int i = 0; i < nodeNum; i++) {
            feedBacks[i] = new FeedBack();
        }
        TestPlanFeedbackPacket testPlanFeedbackPacket = new TestPlanFeedbackPacket(
                testPlanPacket.systemID, nodeNum,
                testPlanPacket.testPacketID, feedBacks);

        // collect old version coverage
        for (int nodeIdx = 0; nodeIdx < nodeNum; nodeIdx++) {
            if (executor.oriCoverage[nodeIdx] != null)
                testPlanFeedbackPacket.feedBacks[nodeIdx].originalCodeCoverage = executor.oriCoverage[nodeIdx];
        }

        // System state comparison
        if (Config.getConf().enableStateComp) {
            Map<Integer, Map<String, String>> states = executor
                    .readSystemState();
            logger.info("rolling upgrade system states = " + states);
            logger.info("full stop upgrade system state"
                    + testPlanPacket.testPlan.targetSystemStatesOracle);
            Map<Integer, Map<String, Pair<String, String>>> inconsistentStates = stateCompare(
                    testPlanPacket.testPlan.targetSystemStatesOracle,
                    states);
            logger.info("inconsistent states = " + inconsistentStates);
        }

        if (!status) {
            // Now we only support checking the state of events
            // The test plan has some problems
            // - (1) Some event cannot execute correctly
            // - (2) There is an inconsistency between rolling upgrade and
            // full-stop upgrade
            // - (3) Node crashed unexpectedly.
            // We should report it.
            int buggyEventIdx = executor.eventIdx;
            testPlanFeedbackPacket.isEventFailed = true;

            StringBuilder eventFailedReport = new StringBuilder();
            eventFailedReport.append(String.format(
                    "Test plan execution failed at event[%d]\n\n",
                    buggyEventIdx));
            testPlanPacketStr = recordTestPlanPacket(testPlanPacket);
            eventFailedReport.append(testPlanPacketStr);
            testPlanFeedbackPacket.eventFailedReport = eventFailedReport
                    .toString();

            testPlanFeedbackPacket.isInconsistent = false;
            testPlanFeedbackPacket.inconsistencyReport = "";

            logger.error(String.format(
                    "The test plan execution met a problem when executing event[%d]",
                    buggyEventIdx));
        } else {

            try {
                ExecutionDataStore[] upCoverages = executor
                        .collectCoverageSeparate("upgraded");
                if (upCoverages != null) {
                    for (int nodeIdx = 0; nodeIdx < nodeNum; nodeIdx++) {
                        testPlanFeedbackPacket.feedBacks[nodeIdx].upgradedCodeCoverage = upCoverages[nodeIdx];
                    }
                }

            } catch (Exception e) {
                // Cannot collect code coverage in the upgraded version
                testPlanFeedbackPacket.isEventFailed = true;
                StringBuilder eventFailedReport = new StringBuilder();
                eventFailedReport.append(
                        "TestPlan execution failed, cannot collect upgrade coverage\n")
                        .append(e);
                eventFailedReport.append(recordTestPlanPacket(testPlanPacket));
                testPlanFeedbackPacket.eventFailedReport = eventFailedReport
                        .toString();
                tearDownExecutor();
                return testPlanFeedbackPacket;
            }
        }

        tearDownExecutor();
        return testPlanFeedbackPacket;
    }

    public MixedFeedbackPacket executeMixedTestPacket(
            MixedTestPacket mixedTestPacket) {

        String mixedTestPacketStr = null;

        StackedTestPacket stackedTestPacket = mixedTestPacket.stackedTestPacket;
        TestPlanPacket testPlanPacket = mixedTestPacket.testPlanPacket;

        assert stackedTestPacket.nodeNum == testPlanPacket.getNodeNum();
        int nodeNum = stackedTestPacket.nodeNum;

        Path configPath = Paths.get(configDirPath.toString(),
                stackedTestPacket.configIdx);
        logger.info("[HKLOG] configPath = " + configPath);

        initExecutor(nodeNum, null, configPath);

        boolean startUpStatus = startUpExecutor();
        if (!startUpStatus) {
            return null;
        }

        Map<Integer, FeedbackPacket> testID2FeedbackPacket = new HashMap<>();
        Map<Integer, List<String>> testID2oriResults = new HashMap<>();
        Map<Integer, List<String>> testID2upResults = new HashMap<>();

        for (TestPacket tp : stackedTestPacket.getTestPacketList()) {
            logger.trace("Execute testpacket " + tp.systemID + " " +
                    tp.testPacketID);
            executor.executeCommands(tp.originalCommandSequenceList);

            FeedBack[] feedBacks = new FeedBack[stackedTestPacket.nodeNum];
            for (int i = 0; i < stackedTestPacket.nodeNum; i++) {
                feedBacks[i] = new FeedBack();
            }
            ExecutionDataStore[] oriCoverages = executor
                    .collectCoverageSeparate("original");
            if (oriCoverages != null) {
                for (int nodeIdx = 0; nodeIdx < stackedTestPacket.nodeNum; nodeIdx++) {
                    feedBacks[nodeIdx].originalCodeCoverage = oriCoverages[nodeIdx];
                }
            }
            testID2FeedbackPacket.put(
                    tp.testPacketID,
                    new FeedbackPacket(tp.systemID, stackedTestPacket.nodeNum,
                            tp.testPacketID, feedBacks));

            List<String> oriResult = executor
                    .executeCommands(tp.validationCommandSequneceList);
            testID2oriResults.put(tp.testPacketID, oriResult);
        }

        StackedFeedbackPacket stackedFeedbackPacket = new StackedFeedbackPacket();
        stackedFeedbackPacket.stackedCommandSequenceStr = recordStackedTestPacket(
                stackedTestPacket);

        // Execute the test plan
        // TODO: We want to collect the feedback before a node is upgraded
        boolean status = executor.execute(testPlanPacket.getTestPlan());

        // test plan coverage
        FeedBack[] testPlanFeedBacks = new FeedBack[nodeNum];
        for (int i = 0; i < nodeNum; i++) {
            testPlanFeedBacks[i] = new FeedBack();
        }

        for (int i = 0; i < nodeNum; i++) {
            if (executor.oriCoverage[i] != null)
                testPlanFeedBacks[i].originalCodeCoverage = executor.oriCoverage[i];
        }

        if (!status) {
            // TODO
            // We already met some problems with test plan, we'll stop here
            // This case is considered as a failed case
            // We need to record all the test cases and the plan as a crash
            // report

            // This can only be the upgrade failure
            TestPlanFeedbackPacket testPlanFeedbackPacket = new TestPlanFeedbackPacket(
                    testPlanPacket.systemID, nodeNum,
                    testPlanPacket.testPacketID, null);
            int buggyEventIdx = executor.eventIdx;
            testPlanFeedbackPacket.isEventFailed = true;

            String eventFailedReport = "";
            eventFailedReport += String.format(
                    "Test plan execution failed at event[%d]\n\n",
                    buggyEventIdx);

            eventFailedReport += new String(
                    "executionId = " + executor.executorID + "\n");
            eventFailedReport += String.format(
                    "ConfigIdx = " + stackedTestPacket.configIdx + "\n\n");
            mixedTestPacketStr = recordMixedTestPacket(mixedTestPacket);
            eventFailedReport += mixedTestPacketStr;
            testPlanFeedbackPacket.eventFailedReport = eventFailedReport;

            testPlanFeedbackPacket.isInconsistent = false;
            testPlanFeedbackPacket.inconsistencyReport = "";

            MixedFeedbackPacket mixedFeedbackPacket = new MixedFeedbackPacket(
                    stackedFeedbackPacket, testPlanFeedbackPacket);
            return mixedFeedbackPacket;
        }

        // ----test plan upgrade coverage----
        ExecutionDataStore[] upCoverages = executor
                .collectCoverageSeparate("upgraded");
        if (upCoverages != null) {
            for (int nodeIdx = 0; nodeIdx < nodeNum; nodeIdx++) {
                testPlanFeedBacks[nodeIdx].upgradedCodeCoverage = upCoverages[nodeIdx];
            }
        }
        TestPlanFeedbackPacket testPlanFeedbackPacket = new TestPlanFeedbackPacket(
                testPlanPacket.systemID, nodeNum,
                testPlanPacket.testPacketID, testPlanFeedBacks);

        // ----stacked read upgrade coverage----
        for (TestPacket tp : stackedTestPacket.getTestPacketList()) {
            List<String> upResult = executor
                    .executeCommands(tp.validationCommandSequneceList);
            testID2upResults.put(tp.testPacketID, upResult);
            if (Config.getConf().collUpFeedBack) {
                upCoverages = executor
                        .collectCoverageSeparate("upgraded");
                if (upCoverages != null) {
                    for (int nodeIdx = 0; nodeIdx < stackedTestPacket.nodeNum; nodeIdx++) {
                        testID2FeedbackPacket.get(
                                tp.testPacketID).feedBacks[nodeIdx].upgradedCodeCoverage = upCoverages[nodeIdx];
                    }
                }
            }
        }
        // Check read results consistency
        for (TestPacket tp : stackedTestPacket.getTestPacketList()) {
            Pair<Boolean, String> compareRes = executor
                    .checkResultConsistency(
                            testID2oriResults.get(tp.testPacketID),
                            testID2upResults.get(tp.testPacketID));

            FeedbackPacket feedbackPacket = testID2FeedbackPacket
                    .get(tp.testPacketID);

            if (!compareRes.left) {
                // Log the failure info into feedback packet

                // Creating the failure report
                StringBuilder failureReport = new StringBuilder();
                failureReport.append(
                        "Results are inconsistent between two versions\n");

                failureReport.append(
                        "ConfigIdx = " + stackedTestPacket.configIdx + "\n\n");
                failureReport
                        .append("executionId = " + executor.executorID + "\n");

                failureReport.append(compareRes.right);
                failureReport.append(recordSingleTestPacket(tp));
                failureReport.append("\n[Full Command Sequence]\n");
                if (mixedTestPacketStr == null)
                    mixedTestPacketStr = recordMixedTestPacket(mixedTestPacket);
                failureReport.append(mixedTestPacketStr);

                // Create the feedback packet
                feedbackPacket.isInconsistent = true;
                feedbackPacket.inconsistencyReport = failureReport
                        .toString();
            } else {
                feedbackPacket.isInconsistent = false;
            }
            stackedFeedbackPacket.addFeedbackPacket(feedbackPacket);
        }

        tearDownExecutor();
        return new MixedFeedbackPacket(stackedFeedbackPacket,
                testPlanFeedbackPacket);
    }

    private String recordStackedTestPacket(
            StackedTestPacket stackedTestPacket) {
        StringBuilder sb = new StringBuilder();
        for (TestPacket tp : stackedTestPacket.getTestPacketList()) {
            for (String cmdStr : tp.originalCommandSequenceList) {
                sb.append(cmdStr).append("\n");
            }
            sb.append("\n");
            for (String cmdStr : tp.validationCommandSequneceList) {
                sb.append(cmdStr).append("\n");
            }
            sb.append("\n\n");
        }
        return sb.toString();
    }

    private String recordFullStopPacket(FullStopPacket fullStopPacket) {
        return String.format("nodeNum = %d\n", fullStopPacket.getNodeNum()) +
                fullStopPacket.fullStopUpgrade;
    }

    private String recordTestPlanPacket(TestPlanPacket testPlanPacket) {
        return String.format("nodeNum = %d\n", testPlanPacket.getNodeNum()) +
                testPlanPacket.getTestPlan().toString();
    }

    private String recordMixedTestPacket(MixedTestPacket mixedTestPacket) {
        return recordTestPlanPacket(mixedTestPacket.testPlanPacket) +
                "\n" +
                recordStackedTestPacket(mixedTestPacket.stackedTestPacket);
    }

    private String recordSingleTestPacket(TestPacket tp) {
        StringBuilder sb = new StringBuilder();
        sb.append("[Original Command Sequence]\n");
        for (String commandStr : tp.originalCommandSequenceList) {
            sb.append(commandStr).append("\n");
        }
        sb.append("\n\n");
        sb.append("[Read Command Sequence]\n");
        for (String commandStr : tp.validationCommandSequneceList) {
            sb.append(commandStr).append("\n");
        }
        return sb.toString();
    }

    private Map<Integer, Map<String, Pair<String, String>>> stateCompare(
            Map<Integer, Map<String, String>> fullStopStates,
            Map<Integer, Map<String, String>> rollingStates) {
        // state value is encoded via Base64, decode is needed
        // how to compare?
        // - simple string comparison
        Map<Integer, Map<String, Pair<String, String>>> inconsistentStates = new HashMap<>();
        if (fullStopStates.keySet().size() != rollingStates.keySet().size()) {
            throw new RuntimeException(
                    "node num is different between full-stop upgrade" +
                            "and rolling upgrade");
        }
        for (int nodeId : fullStopStates.keySet()) {
            Map<String, String> fStates = fullStopStates.get(nodeId);
            Map<String, String> rStates = rollingStates.get(nodeId);
            for (String stateName : fStates.keySet()) {
                String fstateValue = Utilities
                        .decodeString(fStates.get(stateName));
                String rstateValue = Utilities
                        .decodeString(rStates.get(stateName));
                if (!fstateValue.equals(rstateValue)) {
                    if (!inconsistentStates.containsKey(nodeId))
                        inconsistentStates.put(nodeId, new HashMap<>());
                    inconsistentStates.get(nodeId).put(stateName,
                            new Pair<>(fstateValue, rstateValue));
                }
            }
        }
        return inconsistentStates;
    }
}
