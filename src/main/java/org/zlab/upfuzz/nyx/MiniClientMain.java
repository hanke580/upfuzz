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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.StringWriter;
import java.io.PrintWriter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.ocov.tracker.ObjectGraphCoverage;
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
import org.zlab.upfuzz.fuzzingengine.packet.TestPlanPacket;
import org.zlab.upfuzz.fuzzingengine.packet.TestPlanFeedbackPacket;
import org.zlab.upfuzz.fuzzingengine.packet.Packet.PacketType;
import org.zlab.upfuzz.utils.Pair;
import org.zlab.upfuzz.utils.Utilities;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

public class MiniClientMain {
    // WARNING: This must be disabled otherwise it can
    // log to output and corrupt the process
    // INFO => cClient output

    // Where all files are searched for
    static final String workdir = "/miniClientWorkdir";

    // If the cluster startup fails 3 times, then give up
    static final int CLUSTER_START_RETRY = 3;

    static int testType;
    static String testExecutionLog = "";

    public static void setTestType(int type) {
        testType = type;
    }

    public static String startUpExecutor(Executor executor, int type) {
        for (int i = 0; i < CLUSTER_START_RETRY; i++) {
            try {
                if (executor.startup()) {
                    return "success";
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            executor.teardown();
        }
        return "fail" + type;
    }

    public static void main(String[] args) {
        System.err.println("Starting up MiniClient!");
        // setup our input scanner
        Scanner stdin = new Scanner(System.in);

        String logMessages = "setting stream, ";
        PrintStream nullStream = new PrintStream(new OutputStream() {
            public void write(int b) throws IOException {
            }
        });
        PrintStream cAgent = System.out;
        System.setOut(nullStream);

        logMessages += "reading config file, ";
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
        Path defaultStackedTestPath, defaultConfigPath, defaultTestPlanPath,
                stackedTestPath, testPlanPath;
        Path stackedFeedbackPath, testPlanFeedbackPath;
        StackedTestPacket defaultStackedTestPacket;
        StackedTestPacket stackedTestPacket;
        TestPlanPacket defaultTestPlanPacket;
        TestPlanPacket testPlanPacket;
        StackedFeedbackPacket stackedFeedbackPacket;
        TestPlanFeedbackPacket testPlanFeedbackPacket;
        String archive_name, fuzzing_archive_command;
        Long start_time_create_archive, start_time_t, startTimeReadTestPkt;

        logMessages += "Type " + testType + ", ";

        Path defaultTestPath = (testType == 0)
                ? Paths.get(workdir, "defaultStackedTestPacket.ser")
                : Paths.get(workdir, "defaultTestPlanPacket.ser");
        defaultConfigPath = (testType == 0)
                ? Paths.get(workdir, "stackedTestConfigFile")
                : Paths.get(workdir, "testPlanConfigFile");
        String executorStartUpReport = "";
        Executor executor;
        if (Config.getConf().verifyConfig) {
            System.err.println("verifying configuration");
            executor = FuzzingClient.initExecutor(
                    1, null, defaultConfigPath);
            boolean startUpStatus = executor.startup();

            if (!startUpStatus) {
                System.err.println("config cannot start up old version");
                executor.teardown();
                return;
            }
            startUpStatus = executor.freshStartNewVersion();
            executor.teardown();
            if (!startUpStatus) {
                System.err.println("config cannot start up new version");
                return;
            }
        }
        try { // if any of these catches go through we have a big problem
            if (testType == 0) {
                defaultStackedTestPacket = (StackedTestPacket) Utilities
                        .readObjectFromFile(defaultTestPath.toFile());
                executor = FuzzingClient.initExecutor(
                        defaultStackedTestPacket.nodeNum, null,
                        defaultConfigPath);
                executorStartUpReport = startUpExecutor(executor, testType);
            } else {
                defaultTestPlanPacket = (TestPlanPacket) Utilities
                        .readObjectFromFile(defaultTestPath.toFile());
                executor = FuzzingClient.initExecutor(
                        defaultTestPlanPacket.getNodeNum(), null,
                        defaultConfigPath);
                executorStartUpReport = startUpExecutor(executor, testType);
            }
        } catch (ClassNotFoundException | IOException
                | ClassCastException e) {
            e.printStackTrace();
            return;
        }

        if (executorStartUpReport.equals("fail0")) {
            // was unable to startup the docker system
            List<Integer> testIds = new ArrayList<>();
            testIds.add(-1);
            System.err.println(
                    "Nyx MiniClient: Executor failed to start up!");
            stackedFeedbackPacket = new StackedFeedbackPacket(
                    "/home/nyx/upfuzz/config.json", testIds);
            stackedFeedbackPath = Paths.get(workdir,
                    "stackedFeedbackPacket.ser"); // "/miniClientWorkdir/stackedFeedbackPacket.ser"
            try (DataOutputStream out = new DataOutputStream(
                    new FileOutputStream(
                            stackedFeedbackPath.toAbsolutePath()
                                    .toString()))) {
                String text = "-1";
                byte[] bytes = text.getBytes("UTF-8");
                out.write(bytes);
                stackedFeedbackPacket.write(out);
            } catch (IOException e) {
                e.printStackTrace(System.err);
                return;
            }
            cAgent.print("F0"); // F for failed
            return;
        } else if (executorStartUpReport.equals("fail4")) {
            // was unable to startup the docker system
            FeedBack[] testPlanFeedBacks = new FeedBack[1];
            System.err.println(
                    "Nyx MiniClient: Executor failed to start up!");
            testPlanFeedbackPacket = new TestPlanFeedbackPacket("",
                    "/home/nyx/upfuzz/config.json", 0, testPlanFeedBacks);
            testPlanFeedbackPath = Paths.get(workdir,
                    "testPlanFeedbackPacket.ser"); // "/miniClientWorkdir/testPlanFeedbackPacket.ser"
            try (DataOutputStream out = new DataOutputStream(
                    new FileOutputStream(
                            testPlanFeedbackPath.toAbsolutePath()
                                    .toString()))) {
                String text = "-1";
                byte[] bytes = text.getBytes("UTF-8");
                out.write(bytes);
                testPlanFeedbackPacket.write(out);
            } catch (IOException e) {
                e.printStackTrace(System.err);
                return;
            }
            cAgent.print("F4"); // F for failed
            return;
        } else if (executorStartUpReport.equals("success")) {
            logMessages += "Sending read signal, ";
            cAgent.print("R"); // READY_FOR_TESTS
        } else {
            cAgent.print("F0");
            return;
        }
        logMessages += executorStartUpReport + ", ";

        // c agent should checkpoint the vm here
        System.err.println("Waiting to start TESTING");
        String cAgentMsg = stdin.nextLine();

        // wait for c agent to tell us to start testing
        if (!(cAgentMsg.equals("START_TESTING0")
                || (cAgentMsg.equals("START_TESTING4")))) {
            // possible sync issue
            System.err.println("POSSIBLE SYNC ERROR OCCURED IN MINICLIENT");
            return;
        }

        // Read the new stackedTestPacket to be used for test case sending
        startTimeReadTestPkt = System.currentTimeMillis();
        if (cAgentMsg.equals("START_TESTING0")) {
            stackedTestPath = Paths.get(workdir,
                    "mainStackedTestPacket.ser"); // "/miniClientWorkdir/mainStackedTestPacket.ser"
            try { // if any of these catches go through we have a big problem
                stackedTestPacket = (StackedTestPacket) Utilities
                        .readObjectFromFile(stackedTestPath.toFile());
            } catch (ClassNotFoundException | IOException
                    | ClassCastException e) {
                e.printStackTrace();
                return;
            }
            logMessages += "read stacked test file in "
                    + (System.currentTimeMillis() - startTimeReadTestPkt)
                    + " ms, ";

            start_time_t = System.currentTimeMillis();
            stackedFeedbackPacket = runTheTests(executor, stackedTestPacket);
            logMessages += "Testing time "
                    + (System.currentTimeMillis() - start_time_t)
                    + "ms, ";
            System.err.println(
                    "[MiniClient] Completed running stacked test packet");

            start_time_create_archive = System.currentTimeMillis();
            stackedFeedbackPath = Paths.get(workdir,
                    "stackedFeedbackPacket.ser");
            try (DataOutputStream out = new DataOutputStream(
                    new FileOutputStream(
                            stackedFeedbackPath.toAbsolutePath().toString()))) {
                stackedFeedbackPacket.write(out);
            } catch (IOException e) {
                e.printStackTrace(System.err);
                return;
            }

            String storagePath = executor.dockerCluster.workdir
                    .getAbsolutePath()
                    .toString();
            int lastDashIndex = storagePath.lastIndexOf('-');
            String str1 = storagePath.substring(0, lastDashIndex);

            String str2 = storagePath.substring(lastDashIndex + 1,
                    lastDashIndex + 9); // Extract 8 characters after last dash

            String fuzzing_storage_dir = str1 + '-' + str2 + '/';
            archive_name = str2 + ".tar.gz";

            fuzzing_archive_command = "cp "
                    + stackedFeedbackPath.toAbsolutePath().toString() + " "
                    + fuzzing_storage_dir + "persistent/ ; "
                    + "cd " + fuzzing_storage_dir + " ; "
                    + "tar -czf " + archive_name + " persistent ; "
                    + "cp " + archive_name + " /miniClientWorkdir/ ;"
                    + "cd - ";
        } else {
            testPlanPath = Paths.get(workdir,
                    "mainTestPlanPacket.ser"); // "/miniClientWorkdir/mainStackedTestPacket.ser"
            try { // if any of these catches go through we have a big problem
                testPlanPacket = (TestPlanPacket) Utilities
                        .readObjectFromFile(testPlanPath.toFile());
            } catch (ClassNotFoundException | IOException
                    | ClassCastException e) {
                e.printStackTrace();
                return;
            }

            logMessages += "read stacked test file in "
                    + (System.currentTimeMillis() - startTimeReadTestPkt)
                    + " ms, ";

            start_time_t = System.currentTimeMillis();
            testPlanFeedbackPacket = runTestPlanPacket(executor,
                    testPlanPacket);
            logMessages += "Testing time "
                    + (System.currentTimeMillis() - start_time_t)
                    + "ms, ";

            start_time_create_archive = System.currentTimeMillis();
            testPlanFeedbackPath = Paths.get(workdir,
                    "testPlanFeedbackPacket.ser");

            try (DataOutputStream out = new DataOutputStream(
                    new FileOutputStream(
                            testPlanFeedbackPath.toAbsolutePath()
                                    .toString()))) {
                testPlanFeedbackPacket.write(out);
            } catch (IOException e) {
                e.printStackTrace(System.err);
                return;
            }

            String storagePath = executor.dockerCluster.workdir
                    .getAbsolutePath()
                    .toString();
            int lastDashIndex = storagePath.lastIndexOf('-');
            String str1 = storagePath.substring(0, lastDashIndex);

            String str2 = storagePath.substring(lastDashIndex + 1,
                    lastDashIndex + 9); // Extract 8 characters after last dash

            String fuzzing_storage_dir = str1 + '-' + str2 + '/';
            archive_name = str2 + ".tar.gz";

            fuzzing_archive_command = "cp "
                    + testPlanFeedbackPath.toAbsolutePath().toString() + " "
                    + fuzzing_storage_dir + "persistent/ ; "
                    + "cd " + fuzzing_storage_dir + " ; "
                    + "tar -czf " + archive_name + " persistent ; "
                    + "cp " + archive_name + " /miniClientWorkdir/ ;"
                    + "cd - ";

        }

        try {
            ProcessBuilder builder = new ProcessBuilder();
            builder.command("/bin/bash", "-c", fuzzing_archive_command);
            builder.redirectErrorStream(true);

            Process process = builder.start();
            int exitCode = process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
        logMessages += "Creating feedback archive "
                + (System.currentTimeMillis() - start_time_create_archive)
                + "ms, ";

        // lets c agent know that the stackedFeedbackFile is ready
        String printMsg;
        if (Config.getConf().debug) {
            printMsg = "2:" + archive_name + "; " + logMessages
                    + testExecutionLog;
        } else {
            printMsg = "2:" + archive_name;
        }
        cAgent.print(printMsg);

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

        for (TestPacket tp : stackedTestPacket.getTestPacketList()) {
            executedTestNum++;

            // if you want to run fixed command sequence, remove the comments
            // from the following lines
            // Moved the commented code to Utilities.createExampleCommands();
            executor.executeCommands(tp.originalCommandSequenceList);
            testExecutionLog += executor.getTestPlanExecutionLog();
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

            // List<String> oriResult =
            // executor.executeCommands(Arrays.asList(validationCommandsList));
            List<String> oriResult = executor
                    .executeCommands(tp.validationCommandSequenceList);
            testID2oriResults.put(tp.testPacketID, oriResult);

            if (Config.getConf().useFormatCoverage) {
                // logger.info("[HKLOG] format coverage checking");
                testID2FeedbackPacket
                        .get(tp.testPacketID).formatCoverage = executor
                                .getFormatCoverage();
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

        if (Config.getConf().testSingleVersion) {
            // return feedback packet
            if (Config.getConf().enableLogCheck
                    && FuzzingClient.hasERRORLOG(logInfoBeforeUpgrade)) {
                stackedFeedbackPacket.hasERRORLog = true;
                stackedFeedbackPacket.errorLogReport = FuzzingClient
                        .genErrorLogReport(
                                executor.executorID,
                                stackedTestPacket.configFileName,
                                logInfoBeforeUpgrade);
            }
            for (int testPacketIdx = 0; testPacketIdx < executedTestNum; testPacketIdx++) {
                TestPacket tp = stackedTestPacket.getTestPacketList()
                        .get(testPacketIdx);
                FeedbackPacket feedbackPacket = testID2FeedbackPacket
                        .get(tp.testPacketID);
                stackedFeedbackPacket.addFeedbackPacket(feedbackPacket);
            }
            return stackedFeedbackPacket;
        }

        boolean upgradeStatus = executor.fullStopUpgrade();

        if (!upgradeStatus) {
            // upgrade failed
            String upgradeFailureReport = FuzzingClient.genUpgradeFailureReport(
                    executor.executorID, stackedTestPacket.configFileName);
            stackedFeedbackPacket.isUpgradeProcessFailed = true;
            stackedFeedbackPacket.upgradeFailureReport = upgradeFailureReport;
        } else {
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
                Pair<Boolean, String> compareRes = executor
                        .checkResultConsistency(
                                testID2oriResults.get(tp.testPacketID),
                                testID2upResults.get(tp.testPacketID), true);
                // Update FeedbackPacket
                FeedbackPacket feedbackPacket = testID2FeedbackPacket
                        .get(tp.testPacketID);
                if (!compareRes.left) {
                    String failureReport = FuzzingClient.genInconsistencyReport(
                            executor.executorID,
                            stackedTestPacket.configFileName,
                            compareRes.right,
                            FuzzingClient.recordSingleTestPacket(tp));
                    feedbackPacket.isInconsistent = true;
                    feedbackPacket.inconsistencyReport = failureReport;
                }
                feedbackPacket.validationReadResults = testID2upResults
                        .get(tp.testPacketID);
            }
        }

        // update stackedFeedbackPacket
        for (int testPacketIdx = 0; testPacketIdx < executedTestNum; testPacketIdx++) {
            TestPacket tp = stackedTestPacket.getTestPacketList()
                    .get(testPacketIdx);
            FeedbackPacket feedbackPacket = testID2FeedbackPacket
                    .get(tp.testPacketID);
            stackedFeedbackPacket.addFeedbackPacket(feedbackPacket);
        }

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
            Map<Integer, LogInfo> logInfo = FuzzingClient
                    .extractErrorLog(executor, logInfoBeforeUpgrade);
            if (FuzzingClient.hasERRORLOG(logInfo)) {
                stackedFeedbackPacket.hasERRORLog = true;
                stackedFeedbackPacket.errorLogReport = FuzzingClient
                        .genErrorLogReport(
                                executor.executorID,
                                stackedTestPacket.configFileName,
                                logInfo);
            }
        }
        // teardown is teardown in a higher loop
        return stackedFeedbackPacket;
    }

    public static TestPlanFeedbackPacket runTestPlanPacket(Executor executor,
            TestPlanPacket testPlanPacket) {

        Long initTime = System.currentTimeMillis();
        String testPlanPacketStr = String.format("nodeNum = %d\n",
                testPlanPacket.getNodeNum())
                + testPlanPacket.getTestPlan().toString();
        ;
        int nodeNum = testPlanPacket.getNodeNum();

        // LOG checking1
        Map<Integer, LogInfo> logInfoBeforeUpgrade = null;
        if (Config.getConf().enableLogCheck) {
            logInfoBeforeUpgrade = executor.grepLogInfo();
        }

        // execute test plan (rolling upgrade + fault)

        boolean status = executor.execute(testPlanPacket.getTestPlan());
        testExecutionLog += "testing plan done in "
                + (System.currentTimeMillis() - initTime) + " ms, status "
                + status + ", ";
        testExecutionLog += executor.getTestPlanExecutionLog();

        initTime = System.currentTimeMillis();
        FeedBack[] testPlanFeedBacks = new FeedBack[nodeNum];

        if (status && Config.getConf().fullStopUpgradeWithFaults) {
            // collect old version coverage
            ExecutionDataStore[] oriCoverages = executor
                    .collectCoverageSeparate("original");
            for (int i = 0; i < nodeNum; i++) {
                testPlanFeedBacks[i] = new FeedBack();
                if (oriCoverages != null)
                    testPlanFeedBacks[i].originalCodeCoverage = oriCoverages[i];
            }
            testExecutionLog += "(fullstop) coverage collected and processed in "
                    + (System.currentTimeMillis() - initTime) + " ms, ";
            // upgrade

            initTime = System.currentTimeMillis();
            status = executor.fullStopUpgrade();
            if (!status)
                // update event id
                executor.eventIdx = -1; // this means full-stop upgrade failed

            testExecutionLog += "fullstop upgrade done in "
                    + (System.currentTimeMillis() - initTime) + " ms, ";

        } else {
            // It contains the new version coverage!
            // collect test plan coverage
            for (int i = 0; i < nodeNum; i++) {
                testPlanFeedBacks[i] = new FeedBack();
                if (executor.oriCoverage[i] != null)
                    testPlanFeedBacks[i].originalCodeCoverage = executor.oriCoverage[i];
            }
            testExecutionLog += "fullstop upgrade done in "
                    + (System.currentTimeMillis() - initTime) + " ms, ";
        }

        initTime = System.currentTimeMillis();
        TestPlanFeedbackPacket testPlanFeedbackPacket = new TestPlanFeedbackPacket(
                testPlanPacket.systemID, testPlanPacket.configFileName,
                testPlanPacket.testPacketID, testPlanFeedBacks);
        testPlanFeedbackPacket.fullSequence = testPlanPacketStr;

        // System state comparison
        // if (Config.getConf().enableStateComp) {
        // Map<Integer, Map<String, String>> states = executor
        // .readSystemState();
        // // + testPlanPacket.testPlan.targetSystemStatesOracle);
        // Map<Integer, Map<String, Pair<String, String>>> inconsistentStates =
        // stateCompare(
        // testPlanPacket.testPlan.targetSystemStatesOracle,
        // states);
        // }

        if (!status) {
            testPlanFeedbackPacket.isEventFailed = true;

            testPlanFeedbackPacket.eventFailedReport = "[Test plan execution failed at event"
                    + executor.eventIdx + "]\n" +
                    "executionId = " + executor.executorID + "\n" +
                    "ConfigIdx = " + testPlanPacket.configFileName + "\n" +
                    testPlanPacketStr + "\n";
            testPlanFeedbackPacket.isInconsistent = false;
            testPlanFeedbackPacket.inconsistencyReport = "";
        } else {
            // Test single version
            if (Config.getConf().testSingleVersion) {
                try {
                    ExecutionDataStore[] oriCoverages = executor
                            .collectCoverageSeparate("original");
                    testExecutionLog += "(single) collected coverage, ";
                    if (oriCoverages != null) {
                        for (int nodeIdx = 0; nodeIdx < nodeNum; nodeIdx++) {
                            testPlanFeedbackPacket.feedBacks[nodeIdx].originalCodeCoverage = oriCoverages[nodeIdx];
                        }
                    }
                    testExecutionLog += "(single success) coverage collected and processed in "
                            + (System.currentTimeMillis() - initTime) + " ms, ";
                } catch (Exception e) {
                    // Cannot collect code coverage in the upgraded version
                    String recordedTestPlanPacket = String.format(
                            "nodeNum = %d\n", testPlanPacket.getNodeNum())
                            + testPlanPacket.getTestPlan().toString();
                    testPlanFeedbackPacket.isEventFailed = true;
                    testPlanFeedbackPacket.eventFailedReport = "[Original Coverage Collect Failed]\n"
                            +
                            "executionId = " + executor.executorID + "\n" +
                            "ConfigIdx = " + testPlanPacket.configFileName
                            + "\n" +
                            recordedTestPlanPacket + "\n" + "Exception:"
                            + e;

                    testExecutionLog += "(failed) single version coverage collection in "
                            + (System.currentTimeMillis() - initTime) + " ms, ";
                    return testPlanFeedbackPacket;
                }

            } else {
                Pair<Boolean, String> compareRes;
                // read comparison between full-stop and rolling
                if (!testPlanPacket.testPlan.validationReadResultsOracle
                        .isEmpty()) {

                    List<String> testPlanReadResults = executor
                            .executeCommands(
                                    testPlanPacket.testPlan.validationCommands);
                    compareRes = executor
                            .checkResultConsistency(
                                    testPlanPacket.testPlan.validationReadResultsOracle,
                                    testPlanReadResults, false);
                    if (!compareRes.left) {
                        testPlanFeedbackPacket.isInconsistent = true;
                        testPlanFeedbackPacket.inconsistencyReport = "[Results inconsistency between full-stop and rolling upgrade]\n"
                                + "executionId = " + executor.executorID + "\n"
                                +
                                "ConfigIdx = " + testPlanPacket.configFileName
                                + "\n" +
                                compareRes.right + "\n" +
                                testPlanPacketStr + "\n";
                    }
                } else {
                    // logger.debug("validationReadResultsOracle is empty!");
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
                    String recordedTestPlanPacket = String.format(
                            "nodeNum = %d\n", testPlanPacket.getNodeNum())
                            + testPlanPacket.getTestPlan().toString();
                    testPlanFeedbackPacket.isEventFailed = true;
                    testPlanFeedbackPacket.eventFailedReport = "[Upgrade Coverage Collect Failed]\n"
                            +
                            "executionId = " + executor.executorID + "\n" +
                            "ConfigIdx = " + testPlanPacket.configFileName
                            + "\n" +
                            recordedTestPlanPacket + "\n" + "Exception:" + e;
                    return testPlanFeedbackPacket;
                }
            }
        }

        // LOG checking2
        initTime = System.currentTimeMillis();
        if (Config.getConf().enableLogCheck) {
            assert logInfoBeforeUpgrade != null;
            Map<Integer, LogInfo> logInfo = FuzzingClient
                    .extractErrorLog(executor, logInfoBeforeUpgrade);
            if (FuzzingClient.hasERRORLOG(logInfo)) {
                testPlanFeedbackPacket.hasERRORLog = true;
                testPlanFeedbackPacket.errorLogReport = FuzzingClient
                        .genErrorLogReport(
                                executor.executorID,
                                testPlanPacket.configFileName,
                                logInfo);
            }
            testExecutionLog += "(log check) fuzzing client log check in "
                    + (System.currentTimeMillis() - initTime) + " ms, ";
        }
        return testPlanFeedbackPacket;
    }
}
