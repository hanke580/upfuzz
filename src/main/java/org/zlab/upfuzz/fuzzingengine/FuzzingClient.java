package org.zlab.upfuzz.fuzzingengine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jacoco.core.data.ExecutionDataStore;
import org.zlab.upfuzz.cassandra.CassandraExecutor;
import org.zlab.upfuzz.fuzzingengine.Packet.*;
import org.zlab.upfuzz.fuzzingengine.executor.Executor;
import org.zlab.upfuzz.fuzzingengine.testplan.TestPlan;
import org.zlab.upfuzz.hdfs.HdfsExecutor;
import org.zlab.upfuzz.utils.Pair;

public class FuzzingClient {
    static Logger logger = LogManager.getLogger(FuzzingClient.class);

    public static int epochStartTestId;

    public Executor executor;

    // If the cluster cannot start up for 3 times, it means some serious
    // problems
    int CLUSTER_START_RETRY = 3;

    public static Map<Integer, Pair<List<String>, List<String>>> testId2Sequence;

    FuzzingClient() {
        epochStartTestId = 0; // FIXME: It might not be zero
        testId2Sequence = new HashMap<>();

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

    public void initExecutor(int nodeNum) {
        if (Config.getConf().system.equals("cassandra")) {
            executor = new CassandraExecutor(nodeNum);
        } else if (Config.getConf().system.equals("hdfs")) {
            // TODO: modify later
            executor = new HdfsExecutor();
        }
    }

    public void startUpExecutor() {
        for (int i = 0; i < CLUSTER_START_RETRY; i++) {
            try {
                executor.startup();
                return;
            } catch (Exception e) {
                executor.teardown();
                e.printStackTrace();
            }
        }
        throw new RuntimeException("cluster cannot start up");
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
     * @throws Exception
     */
    public StackedFeedbackPacket executeStackedTestPacket(
            StackedTestPacket stackedTestPacket) {
        String stackedTestPacketStr = null;
        // make sure the system has is up

        initExecutor(stackedTestPacket.nodeNum);
        startUpExecutor();

        Map<Integer, FeedbackPacket> testID2FeedbackPacket = new HashMap<>();
        Map<Integer, List<String>> testID2oriResults = new HashMap<>();
        Map<Integer, List<String>> testID2upResults = new HashMap<>();

        for (TestPacket tp : stackedTestPacket.getTestPacketList()) {
            logger.trace("Execute testpacket " + tp.systemID + " " +
                    tp.testPacketID);
            executor.execute(tp.originalCommandSequenceList);

            FeedBack[] feedBacks = new FeedBack[stackedTestPacket.nodeNum];
            for (int i = 0; i < stackedTestPacket.nodeNum; i++) {
                feedBacks[i] = new FeedBack();
            }
            ExecutionDataStore[] oriCoverages = executor
                    .collectCoverageSeparate("original");
            for (int nodeIdx = 0; nodeIdx < stackedTestPacket.nodeNum; nodeIdx++) {
                feedBacks[nodeIdx].originalCodeCoverage = oriCoverages[nodeIdx];
            }
            testID2FeedbackPacket.put(
                    tp.testPacketID,
                    new FeedbackPacket(tp.systemID, stackedTestPacket.nodeNum,
                            tp.testPacketID, feedBacks));

            List<String> oriResult = executor
                    .execute(tp.validationCommandSequneceList);
            testID2oriResults.put(tp.testPacketID, oriResult);
        }

        executor.saveSnapshot();

        StackedFeedbackPacket stackedFeedbackPacket = new StackedFeedbackPacket();
        stackedFeedbackPacket.stackedCommandSequenceStr = recordStackedTestPacket(
                stackedTestPacket);

        // Upgrade should only contain the upgrade process
        boolean ret = executor.upgrade();

        if (!ret) {
            // upgrade process failed
            logger.info("upgrade failed");
            stackedFeedbackPacket.isUpgradeProcessFailed = true;
            StringBuilder sb = new StringBuilder();
            sb.append("[upgrade failed]\n[Full Command Sequence]\n");
            if (stackedTestPacketStr == null)
                stackedTestPacketStr = recordStackedTestPacket(
                        stackedTestPacket);
            sb.append(stackedTestPacketStr);
            stackedFeedbackPacket.upgradeFailureReport = sb.toString();
        } else {
            // upgrade process succeeds, compare results here
            logger.info("upgrade succeed");

            for (TestPacket tp : stackedTestPacket.getTestPacketList()) {
                List<String> upResult = executor
                        .execute(tp.validationCommandSequneceList);
                testID2upResults.put(tp.testPacketID, upResult);
                if (Config.getConf().collUpFeedBack) {

                    ExecutionDataStore[] upCoverages = executor
                            .collectCoverageSeparate("upgraded");
                    for (int nodeIdx = 0; nodeIdx < stackedTestPacket.nodeNum; nodeIdx++) {
                        testID2FeedbackPacket.get(
                                tp.testPacketID).feedBacks[nodeIdx].upgradedCodeCoverage = upCoverages[nodeIdx];
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

    // Test Plan: The commands are interleaving with upgrade operations and
    // faults. So we only collect the final coverage.
    public TestPlanFeedbackPacket executeTestPlanPacket(
            TestPlanPacket testPlanPacket) {
        String testPlanPacketStr = null;
        int nodeNum = testPlanPacket.getNodeNum();

        initExecutor(testPlanPacket.getNodeNum());
        startUpExecutor();

        // TestPlan only contains one test sequence
        // We need to compare the results between two versions for once
        // Then we return the feedback packet
        boolean status = executor.execute(testPlanPacket.getTestPlan());
        // For test plan, we don't distinguish the old version coverage
        // and the new verison coverage. We only collect the final coverage

        FeedBack[] feedBacks = new FeedBack[nodeNum];
        for (int i = 0; i < nodeNum; i++) {
            feedBacks[i] = new FeedBack();
        }
        ExecutionDataStore[] upCoverages = executor
                .collectCoverageSeparate("upgraded");
        for (int nodeIdx = 0; nodeIdx < nodeNum; nodeIdx++) {
            feedBacks[nodeIdx].upgradedCodeCoverage = upCoverages[nodeIdx];
        }

        logger.info("Finished Execution");
        TestPlanFeedbackPacket testPlanFeedbackPacket = new TestPlanFeedbackPacket(
                testPlanPacket.systemID, nodeNum,
                testPlanPacket.testPacketID, feedBacks);
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
            if (testPlanPacketStr == null) {
                testPlanPacketStr = recordTestPlanPacket(testPlanPacket);
            }
            eventFailedReport.append(testPlanPacketStr);
            testPlanFeedbackPacket.eventFailedReport = eventFailedReport
                    .toString();

            testPlanFeedbackPacket.isInconsistent = false;
            testPlanFeedbackPacket.inconsistencyReport = "";

            logger.error(String.format(
                    "The test plan execution met a problem when executing event[%d]",
                    buggyEventIdx));
        }

        // TODO: We should also control the nodeNum in the test file
        // Start up one cluster, execute the test plan, keep this cluster
        // for debugging and exit.
        if (Config.getConf().startUpOneCluster) {
            logger.info("Start up a cluster and leave it for debugging");
            try {
                Thread.sleep(1800 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.exit(1);

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

        initExecutor(nodeNum);
        startUpExecutor();

        Map<Integer, FeedbackPacket> testID2FeedbackPacket = new HashMap<>();
        Map<Integer, List<String>> testID2oriResults = new HashMap<>();
        Map<Integer, List<String>> testID2upResults = new HashMap<>();

        for (TestPacket tp : stackedTestPacket.getTestPacketList()) {
            logger.trace("Execute testpacket " + tp.systemID + " " +
                    tp.testPacketID);
            executor.execute(tp.originalCommandSequenceList);

            FeedBack[] feedBacks = new FeedBack[stackedTestPacket.nodeNum];
            for (int i = 0; i < stackedTestPacket.nodeNum; i++) {
                feedBacks[i] = new FeedBack();
            }
            ExecutionDataStore[] oriCoverages = executor
                    .collectCoverageSeparate("original");
            for (int nodeIdx = 0; nodeIdx < stackedTestPacket.nodeNum; nodeIdx++) {
                feedBacks[nodeIdx].originalCodeCoverage = oriCoverages[nodeIdx];
            }
            testID2FeedbackPacket.put(
                    tp.testPacketID,
                    new FeedbackPacket(tp.systemID, stackedTestPacket.nodeNum,
                            tp.testPacketID, feedBacks));

            List<String> oriResult = executor
                    .execute(tp.validationCommandSequneceList);
            testID2oriResults.put(tp.testPacketID, oriResult);
        }

        StackedFeedbackPacket stackedFeedbackPacket = new StackedFeedbackPacket();
        stackedFeedbackPacket.stackedCommandSequenceStr = recordStackedTestPacket(
                stackedTestPacket);

        // Execute the test plan
        boolean status = executor.execute(testPlanPacket.getTestPlan());

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
            if (mixedTestPacketStr == null)
                mixedTestPacketStr = recordMixedTestPacket(mixedTestPacket);
            eventFailedReport += mixedTestPacketStr;
            testPlanFeedbackPacket.eventFailedReport = eventFailedReport;

            testPlanFeedbackPacket.isInconsistent = false;
            testPlanFeedbackPacket.inconsistencyReport = "";

            MixedFeedbackPacket mixedFeedbackPacket = new MixedFeedbackPacket(
                    stackedFeedbackPacket, testPlanFeedbackPacket);
            mixedFeedbackPacket.testPlanFailed = true;
            mixedFeedbackPacket.stackedTestFailed = false;

            return mixedFeedbackPacket;
        }

        FeedBack[] feedBacks = new FeedBack[nodeNum];
        for (int i = 0; i < nodeNum; i++) {
            feedBacks[i] = new FeedBack();
        }
        ExecutionDataStore[] upCoverages = executor
                .collectCoverageSeparate("upgraded");
        for (int nodeIdx = 0; nodeIdx < nodeNum; nodeIdx++) {
            feedBacks[nodeIdx].upgradedCodeCoverage = upCoverages[nodeIdx];
        }
        TestPlanFeedbackPacket testPlanFeedbackPacket = new TestPlanFeedbackPacket(
                testPlanPacket.systemID, nodeNum,
                testPlanPacket.testPacketID, feedBacks);

        // Execute the validation commands
        for (TestPacket tp : stackedTestPacket.getTestPacketList()) {
            List<String> upResult = executor
                    .execute(tp.validationCommandSequneceList);
            testID2upResults.put(tp.testPacketID, upResult);
            if (Config.getConf().collUpFeedBack) {

                upCoverages = executor
                        .collectCoverageSeparate("upgraded");
                for (int nodeIdx = 0; nodeIdx < stackedTestPacket.nodeNum; nodeIdx++) {
                    testID2FeedbackPacket.get(
                            tp.testPacketID).feedBacks[nodeIdx].upgradedCodeCoverage = upCoverages[nodeIdx];
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

    private String recordTestPlanPacket(TestPlanPacket testPlanPacket) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("nodeNum = %d\n", testPlanPacket.getNodeNum()));
        sb.append(testPlanPacket.getTestPlan().toString());
        return sb.toString();
    }

    private String recordMixedTestPacket(MixedTestPacket mixedTestPacket) {
        StringBuilder sb = new StringBuilder();
        sb.append(recordTestPlanPacket(mixedTestPacket.testPlanPacket));
        sb.append("\n");
        sb.append(recordStackedTestPacket(mixedTestPacket.stackedTestPacket));
        return sb.toString();
    }

    private String recordSingleTestPacket(TestPacket tp) {
        StringBuilder sb = new StringBuilder();
        sb.append("[Original Command Sequence]\n");
        for (String commandStr : tp.originalCommandSequenceList) {
            sb.append(commandStr + "\n");
        }
        sb.append("\n\n");
        sb.append("[Read Command Sequence]\n");
        for (String commandStr : tp.validationCommandSequneceList) {
            sb.append(commandStr + "\n");
        }
        return sb.toString();
    }

    enum FuzzingClientActions {
        start, collect;
    }
}
