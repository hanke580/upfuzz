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

    public Executor initExecutor(int nodeNum, Set<String> targetSystemStates,
            Path configPath) {
        if (Config.getConf().system.equals("cassandra")) {
            return new CassandraExecutor(nodeNum, targetSystemStates,
                    configPath);
        } else if (Config.getConf().system.equals("hdfs")) {
            return new HdfsExecutor(nodeNum, targetSystemStates,
                    configPath);
        }
        throw new RuntimeException(String.format(
                "System %s is not supported yet, supported system: cassandra, hdfs",
                Config.getConf().system));
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
        Path configPath = Paths.get(configDirPath.toString(),
                stackedTestPacket.configFileName);
        logger.info("[HKLOG] configPath = " + configPath);

        // config verification
        if (Config.getConf().verifyConfig) {
            boolean validConfig = verifyConfig(configPath);
            if (!validConfig) {
                logger.error(
                        "problem with configuration! system cannot start up");
                return null;
            }
        }

        executor = initExecutor(stackedTestPacket.nodeNum, null, configPath);
        boolean startUpStatus = startUpExecutor();
        if (!startUpStatus) {
            // old version **cluster** start up problem, this won't be upgrade
            // bugs
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
                            tp.testPacketID, feedBacks, null));

            List<String> oriResult = executor
                    .executeCommands(tp.validationCommandSequneceList);

            testID2oriResults.put(tp.testPacketID, oriResult);
        }

        StackedFeedbackPacket stackedFeedbackPacket = new StackedFeedbackPacket(
                stackedTestPacket.configFileName);
        stackedFeedbackPacket.fullSequence = recordStackedTestPacket(
                stackedTestPacket);

        // LOG checking1
        Map<Integer, LogInfo> logInfoBeforeUpgrade = null;
        if (Config.getConf().enableLogCheck) {
            logger.info("[HKLOG] error checking");
            logInfoBeforeUpgrade = executor.grepLogInfo();
        }

        boolean ret = executor.fullStopUpgrade();

        if (!ret) {
            // upgrade failed
            String upgradeFailureReport = genUpgradeFailureReport(
                    executor.executorID, stackedTestPacket.configFileName);
            stackedFeedbackPacket.isUpgradeProcessFailed = true;
            stackedFeedbackPacket.upgradeFailureReport = upgradeFailureReport;
            tearDownExecutor();
            return stackedFeedbackPacket;
        }

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
                String failureReport = genInconsistencyReport(
                        executor.executorID, stackedTestPacket.configFileName,
                        compareRes.right, recordSingleTestPacket(tp));
                feedbackPacket.isInconsistent = true;
                feedbackPacket.inconsistencyReport = failureReport;
            }
            feedbackPacket.validationReadResults = testID2upResults
                    .get(tp.testPacketID);
            stackedFeedbackPacket.addFeedbackPacket(feedbackPacket);
        }
        logger.info(executor.systemID + " executor: " + executor.executorID
                + " finished execution");

        // test downgrade
        if (Config.getConf().testDowngrade) {
            logger.info("downgrade cluster");
            boolean downgradeStatus = executor.downgrade();
            if (!downgradeStatus) {
                // downgrade failed
                stackedFeedbackPacket.isDowngradeProcessFailed = true;
                stackedFeedbackPacket.downgradeFailureReport = genDowngradeFailureReport(
                        executor.executorID,
                        stackedFeedbackPacket.configFileName);
            }
        }

        // LOG checking2
        if (Config.getConf().enableLogCheck) {
            logger.info("[HKLOG] error checking: merge logs");
            assert logInfoBeforeUpgrade != null;
            Map<Integer, LogInfo> logInfo = filterErrorLog(logInfoBeforeUpgrade,
                    executor.grepLogInfo());
            if (hasERRORLOG(logInfo)) {
                stackedFeedbackPacket.hasERRORLog = true;
                stackedFeedbackPacket.errorLogReport = genErrorLogReport(
                        executor.executorID, stackedTestPacket.configFileName,
                        logInfo);
            }
        }
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
                fullStopPacket.systemID, fullStopPacket.configFileName,
                fullStopPacket.testPacketID, feedBacks, new HashMap<>());
        fullStopFeedbackPacket.fullSequence = recordFullStopPacket(
                fullStopPacket);

        logger.info("[HKLOG] configPath = " + fullStopPacket.configFileName);
        Path configPath = Paths.get(configDirPath.toString(),
                fullStopPacket.configFileName);

        // config verification
        if (Config.getConf().verifyConfig) {
            boolean validConfig = verifyConfig(configPath);
            if (!validConfig) {
                logger.error(
                        "problem with configuration! system cannot start up");
                return null;
            }
        }

        // start up
        executor = initExecutor(nodeNum,
                fullStopPacket.fullStopUpgrade.targetSystemStates, configPath);
        boolean startUpStatus = startUpExecutor();
        if (!startUpStatus) {
            return null;
        }

        // LOG checking1
        Map<Integer, LogInfo> logInfoBeforeUpgrade = null;
        if (Config.getConf().enableLogCheck) {
            logger.info("[HKLOG] error checking");
            logInfoBeforeUpgrade = executor.grepLogInfo();
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
            fullStopFeedbackPacket.isUpgradeProcessFailed = true;
            fullStopFeedbackPacket.upgradeFailureReport = genOriCoverageCollFailureReport(
                    executor.executorID, fullStopPacket.configFileName,
                    recordFullStopPacket(fullStopPacket)) + "Exception:" + e;
            tearDownExecutor();
            return fullStopFeedbackPacket;
        }

        boolean upgradeStatus = executor.fullStopUpgrade();

        if (!upgradeStatus) {
            fullStopFeedbackPacket.isUpgradeProcessFailed = true;
            fullStopFeedbackPacket.upgradeFailureReport = genUpgradeFailureReport(
                    executor.executorID, fullStopPacket.configFileName);
        } else {
            // Upgrade is done successfully, collect coverage and check results
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
                fullStopFeedbackPacket.isUpgradeProcessFailed = true;
                fullStopFeedbackPacket.upgradeFailureReport = genUpCoverageCollFailureReport(
                        executor.executorID, fullStopPacket.configFileName,
                        recordFullStopPacket(fullStopPacket)) + "Exception:"
                        + e;
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

            // Compare results
            Pair<Boolean, String> compareRes = executor
                    .checkResultConsistency(oriResult, upResult);
            if (!compareRes.left) {
                fullStopFeedbackPacket.isInconsistent = true;
                fullStopFeedbackPacket.inconsistencyReport = genInconsistencyReport(
                        executor.executorID, fullStopPacket.configFileName,
                        compareRes.right, recordFullStopPacket(fullStopPacket));

                logger.info("Execution ID = " + executor.executorID
                        + "\ninconsistency: " + compareRes.right);
                if (Config.getConf().startUpClusterForDebugging) {
                    logger.info(String.format(
                            "Start up a cluster %s and leave it for debugging: This is not testing mode! Please set this startUpClusterForDebugging to false for real testing mode",
                            executor.executorID));
                    try {
                        Thread.sleep(3600 * 1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.exit(1);

                }
            }

            // test downgrade
            if (Config.getConf().testDowngrade) {
                boolean downgradeStatus = executor.downgrade();
                if (!downgradeStatus) {
                    // downgrade failed
                    fullStopFeedbackPacket.isDowngradeProcessFailed = true;
                    fullStopFeedbackPacket.downgradeFailureReport = genDowngradeFailureReport(
                            executor.executorID, fullStopPacket.configFileName);
                }
            }
        }

        // LOG checking2
        if (Config.getConf().enableLogCheck) {
            logger.info("[HKLOG] error checking");
            assert logInfoBeforeUpgrade != null;
            Map<Integer, LogInfo> logInfo = filterErrorLog(logInfoBeforeUpgrade,
                    executor.grepLogInfo());
            if (hasERRORLOG(logInfo)) {
                fullStopFeedbackPacket.hasERRORLog = true;
                fullStopFeedbackPacket.errorLogReport = genErrorLogReport(
                        executor.executorID, fullStopPacket.configFileName,
                        logInfo);
            }
        }

        tearDownExecutor();
        return fullStopFeedbackPacket;
    }

    public TestPlanFeedbackPacket executeTestPlanPacket(
            TestPlanPacket testPlanPacket) {

        logger.debug("test plan: \n");
        logger.debug(testPlanPacket.testPlan);

        String testPlanPacketStr = recordTestPlanPacket(testPlanPacket);

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

        String configFileName = "test21";

        Path configPath = Paths.get(configDirPath.toString(), configFileName);
        logger.info("[HKLOG] configPath = " + configPath);

        // config verification
        if (Config.getConf().verifyConfig) {
            boolean validConfig = verifyConfig(configPath);
            if (!validConfig) {
                logger.error(
                        "problem with configuration! system cannot start up");
                return null;
            }
        }

        // start up
        executor = initExecutor(testPlanPacket.getNodeNum(), targetSystemStates,
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
                testPlanPacket.systemID, configFileName,
                testPlanPacket.testPacketID, feedBacks);
        testPlanFeedbackPacket.fullSequence = testPlanPacketStr;

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
            testPlanFeedbackPacket.eventFailedReport = genTestPlanFailureReport(
                    executor.eventIdx, executor.executorID, configFileName,
                    testPlanPacketStr);
            testPlanFeedbackPacket.isEventFailed = true;
            testPlanFeedbackPacket.isInconsistent = false;
            testPlanFeedbackPacket.inconsistencyReport = "";

        } else {
            Pair<Boolean, String> compareRes;
            // collect read results of a test plan
            if (!testPlanPacket.testPlan.validationReadResultsOracle
                    .isEmpty()) {
                List<String> testPlanReadResults = executor
                        .executeCommands(
                                testPlanPacket.testPlan.validationCommands);
                compareRes = executor
                        .checkResultConsistency(
                                testPlanReadResults,
                                testPlanPacket.testPlan.validationReadResultsOracle);
                if (!compareRes.left) {
                    testPlanFeedbackPacket.isInconsistent = true;
                    testPlanFeedbackPacket.inconsistencyReport = genTestPlanInconsistencyReport(
                            executor.executorID, configFileName,
                            compareRes.right, testPlanPacketStr);
                }
            }

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
                testPlanFeedbackPacket.eventFailedReport = genUpCoverageCollFailureReport(
                        executor.executorID, configFileName,
                        recordTestPlanPacket(testPlanPacket)) + "Exception:"
                        + e;
                tearDownExecutor();
                return testPlanFeedbackPacket;
            }
        }

        tearDownExecutor();
        return testPlanFeedbackPacket;
    }

    public MixedFeedbackPacket executeMixedTestPacket(
            MixedTestPacket mixedTestPacket) {

        StackedTestPacket stackedTestPacket = mixedTestPacket.stackedTestPacket;
        TestPlanPacket testPlanPacket = mixedTestPacket.testPlanPacket;

        String testPlanPacketStr = recordTestPlanPacket(testPlanPacket);
        String mixedTestPacketStr = recordMixedTestPacket(mixedTestPacket);

        assert stackedTestPacket.nodeNum == testPlanPacket.getNodeNum();
        int nodeNum = stackedTestPacket.nodeNum;

        Path configPath = Paths.get(configDirPath.toString(),
                stackedTestPacket.configFileName);
        logger.info("[HKLOG] configPath = " + configPath);

        // config verification
        if (Config.getConf().verifyConfig) {
            boolean validConfig = verifyConfig(configPath);
            if (!validConfig) {
                logger.error(
                        "problem with configuration! system cannot start up");
                return null;
            }
        }

        // start up cluster
        executor = initExecutor(nodeNum, null, configPath);

        boolean startUpStatus = startUpExecutor();
        if (!startUpStatus) {
            return null;
        }

        // execute stacked packets
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
                            tp.testPacketID, feedBacks, null));

            List<String> oriResult = executor
                    .executeCommands(tp.validationCommandSequneceList);
            testID2oriResults.put(tp.testPacketID, oriResult);
        }

        StackedFeedbackPacket stackedFeedbackPacket = new StackedFeedbackPacket(
                stackedTestPacket.configFileName);
        stackedFeedbackPacket.fullSequence = mixedTestPacketStr;

        // LOG checking1
        Map<Integer, LogInfo> logInfoBeforeUpgrade = null;
        if (Config.getConf().enableLogCheck) {
            logger.info("[HKLOG] error checking");
            logInfoBeforeUpgrade = executor.grepLogInfo();
        }

        // execute test plan (rolling upgrade + fault)
        boolean status = executor.execute(testPlanPacket.getTestPlan());

        // collect test plan coverage
        FeedBack[] testPlanFeedBacks = new FeedBack[nodeNum];
        for (int i = 0; i < nodeNum; i++) {
            testPlanFeedBacks[i] = new FeedBack();
            if (executor.oriCoverage[i] != null)
                testPlanFeedBacks[i].originalCodeCoverage = executor.oriCoverage[i];
        }

        TestPlanFeedbackPacket testPlanFeedbackPacket = new TestPlanFeedbackPacket(
                testPlanPacket.systemID, stackedTestPacket.configFileName,
                testPlanPacket.testPacketID, testPlanFeedBacks);
        testPlanFeedbackPacket.fullSequence = mixedTestPacketStr;

        if (!status) {
            // one event in the test plan failed
            testPlanFeedbackPacket.isEventFailed = true;

            testPlanFeedbackPacket.eventFailedReport = genTestPlanFailureReport(
                    executor.eventIdx, executor.executorID,
                    stackedTestPacket.configFileName, testPlanPacketStr);
            testPlanFeedbackPacket.isInconsistent = false;
            testPlanFeedbackPacket.inconsistencyReport = "";
        } else {
            Pair<Boolean, String> compareRes;
            // read results comparison between full-stop upgrade and rolling
            // upgrade
            if (!testPlanPacket.testPlan.validationReadResultsOracle
                    .isEmpty()) {
                List<String> testPlanReadResults = executor
                        .executeCommands(
                                testPlanPacket.testPlan.validationCommands);

                compareRes = executor
                        .checkResultConsistency(
                                testPlanReadResults,
                                testPlanPacket.testPlan.validationReadResultsOracle);

                if (!compareRes.left) {
                    testPlanFeedbackPacket.isInconsistent = true;
                    testPlanFeedbackPacket.inconsistencyReport = genTestPlanInconsistencyReport(
                            executor.executorID,
                            stackedTestPacket.configFileName,
                            compareRes.right, testPlanPacketStr);
                }
            }

            // ----test plan upgrade coverage----
            ExecutionDataStore[] upCoverages = executor
                    .collectCoverageSeparate("upgraded");
            if (upCoverages != null) {
                for (int nodeIdx = 0; nodeIdx < nodeNum; nodeIdx++) {
                    testPlanFeedbackPacket.feedBacks[nodeIdx].upgradedCodeCoverage = upCoverages[nodeIdx];
                }
            }

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
                compareRes = executor
                        .checkResultConsistency(
                                testID2oriResults.get(tp.testPacketID),
                                testID2upResults.get(tp.testPacketID));

                FeedbackPacket feedbackPacket = testID2FeedbackPacket
                        .get(tp.testPacketID);

                if (!compareRes.left) {
                    String failureReport = genInconsistencyReport(
                            executor.executorID,
                            stackedTestPacket.configFileName,
                            compareRes.right, recordSingleTestPacket(tp));

                    // Create the feedback packet
                    feedbackPacket.isInconsistent = true;
                    feedbackPacket.inconsistencyReport = failureReport;
                } else {
                    feedbackPacket.isInconsistent = false;
                }
                feedbackPacket.validationReadResults = testID2upResults
                        .get(tp.testPacketID);
                stackedFeedbackPacket.addFeedbackPacket(feedbackPacket);
            }
        }

        // LOG checking2
        if (Config.getConf().enableLogCheck) {
            logger.info("[HKLOG] error checking");
            assert logInfoBeforeUpgrade != null;
            Map<Integer, LogInfo> logInfo = filterErrorLog(logInfoBeforeUpgrade,
                    executor.grepLogInfo());
            if (hasERRORLOG(logInfo)) {
                stackedFeedbackPacket.hasERRORLog = true;
                stackedFeedbackPacket.errorLogReport = genErrorLogReport(
                        executor.executorID, stackedTestPacket.configFileName,
                        logInfo);
                testPlanFeedbackPacket.hasERRORLog = true;
                testPlanFeedbackPacket.errorLogReport = genErrorLogReport(
                        executor.executorID, stackedTestPacket.configFileName,
                        logInfo);
            }
        }

        tearDownExecutor();
        return new MixedFeedbackPacket(stackedFeedbackPacket,
                testPlanFeedbackPacket);
    }

    private boolean verifyConfig(Path configPath) {
        // start up one node in old version, verify old version config file
        // start up one node in new version, verify new version config file
        logger.info("verifying configuration");
        Executor executor = initExecutor(1, null, configPath);
        boolean startUpStatus = executor.startup();

        if (!startUpStatus) {
            logger.error("config cannot start up old version");
            return false;
        }
        startUpStatus = executor.freshStartNewVersion();
        executor.teardown();
        if (!startUpStatus) {
            logger.error("config cannot start up new version");
        }
        return startUpStatus;
    }

    private String genTestPlanFailureReport(int failEventIdx, String executorID,
            String configFileName, String testPlanPacket) {
        return "[Test plan execution failed at event" + failEventIdx + "]\n" +
                "executionId = " + executorID + "\n" +
                "ConfigIdx = " + configFileName + "\n" +
                testPlanPacket + "\n";
    }

    private String genInconsistencyReport(String executorID,
            String configFileName, String inconsistencyRecord,
            String singleTestPacket) {
        return "[Results inconsistency between two versions]\n" +
                "executionId = " + executorID + "\n" +
                "ConfigIdx = " + configFileName + "\n" +
                inconsistencyRecord + "\n" +
                singleTestPacket + "\n";
    }

    private String genTestPlanInconsistencyReport(String executorID,
            String configFileName, String inconsistencyRecord,
            String singleTestPacket) {
        return "[Results inconsistency between full-stop and rolling upgrade]\n"
                +
                "executionId = " + executorID + "\n" +
                "ConfigIdx = " + configFileName + "\n" +
                inconsistencyRecord + "\n" +
                singleTestPacket + "\n";
    }

    private String genUpgradeFailureReport(String executorID,
            String configFileName) {
        return "[Upgrade Failed]\n" +
                "executionId = " + executorID + "\n" +
                "ConfigIdx = " + configFileName + "\n";
    }

    private String genDowngradeFailureReport(String executorID,
            String configFileName) {
        return "[Downgrade Failed]\n" +
                "executionId = " + executorID + "\n" +
                "ConfigIdx = " + configFileName + "\n";
    }

    private String genOriCoverageCollFailureReport(String executorID,
            String configFileName, String singleTestPacket) {
        return "[Original Coverage Collect Failed]\n" +
                "executionId = " + executorID + "\n" +
                "ConfigIdx = " + configFileName + "\n" +
                singleTestPacket + "\n";
    }

    private String genUpCoverageCollFailureReport(String executorID,
            String configFileName, String singleTestPacket) {
        return "[Upgrade Coverage Collect Failed]\n" +
                "executionId = " + executorID + "\n" +
                "ConfigIdx = " + configFileName + "\n" +
                singleTestPacket + "\n";
    }

    private String genErrorLogReport(String executorID, String configFileName,
            Map<Integer, LogInfo> logInfo) {
        StringBuilder ret = new StringBuilder("[ERROR LOG]\n");
        ret.append("executionId = ").append(executorID).append("\n");
        ret.append("ConfigIdx = ").append(configFileName).append("\n");
        for (int i : logInfo.keySet()) {
            if (logInfo.get(i).ERRORMsg.size() > 0) {
                ret.append("Node").append(i).append("\n");
                for (String msg : logInfo.get(i).ERRORMsg) {
                    ret.append(msg).append("\n");
                }
                ret.append("\n");
            }
        }
        return ret.toString();
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

    private Map<Integer, LogInfo> filterErrorLog(
            Map<Integer, LogInfo> logInfoBeforeUpgrade,
            Map<Integer, LogInfo> logInfoAfterUpgrade) {
        Map<Integer, LogInfo> filteredLogInfo = new HashMap<>();
        for (int nodeIdx : logInfoBeforeUpgrade.keySet()) {
            LogInfo beforeUpgradeLogInfo = logInfoBeforeUpgrade.get(nodeIdx);
            LogInfo afterUpgradeLogInfo = logInfoAfterUpgrade.get(nodeIdx);

            LogInfo logInfo = new LogInfo();
            for (String errorMsg : afterUpgradeLogInfo.ERRORMsg) {
                if (!beforeUpgradeLogInfo.ERRORMsg.contains(errorMsg)) {
                    logInfo.addErrorMsg(errorMsg);
                }
            }
            for (String warnMsg : afterUpgradeLogInfo.WARNMsg) {
                if (!beforeUpgradeLogInfo.WARNMsg.contains(warnMsg)) {
                    logInfo.addWARNMsg(warnMsg);
                }
            }
            filteredLogInfo.put(nodeIdx, logInfo);
        }
        return filteredLogInfo;
    }

    private boolean hasERRORLOG(Map<Integer, LogInfo> logInfo) {
        boolean hasErrorLog = false;
        for (int i : logInfo.keySet()) {
            if (logInfo.get(i).ERRORMsg.size() > 0) {
                hasErrorLog = true;
                break;
            }
        }
        return hasErrorLog;
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
