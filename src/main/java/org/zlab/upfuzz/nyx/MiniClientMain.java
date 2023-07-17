package org.zlab.upfuzz.nyx;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.fuzzingengine.Config.Configuration;
import org.jacoco.core.data.ExecutionDataStore;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.fuzzingengine.FeedBack;
import org.zlab.upfuzz.fuzzingengine.FuzzingClient;
import org.zlab.upfuzz.fuzzingengine.LogInfo;
import org.zlab.upfuzz.fuzzingengine.executor.Executor;
import org.zlab.upfuzz.fuzzingengine.packet.FeedbackPacket;
import org.zlab.upfuzz.fuzzingengine.packet.StackedFeedbackPacket;
import org.zlab.upfuzz.fuzzingengine.packet.StackedTestPacket;
import org.zlab.upfuzz.fuzzingengine.packet.TestPacket;
import org.zlab.upfuzz.utils.Pair;
import org.zlab.upfuzz.utils.Utilities;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

public class MiniClientMain {
    static Logger logger = LogManager.getLogger(MiniClientMain.class);

    // Where all files are searched for
    static final String workdir = "/miniClientWorkdir";

    // If the cluster startup fails 3 times, then give up
    static final int CLUSTER_START_RETRY = 3;

    public static boolean startUpExecutor(Executor executor) {
        for (int i = 0; i < CLUSTER_START_RETRY; i++) {
            try {
                if (executor.startup())
                    return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
            executor.teardown();
        }
        return false;
    }

    public static void main(String[] args) {
        // System.out.println("Starting up MiniClient!");
        // setup our input scanner
        Scanner stdin = new Scanner(System.in);

        // ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream nullStream = new PrintStream(new OutputStream() {
            public void write(int b) throws IOException {
            }
        });
        PrintStream cAgent = System.out;
        System.setOut(nullStream);

        try {
            File configFile = new File("/home/nyx/upfuzz/config.json");
            Configuration cfg = new Gson().fromJson(
                    new FileReader(configFile), Configuration.class);
            Config.setInstance(cfg);
        } catch (JsonSyntaxException | JsonIOException
                | FileNotFoundException e) {
            e.printStackTrace();
        }

        // there should be a defaultStackedTestPacket.ser in whatever working
        // dir this was started up in
        // there also should be a config file
        Path defaultStackedTestPath = Paths.get(workdir,
                "defaultStackedTestPacket.ser"); // "/miniClientWorkdir/defaultStackedTestPacket.ser"
        Path defaultConfigPath = Paths.get(workdir, "stackedTestConfigFile"); // "/miniClientWorkdir/stackedTestConfigFile"
        StackedTestPacket defaultStackedTestPacket;
        try { // if any of these catches go through we have a big problem
            defaultStackedTestPacket = (StackedTestPacket) Utilities
                    .readObjectFromFile(defaultStackedTestPath.toFile());
        } catch (ClassNotFoundException | IOException | ClassCastException e) {
            e.printStackTrace();
            return;
        }

        Executor executor = FuzzingClient.initExecutor(
                defaultStackedTestPacket.nodeNum, null, defaultConfigPath);

        if (!startUpExecutor(executor)) {
            // was unable to startup the docker system
            cAgent.print("F"); // F for failed
            return;
        } else {
            cAgent.print("R"); // READY_FOR_TESTS
        }
        // c agent should checkpoint the vm here
        System.err.println("Waiting to start TESTING");
        // wait for c agent to tell us to start testing
        if (!stdin.nextLine().equals("START_TESTING")) {
            // possible sync issue
            System.err.println("POSSIBLE SYNC ERROR OCCURED IN MINICLIENT");
            return;
        }

        // Read the new stackedTestPacket to be used for test case sending
        Path stackedTestPath = Paths.get(workdir, "mainStackedTestPacket.ser"); // "/miniClientWorkdir/mainStackedTestPacket.ser"
        StackedTestPacket stackedTestPacket;
        try { // if any of these catches go through we have a big problem
            stackedTestPacket = (StackedTestPacket) Utilities
                    .readObjectFromFile(stackedTestPath.toFile());
        } catch (ClassNotFoundException | IOException | ClassCastException e) {
            e.printStackTrace();
            return;
        }

