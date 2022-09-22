package org.zlab.upfuzz.fuzzingengine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

    private Thread t; // new version stop + old version restart

    public static Map<Integer, Pair<List<String>, List<String>>> testId2Sequence;

    FuzzingClient() {
        init();
        if (Config.getConf().system.equals("cassandra")) {
            executor = new CassandraExecutor();
        } else if (Config.getConf().system.equals("hdfs")) {
            executor = new HdfsExecutor();
        }
        t = new Thread(() -> {
            try {
                executor.startup();
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }); // Startup before tests
        t.start();
    }

    private void init() {
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

    public void waitForClusterUp() {
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
    }

    public void restartCluster() {
        t = new Thread(() -> {
            executor.upgradeTeardown();
            executor.clearState();
            executor.teardown();
            try {
                executor.startup();
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        });
        t.start();
    }

    /**
     * start the old version system, execute and count the coverage of all
     * test cases of stackedFeedbackPacket, perform an upgrade process, check
     * the (1) upgrade process failed (2) result inconsistency
     * @param stackedTestPacket the stacked test packets from server
     * @throws Exception
     */
    public StackedFeedbackPacket executeStackedTestPacket(
            StackedTestPacket stackedTestPacket)
            throws Exception {
        // make sure the system has is up
        waitForClusterUp();

        Map<Integer, FeedbackPacket> testID2FeedbackPacket = new HashMap<>();
        Map<Integer, List<String>> testID2oriResults = new HashMap<>();
        Map<Integer, List<String>> testID2upResults;

        FeedBack fb;
        for (TestPacket tp : stackedTestPacket.getTestPacketList()) {
            logger.trace("Execute testpacket " + tp.systemID + " " +
                    tp.testPacketID);
            executor.execute(tp);
            fb = new FeedBack();
            fb.originalCodeCoverage = executor.collect("original");
            if (fb.originalCodeCoverage == null) {
                logger.info("ERROR: null origin code coverage");
                System.exit(1);
            }
            testID2FeedbackPacket.put(
                    tp.testPacketID,
                    new FeedbackPacket(tp.systemID, tp.testPacketID, fb));

            List<String> oriResult = executor.executeRead(tp.testPacketID);
            testID2oriResults.put(tp.testPacketID, oriResult);
        }

        // Perform upgrade (1) check whether upgrade succeeds (2) new version
        // read
        // results, compare
        // executor.teardown();
        executor.saveSnapshot();
        executor.moveSnapShot();

        StackedFeedbackPacket stackedFeedbackPacket = new StackedFeedbackPacket();
        stackedFeedbackPacket.stackedCommandSequenceStr = recordAllStackedTests(
                stackedTestPacket);

        boolean ret = executor.upgradeTest();

        if (!ret) {
            // upgrade process failed
            logger.info("upgrade failed");
            stackedFeedbackPacket.isUpgradeProcessFailed = true;
        } else {
            // upgrade process succeeds, compare results here
            logger.info("upgrade succeed");
            testID2upResults = executor.getTestId2newVersionResult();

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

                    failureReport.append("Original Command Sequence\n");
                    for (String commandStr : tp.originalCommandSequenceList) {
                        failureReport.append(commandStr + "\n");
                    }
                    failureReport.append("\n\n");
                    failureReport.append("Read Command Sequence\n");
                    for (String commandStr : tp.validationCommandSequneceList) {
                        failureReport.append(commandStr + "\n");
                    }

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
        restartCluster();
        return stackedFeedbackPacket;
    }

    // Test Plan: The commands are interleaving with upgrade operations and
    // faults. So we only collect the final coverage.
    public TestPlanFeedbackPacket executeTestPlanPacket(
            TestPlanPacket testPlanPacket) {
        waitForClusterUp();
        // TestPlan only contains one test sequence
        // We need to compare the results between two versions for once
        // Then we return the feedback packet
        boolean status = executor.execute(testPlanPacket.getTestPlan());
        // For test plan, we don't distinguish the old version coverage
        // and the new verison coverage. We only collect the final coverage

        if (!status) {
            // The test plan has some problems
            // - (1) Some event cannot execute correctly
            // - (2) There is an inconsistency between rolling upgrade and
            // full-stop upgrade
            // We should report it.
            logger.error("The test plan execution met a problem");
        }

        FeedBack fb = new FeedBack();
        fb.upgradedCodeCoverage = executor.collect("upgraded");
        if (fb.upgradedCodeCoverage == null) {
            logger.info("ERROR: null upgrade code coverage");
            System.exit(1);
        }
        TestPlanFeedbackPacket testPlanFeedbackPacket = new TestPlanFeedbackPacket(
                testPlanPacket.systemID, testPlanPacket.testPacketID, fb);

        restartCluster();
        return testPlanFeedbackPacket;
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

    enum FuzzingClientActions {
        start, collect;
    }
}
