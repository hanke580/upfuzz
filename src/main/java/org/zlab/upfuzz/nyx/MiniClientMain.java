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
import org.zlab.upfuzz.ozone.OzoneExecutor;
import org.zlab.upfuzz.utils.Pair;
import org.zlab.upfuzz.utils.Utilities;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

public class MiniClientMain {
    // WARNING: This must be disabled otherwise it can
    // log to output and corrupt the process
    // INFO => cClient output
    // FIXME: disable this if you want NYX to work
    static org.apache.logging.log4j.Logger logger = LogManager
            .getLogger(MiniClientMain.class);

    // Where all files are searched for
    static final String workdir = "/miniClientWorkdir";

    // If the cluster startup fails 3 times, then give up
    static final int CLUSTER_START_RETRY = 3;

    static int testType;
    static String testExecutionLog = "";

    static Utilities.ExponentialProbabilityModel model = new Utilities.ExponentialProbabilityModel(
            Config.getConf().expProbModel_C,
            Config.getConf().skipUpgradeTargetProb,
            Config.getConf().skipUpgradeTargetProbN);

    public static boolean skipUpgradeBasedOnMutationDepth(int mutationDepth) {
        return Utilities.rand.nextDouble() < model
                .calculateProbability(mutationDepth);
    }

    public static void setTestType(int type) {
        testExecutionLog += "invoked set test type: " + type;
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
            public void write(int b) {
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
        long start_time_create_archive, start_time_t, startTimeReadTestPkt;

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
            if (!Config.getConf().useVersionDelta) {
                executor = FuzzingClient.initExecutor(
                        1, Config.getConf().useFormatCoverage,
                        defaultConfigPath);
            } else {
                // FIXME: Why in nyx, direction is always 0?
                executor = FuzzingClient.initExecutor(
                        1, Config.getConf().useFormatCoverage,
                        defaultConfigPath, 0);
            }
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
                if (!Config.getConf().useVersionDelta) {
                    executor = FuzzingClient.initExecutor(
                            defaultStackedTestPacket.nodeNum,
                            Config.getConf().useFormatCoverage,
                            defaultConfigPath, 0);
                } else {
                    executor = FuzzingClient.initExecutor(
                            defaultStackedTestPacket.nodeNum,
                            Config.getConf().useFormatCoverage,
                            defaultConfigPath,
                            defaultStackedTestPacket.testDirection);
                }
                executorStartUpReport = startUpExecutor(executor, testType);
            } else {
                defaultTestPlanPacket = (TestPlanPacket) Utilities
                        .readObjectFromFile(defaultTestPath.toFile());
                if (!Config.getConf().useVersionDelta) {
                    executor = FuzzingClient.initExecutor(
                            defaultTestPlanPacket.getNodeNum(),
                            Config.getConf().useFormatCoverage,
                            defaultConfigPath);
                } else {
                    executor = FuzzingClient.initExecutor(
                            defaultTestPlanPacket.getNodeNum(),
                            Config.getConf().useFormatCoverage,
                            defaultConfigPath);
                }
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
            if (!Config.getConf().useVersionDelta) {
                stackedFeedbackPacket = runTheTests(executor, stackedTestPacket,
                        null);
            } else {
                logMessages += "direction " + stackedTestPacket.testDirection
                        + ", ";
                logMessages += "group "
                        + stackedTestPacket.clientGroupForVersionDelta + ", ";
                if (stackedTestPacket.clientGroupForVersionDelta == 1) {
                    stackedFeedbackPacket = runTheTestsBeforeChangingVersion(
                            executor, stackedTestPacket,
                            stackedTestPacket.testDirection);
                } else {
                    StackedFeedbackPacket stackedFeedbackPacketBeforeVersionChange = runTheTestsBeforeChangingVersion(
                            executor, stackedTestPacket,
                            stackedTestPacket.testDirection);
                    stackedFeedbackPacket = changeVersionAndRunTheTests(
                            executor, stackedTestPacket,
                            stackedTestPacket.testDirection,
                            stackedTestPacket.isDowngradeSupported,
                            stackedFeedbackPacketBeforeVersionChange);
                }
            }
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
                    + stackedFeedbackPath.toAbsolutePath() + " "
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
                    + testPlanFeedbackPath.toAbsolutePath() + " "
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

    public static StackedFeedbackPacket runTheTestsBeforeChangingVersion(
            Executor executor,
            StackedTestPacket stackedTestPacket, int direction) {
        // If the middle of test has already broken an invariant
        // we stop executing.
        int executedTestNum = 0;
        boolean breakNewInv = false;
        Map<Integer, FeedbackPacket> testID2FeedbackPacket = new HashMap<>();
        Map<Integer, LogInfo> logInfoBeforeVersionChange = new HashMap<>();
        Map<Integer, List<String>> testID2oriResults = new HashMap<>();

        for (TestPacket tp : stackedTestPacket.getTestPacketList()) {
            executedTestNum++;

            // if you want to run fixed command sequence, remove the comments
            // from the following lines
            // Moved the commented code to Utilities.createExampleCommands();
            executor.executeCommands(tp.originalCommandSequenceList);

            FeedBack[] feedBacks = new FeedBack[stackedTestPacket.nodeNum];
            for (int i = 0; i < stackedTestPacket.nodeNum; i++) {
                feedBacks[i] = new FeedBack();
            }
            // logger.info("[HKLOG] Got direction in miniclient: " + direction);
            ExecutionDataStore[] oriCoverages = (direction == 0) ? executor
                    .collectCoverageSeparate("original")
                    : executor
                            .collectCoverageSeparate("upgraded");

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

            if (Config.getConf().useFormatCoverage) {
                if (stackedTestPacket.clientGroupForVersionDelta != 2) {
                    // logger.info("[HKLOG] format coverage checking");
                    Path formatInfoFolder;
                    Path oriFormatInfoFolder = Paths.get("configInfo")
                            .resolve(Config.getConf().originalVersion);
                    Path upFormatInfoFolder = Paths.get("configInfo")
                            .resolve(Config.getConf().upgradedVersion);
                    if (!oriFormatInfoFolder.toFile().exists()
                            || !upFormatInfoFolder
                                    .toFile().exists()) {
                        throw new RuntimeException(
                                "Format info folder does not exist");
                    }
                    if (direction == 0)
                        formatInfoFolder = oriFormatInfoFolder;
                    else
                        formatInfoFolder = upFormatInfoFolder;
                    testID2FeedbackPacket
                            .get(tp.testPacketID).formatCoverage = executor
                                    .getFormatCoverage(formatInfoFolder);
                }
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
        if (Config.getConf().enableLogCheck) {
            // logger.info("[HKLOG] error log checking");
            logInfoBeforeVersionChange = executor.grepLogInfo();
        }
        if (Config.getConf().enableLogCheck
                && FuzzingClient.hasERRORLOG(logInfoBeforeVersionChange)) {
            stackedFeedbackPacket.hasERRORLog = true;
            stackedFeedbackPacket.errorLogReport = FuzzingClient
                    .genErrorLogReport(
                            executor.executorID,
                            stackedTestPacket.configFileName,
                            logInfoBeforeVersionChange);
        }

        for (int testPacketIdx = 0; testPacketIdx < executedTestNum; testPacketIdx++) {
            TestPacket tp = stackedTestPacket.getTestPacketList()
                    .get(testPacketIdx);
            FeedbackPacket feedbackPacket = testID2FeedbackPacket
                    .get(tp.testPacketID);
            List<String> oriResult = testID2oriResults.get(tp.testPacketID);
            LogInfo logInfo = logInfoBeforeVersionChange.get(tp.testPacketID);
            stackedFeedbackPacket.addFeedbackPacket(feedbackPacket);
            stackedFeedbackPacket.oriResults.add(oriResult);
            stackedFeedbackPacket.logInfos.add(logInfo);
        }
        stackedFeedbackPacket.setVersion(executor.dockerCluster.version);
        testExecutionLog += "Completed all testing, ";
        return stackedFeedbackPacket;
    }

    public static StackedFeedbackPacket changeVersionAndRunTheTests(
            Executor executor,
            StackedTestPacket stackedTestPacket, int direction,
            boolean isDowngradeSupported,
            StackedFeedbackPacket stackedFeedbackPacket) {

        // if the middle of test has already broken an invariant
        // we stop executing.
        Map<Integer, List<String>> testID2modifiedVersionResults = new HashMap<>();
        boolean upgradeStatus = false;
        boolean downgradeStatus = false;
        Map<Integer, FeedbackPacket> testID2FeedbackPacket = new HashMap<>();
        Map<Integer, List<String>> testID2oriResults = new HashMap<>();
        Map<Integer, List<String>> testID2upResults = new HashMap<>();
        Map<Integer, LogInfo> logInfoBeforeVersionChange = new HashMap<>();

        int i = 0;
        for (int id : stackedFeedbackPacket.testIDs) {
            testID2FeedbackPacket.put(id,
                    stackedFeedbackPacket.getFpList().get(i));
            testID2oriResults.put(id,
                    stackedFeedbackPacket.getOriResults().get(i));
            i += 1;
        }

        int executedTestNum = stackedTestPacket.getTestPacketList().size();
        if (direction == 0)
            upgradeStatus = executor.fullStopUpgrade();
        else {
            if (isDowngradeSupported) {
                downgradeStatus = executor.downgrade();
            } else {
                downgradeStatus = true;
            }
        }

        if ((direction == 0) && (!upgradeStatus)) {
            // upgrade failed
            String upgradeFailureReport = FuzzingClient.genUpgradeFailureReport(
                    executor.executorID, stackedTestPacket.configFileName);
            stackedFeedbackPacket.isUpgradeProcessFailed = true;
            stackedFeedbackPacket.upgradeFailureReport = upgradeFailureReport;
        } else if ((direction == 1) && (!downgradeStatus)) {
            // downgrade failed
            String downgradeFailureReport = FuzzingClient
                    .genDowngradeFailureReport(
                            executor.executorID,
                            stackedTestPacket.configFileName);
            stackedFeedbackPacket.isDowngradeProcessFailed = true;
            stackedFeedbackPacket.downgradeFailureReport = downgradeFailureReport;
        } else if ((direction == 0) && (upgradeStatus)) {
            // logger.info("upgrade succeed");
            stackedFeedbackPacket.isUpgradeProcessFailed = false;
            for (int testPacketIdx = 0; testPacketIdx < executedTestNum; testPacketIdx++) {
                TestPacket tp = stackedTestPacket.getTestPacketList()
                        .get(testPacketIdx);
                List<String> upResult = executor
                        .executeCommands(tp.validationCommandSequenceList);
                testID2modifiedVersionResults.put(tp.testPacketID, upResult);
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
                                testID2modifiedVersionResults
                                        .get(tp.testPacketID),
                                true);
                // Update FeedbackPacket
                // logger.info("[HKLOG: miniclient] Inconsistency checked");
                FeedbackPacket feedbackPacket = testID2FeedbackPacket
                        .get(tp.testPacketID);
                if (!compareRes.left) {
                    String failureReport = FuzzingClient.genInconsistencyReport(
                            executor.executorID,
                            stackedTestPacket.configFileName,
                            compareRes.right,
                            FuzzingClient.recordSingleTestPacket(tp));
                    feedbackPacket.isInconsistent = true;
                    // logger.info("Inconsistency: " + compareRes.right);
                    if (compareRes.right
                            .contains("Insignificant Result inconsistency")) {
                        // logger.info(
                        // "YES! Insignificant Result inconsistency at: "
                        // + tp.testPacketID);
                        feedbackPacket.isInconsistencyInsignificant = true;
                    }
                    feedbackPacket.inconsistencyReport = failureReport;
                }
                feedbackPacket.validationReadResults = testID2upResults
                        .get(tp.testPacketID);
            }
        } else if ((direction == 1) && (downgradeStatus)) {
            // logger.info("upgrade succeed");
            if (isDowngradeSupported) {
                stackedFeedbackPacket.isDowngradeProcessFailed = false;
                for (int testPacketIdx = 0; testPacketIdx < executedTestNum; testPacketIdx++) {
                    TestPacket tp = stackedTestPacket.getTestPacketList()
                            .get(testPacketIdx);
                    List<String> downResult = executor
                            .executeCommands(tp.validationCommandSequenceList);
                    testID2modifiedVersionResults.put(tp.testPacketID,
                            downResult);
                    if (Config.getConf().collDownFeedBack) {
                        ExecutionDataStore[] downCoverages = executor
                                .collectCoverageSeparate("original");
                        if (downCoverages != null) {
                            for (int nodeIdx = 0; nodeIdx < stackedTestPacket.nodeNum; nodeIdx++) {
                                testID2FeedbackPacket.get(
                                        tp.testPacketID).feedBacks[nodeIdx].downgradedCodeCoverage = downCoverages[nodeIdx];
                            }
                        }
                    }
                    Pair<Boolean, String> compareRes = executor
                            .checkResultConsistency(
                                    testID2oriResults.get(tp.testPacketID),
                                    testID2modifiedVersionResults
                                            .get(tp.testPacketID),
                                    true);
                    // Update FeedbackPacket
                    FeedbackPacket feedbackPacket = testID2FeedbackPacket
                            .get(tp.testPacketID);
                    if (!compareRes.left) {
                        String failureReport = FuzzingClient
                                .genInconsistencyReport(
                                        executor.executorID,
                                        stackedTestPacket.configFileName,
                                        compareRes.right,
                                        FuzzingClient
                                                .recordSingleTestPacket(tp));
                        feedbackPacket.isInconsistent = true;
                        feedbackPacket.inconsistencyReport = failureReport;
                    }
                    feedbackPacket.validationReadResults = testID2upResults
                            .get(tp.testPacketID);
                }
            } else {
                stackedFeedbackPacket.isDowngradeProcessFailed = true;
                return stackedFeedbackPacket;
            }
        }

        // update stackedFeedbackPacket
        for (int testPacketIdx = 0; testPacketIdx < executedTestNum; testPacketIdx++) {
            TestPacket tp = stackedTestPacket.getTestPacketList()
                    .get(testPacketIdx);
            FeedbackPacket feedbackPacket = testID2FeedbackPacket
                    .get(tp.testPacketID);
            stackedFeedbackPacket.updateFeedbackPacket(testPacketIdx,
                    feedbackPacket);
        }

        // test downgrade
        if (Config.getConf().testDowngrade) {
            // logger.info("downgrade cluster");
            if (isDowngradeSupported) {
                downgradeStatus = executor.downgrade();
                if (!downgradeStatus) {
                    // downgrade failed
                    stackedFeedbackPacket.isDowngradeProcessFailed = true;
                    stackedFeedbackPacket.downgradeFailureReport = FuzzingClient
                            .genDowngradeFailureReport(
                                    executor.executorID,
                                    stackedFeedbackPacket.configFileName);
                }
            }
        }

        // LOG checking2
        if (Config.getConf().enableLogCheck) {
            Map<Integer, LogInfo> logInfo = FuzzingClient
                    .extractErrorLog(executor, logInfoBeforeVersionChange);
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

    public static StackedFeedbackPacket runTheTests(Executor executor,
            StackedTestPacket stackedTestPacket,
            ObjectGraphCoverage accumOriObjCoverage) {
        Map<Integer, FeedbackPacket> testID2FeedbackPacket = new HashMap<>();
        Map<Integer, List<String>> testID2oriResults = new HashMap<>();
        Map<Integer, List<String>> testID2upResults = new HashMap<>();
        Map<Integer, List<String>> testID2downResults = new HashMap<>();
        List<String> ozoneDefaultFSs = new ArrayList<>();

        int executedTestNum = 0;
        boolean breakNewInv = false;

        for (TestPacket tp : stackedTestPacket.getTestPacketList()) {
            executedTestNum++;
            if (Config.getConf().system.equals("ozone")) {
                runOzoneWriteCommands(tp, executor, ozoneDefaultFSs);
            } else {
                executor.executeCommands(tp.originalCommandSequenceList);
            }
            if (Config.getConf().flushAfterTest)
                executor.flush();

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

            List<String> oriResult = executor
                    .executeCommands(tp.validationCommandSequenceList);
            testID2oriResults.put(tp.testPacketID, oriResult);

            if (Config.getConf().useFormatCoverage) {
                Path oriFormatInfoFolder = Paths.get("configInfo")
                        .resolve(Config.getConf().originalVersion);
                testID2FeedbackPacket
                        .get(tp.testPacketID).formatCoverage = executor
                                .getFormatCoverage(oriFormatInfoFolder);
            }
        }

        StackedFeedbackPacket stackedFeedbackPacket = new StackedFeedbackPacket(
                stackedTestPacket.configFileName,
                Utilities.extractTestIDs(stackedTestPacket));
        stackedFeedbackPacket.fullSequence = FuzzingClient
                .recordStackedTestPacket(
                        stackedTestPacket);
        stackedFeedbackPacket.breakNewInv = breakNewInv;

        Map<Integer, LogInfo> logInfoBeforeUpgrade = null;
        if (Config.getConf().enableLogCheck) {
            logInfoBeforeUpgrade = executor.grepLogInfo();
        }

        if (Config.getConf().testSingleVersion) {
            // Handle single version
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

        // Skip Upgrade Check
        boolean skipUpgrade = false;
        if (Config.getConf().useFormatCoverage
                && Config.getConf().skipUpgrade) {
            // new format: not skip
            // no new format: skip based on prob related to mutation depth
            assert Config.getConf().STACKED_TESTS_NUM == 1
                    : "Only skip upgrade when there is batch size is 1";
            assert stackedTestPacket.getTestPacketList().size() == 1
                    : "Only skip upgrade when there is batch size is 1";
            assert accumOriObjCoverage != null;
            stackedTestPacket.formatCoverage.copyBasicInfo(accumOriObjCoverage);

            boolean newFormatCoverage = false;
            FeedbackPacket feedbackPacket = testID2FeedbackPacket
                    .get(stackedTestPacket.getTestPacketList()
                            .get(0).testPacketID);
            if (stackedTestPacket.formatCoverage
                    .merge(feedbackPacket.formatCoverage,
                            "ori",
                            feedbackPacket.testPacketID,
                            true,
                            Config.getConf().updateInvariantBrokenFrequency,
                            Config.getConf().checkSpecialDumpIds)
                    .isNewFormat()) {
                newFormatCoverage = true;
            }

            if (!newFormatCoverage) {
                boolean newBranchCoverage = false;
                for (int nodeIdx = 0; nodeIdx < stackedTestPacket.nodeNum; nodeIdx++) {
                    if (Utilities.hasNewBits(stackedTestPacket.branchCoverage,
                            feedbackPacket.feedBacks[nodeIdx].originalCodeCoverage)) {
                        newBranchCoverage = true;
                        break;
                    }
                }
                if (newBranchCoverage) {
                    if (Utilities.rand.nextDouble() < Config
                            .getConf().skipProbForNewBranchCoverage)
                        skipUpgrade = true;
                } else {
                    if (skipUpgradeBasedOnMutationDepth(
                            stackedTestPacket.getTestPacketList()
                                    .get(0).mutationDepth)) {
                        skipUpgrade = true;
                    }
                }
                logger.debug(
                        "[Skip Upgrade] skip = " + skipUpgrade + ", " +
                                "no new format, " +
                                "newBranchCoverage = " + newBranchCoverage
                                + ", " +
                                "mutation depth = "
                                + stackedTestPacket.getTestPacketList()
                                        .get(0).mutationDepth);
            }
        }

        logger.debug("[Skip Upgrade] " + skipUpgrade);

        if (skipUpgrade) {
            stackedFeedbackPacket.upgradeSkipped = true;
        } else {
            testExecutionLog += "upgraded, ";
            boolean upgradeStatus = executor.fullStopUpgrade();
            if (!upgradeStatus) {
                String upgradeFailureReport = FuzzingClient
                        .genUpgradeFailureReport(
                                executor.executorID,
                                stackedTestPacket.configFileName);
                stackedFeedbackPacket.isUpgradeProcessFailed = true;
                stackedFeedbackPacket.upgradeFailureReport = upgradeFailureReport;
            } else {
                stackedFeedbackPacket.isUpgradeProcessFailed = false;
                for (int testPacketIdx = 0; testPacketIdx < executedTestNum; testPacketIdx++) {
                    TestPacket tp = stackedTestPacket.getTestPacketList()
                            .get(testPacketIdx);
                    List<String> upResult = new ArrayList<>();
                    if (Config.getConf().system.equals("ozone")) {
                        upResult = runOzoneReadCommandsAfterUpgrade(
                                testPacketIdx, tp,
                                executor, ozoneDefaultFSs);
                    } else {
                        upResult = executor
                                .executeCommands(
                                        tp.validationCommandSequenceList);
                    }
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
                                    testID2upResults.get(tp.testPacketID),
                                    true);
                    // Update FeedbackPacket
                    FeedbackPacket feedbackPacket = testID2FeedbackPacket
                            .get(tp.testPacketID);
                    if (!compareRes.left) {
                        String failureReport = FuzzingClient
                                .genInconsistencyReport(
                                        executor.executorID,
                                        stackedTestPacket.configFileName,
                                        compareRes.right,
                                        FuzzingClient
                                                .recordSingleTestPacket(tp));
                        feedbackPacket.isInconsistent = true;
                        feedbackPacket.inconsistencyReport = failureReport;
                    }
                    feedbackPacket.validationReadResults = testID2upResults
                            .get(tp.testPacketID);
                }

                // Downgrade
                if (Config.getConf().testDowngrade) {
                    logger.debug("[hklog] test downgrade");
                    boolean downgradeStatus = executor.downgrade();
                    if (!downgradeStatus) {
                        stackedFeedbackPacket.isDowngradeProcessFailed = true;
                        stackedFeedbackPacket.downgradeFailureReport = FuzzingClient
                                .genDowngradeFailureReport(
                                        executor.executorID,
                                        stackedTestPacket.configFileName);
                    }
                }
            }
        }

        for (int testPacketIdx = 0; testPacketIdx < executedTestNum; testPacketIdx++) {
            TestPacket tp = stackedTestPacket.getTestPacketList()
                    .get(testPacketIdx);
            FeedbackPacket feedbackPacket = testID2FeedbackPacket
                    .get(tp.testPacketID);
            stackedFeedbackPacket.addFeedbackPacket(feedbackPacket);
        }

        // LOG checking2
        if (Config.getConf().enableLogCheck) {
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
        if (Config.getConf().system.equals("ozone"))
            ozoneDefaultFSs.clear();
        return stackedFeedbackPacket;
    }

    public static void runOzoneWriteCommands(TestPacket tp, Executor executor,
            List<String> ozoneDefaultFSs) {
        if (!Config.getConf().ozoneAppendSpecialCommand) {
            executor.executeCommands(tp.originalCommandSequenceList);
            return;
        }
        List<String> originalCommandSequenceListCombined = tp.originalCommandSequenceList;
        List<String> initCommands = new ArrayList<>();
        String initBucketCommand = originalCommandSequenceListCombined
                .get(0);
        try {
            String[] initBucketCommandParams = initBucketCommand.split(" ");
            String volumeNameParam = initBucketCommandParams[initBucketCommandParams.length
                    - 1].split("/")[0];
            String bucketNameParam = initBucketCommandParams[initBucketCommandParams.length
                    - 1].split("/")[1];
            String initVolumeCommand = "sh volume create "
                    + volumeNameParam;
            initCommands.add(initVolumeCommand);
            initCommands.add(initBucketCommand);
            List<String> laterWriteCommands = new ArrayList<>(
                    originalCommandSequenceListCombined.subList(1,
                            originalCommandSequenceListCombined.size()));
            executor.executeCommands(initCommands);

            String fsPath = "o3fs://" + bucketNameParam + "."
                    + volumeNameParam;

            // Make sure that interferences are avoided:
            // Each set of write commands work on different defaultFS
            ((OzoneExecutor) executor).setDefaultFs(fsPath, 0);
            ozoneDefaultFSs.add(fsPath);

            executor.executeCommands(laterWriteCommands);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static List<String> runOzoneReadCommandsAfterUpgrade(
            int testPacketIdx, TestPacket tp,
            Executor executor, List<String> ozoneDefaultFSs) {
        if (!Config.getConf().ozoneAppendSpecialCommand) {
            List<String> upResult = executor
                    .executeCommands(tp.validationCommandSequenceList);
            return upResult;
        }
        try {
            // Have to use the same defaultFS as the corresponding set of write
            // commands
            String fsPath = ozoneDefaultFSs.get(testPacketIdx);
            ((OzoneExecutor) executor).setDefaultFs(fsPath, 1);

            List<String> upResult = executor
                    .executeCommands(tp.validationCommandSequenceList);
            return upResult;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static TestPlanFeedbackPacket runTestPlanPacket(Executor executor,
            TestPlanPacket testPlanPacket) {

        long initTime = System.currentTimeMillis();
        String testPlanPacketStr = String.format("nodeNum = %d\n",
                testPlanPacket.getNodeNum())
                + testPlanPacket.getTestPlan().toString();
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
                if (!testPlanPacket.getTestPlan().validationReadResultsOracle
                        .isEmpty()) {

                    List<String> testPlanReadResults = executor
                            .executeCommands(
                                    testPlanPacket
                                            .getTestPlan().validationCommands);
                    compareRes = executor
                            .checkResultConsistency(
                                    testPlanPacket
                                            .getTestPlan().validationReadResultsOracle,
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