        StackedFeedbackPacket stackedFeedbackPacket = runTheTests(executor,
                stackedTestPacket);
        Path stackedFeedbackPath = Paths.get(workdir,
                "stackedFeedbackPacket.ser"); // "/miniClientWorkdir/stackedFeedbackPacket.ser"
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(
                stackedFeedbackPath.toAbsolutePath().toString()))) {
            stackedFeedbackPacket.write(out);
        } catch (IOException e) {
            e.printStackTrace(System.err);
            return;
        }

        // lets c agent know that the stackedFeedbackFile is ready
        cAgent.print("2");

        // sit here, if any communication desync happened
        // this should be passed and system will crash
        stdin.nextLine();

        stdin.close();
    }

    public static StackedFeedbackPacket runTheTests(Executor executor,
            StackedTestPacket stackedTestPacket) {
        Map<Integer, FeedbackPacket> testID2FeedbackPacket = new HashMap<>();
        Map<Integer, List<String>> testID2oriResults = new HashMap<>();
        Map<Integer, List<String>> testID2upResults = new HashMap<>();
        // if the middle of test has already broken an invariant
        // we stop executing.
        int executedTestNum = 0;
        boolean breakNewInv = false;

        int[] lastBrokenInv = null;
        if (Config.getConf().useLikelyInv) {
            lastBrokenInv = executor.getBrokenInv();
        }

        for (TestPacket tp : stackedTestPacket.getTestPacketList()) {
            executedTestNum++;
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
                    .executeCommands(tp.validationCommandSequenceList);
            testID2oriResults.put(tp.testPacketID, oriResult);

            // check invariants!
            // calculate the diff between the current inv vs last inv
            if (Config.getConf().useLikelyInv) {
                // calculate the startup invariant, this should give credit
                // to configuration file
                int[] curBrokenInv = executor.getBrokenInv();
                int[] diffBrokenInv = Utilities
                        .computeDiffBrokenInv(lastBrokenInv, curBrokenInv);
                lastBrokenInv = curBrokenInv;
                testID2FeedbackPacket
                        .get(tp.testPacketID).brokenInvs = diffBrokenInv;
                // check whether any new invariant is broken
                for (int i = 0; i < diffBrokenInv.length; i++) {
                    if (!stackedTestPacket.ignoredInvs.contains(i)
                            && diffBrokenInv[i] != 0) {
                        testID2FeedbackPacket
                                .get(tp.testPacketID).breakNewInv = true;
                        breakNewInv = true;
                        break;
                    }
                }
                if (Config.getConf().skip && breakNewInv)
                    break;
            }

        }

        StackedFeedbackPacket stackedFeedbackPacket = new StackedFeedbackPacket(
                stackedTestPacket.configFileName,
                Utilities.extractTestIDs(stackedTestPacket));
        stackedFeedbackPacket.fullSequence = FuzzingClient
                .recordStackedTestPacket(
                        stackedTestPacket);
        stackedFeedbackPacket.breakNewInv = breakNewInv;

        // LOG checking1
        Map<Integer, LogInfo> logInfoBeforeUpgrade = null;
        if (Config.getConf().enableLogCheck) {
            // logger.info("[HKLOG] error log checking");
            logInfoBeforeUpgrade = executor.grepLogInfo();
        }

        boolean ret = executor.fullStopUpgrade();

        if (!ret) {
            // upgrade failed
            String upgradeFailureReport = FuzzingClient.genUpgradeFailureReport(
                    executor.executorID, stackedTestPacket.configFileName);
            stackedFeedbackPacket.isUpgradeProcessFailed = true;
            stackedFeedbackPacket.upgradeFailureReport = upgradeFailureReport;
            // tearDownExecutor();
            return stackedFeedbackPacket;
        }

        // logger.info("upgrade succeed");
        stackedFeedbackPacket.isUpgradeProcessFailed = false;

        for (int testPacketIdx = 0; testPacketIdx < executedTestNum; testPacketIdx++) {
            TestPacket tp = stackedTestPacket.getTestPacketList()
                    .get(testPacketIdx);
            List<String> upResult = executor
                    .executeCommands(tp.validationCommandSequenceList);
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
        for (int testPacketIdx = 0; testPacketIdx < executedTestNum; testPacketIdx++) {
            TestPacket tp = stackedTestPacket.getTestPacketList()
                    .get(testPacketIdx);
            Pair<Boolean, String> compareRes = executor
                    .checkResultConsistency(
                            testID2oriResults.get(tp.testPacketID),
                            testID2upResults.get(tp.testPacketID), true);

            FeedbackPacket feedbackPacket = testID2FeedbackPacket
                    .get(tp.testPacketID);

            if (!compareRes.left) {
                String failureReport = FuzzingClient.genInconsistencyReport(
                        executor.executorID, stackedTestPacket.configFileName,
                        compareRes.right,
                        FuzzingClient.recordSingleTestPacket(tp));
                feedbackPacket.isInconsistent = true;
                feedbackPacket.inconsistencyReport = failureReport;
            }
            // logger.debug("testID2upResults = " + testID2upResults
            // .get(tp.testPacketID));
            feedbackPacket.validationReadResults = testID2upResults
                    .get(tp.testPacketID);
            stackedFeedbackPacket.addFeedbackPacket(feedbackPacket);
        }
        // logger.info(executor.systemID + " executor: " + executor.executorID
        // + " finished execution");

        // test downgrade
        if (Config.getConf().testDowngrade) {
            // logger.info("downgrade cluster");
            boolean downgradeStatus = executor.downgrade();
            if (!downgradeStatus) {
                // downgrade failed
                stackedFeedbackPacket.isDowngradeProcessFailed = true;
                stackedFeedbackPacket.downgradeFailureReport = FuzzingClient
                        .genDowngradeFailureReport(
                                executor.executorID,
                                stackedFeedbackPacket.configFileName);
            }
        }

        // LOG checking2
        if (Config.getConf().enableLogCheck) {
            // logger.info("[HKLOG] error log checking: merge logs");
            assert logInfoBeforeUpgrade != null;
            Map<Integer, LogInfo> logInfo = FuzzingClient.filterErrorLog(
                    logInfoBeforeUpgrade,
                    executor.grepLogInfo());
            if (FuzzingClient.hasERRORLOG(logInfo)) {
                stackedFeedbackPacket.hasERRORLog = true;
                stackedFeedbackPacket.errorLogReport = FuzzingClient
                        .genErrorLogReport(
                                executor.executorID,
                                stackedTestPacket.configFileName,
                                logInfo);
            }
        }
        return stackedFeedbackPacket;
    }
}
