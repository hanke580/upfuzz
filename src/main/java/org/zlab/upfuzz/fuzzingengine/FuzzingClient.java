package org.zlab.upfuzz.fuzzingengine;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jacoco.core.data.ExecutionDataStore;
import org.zlab.upfuzz.cassandra.CassandraExecutor;
import org.zlab.upfuzz.fuzzingengine.packet.*;
import org.zlab.upfuzz.fuzzingengine.packet.Packet.PacketType;
import org.zlab.upfuzz.fuzzingengine.executor.Executor;
import org.zlab.upfuzz.fuzzingengine.configgen.ConfigGen;
import org.zlab.upfuzz.hdfs.HdfsExecutor;
import org.zlab.upfuzz.hbase.HBaseExecutor;
import org.zlab.upfuzz.utils.Pair;
import org.zlab.upfuzz.utils.Utilities;
import org.zlab.upfuzz.nyx.LibnyxInterface;

import static org.zlab.upfuzz.fuzzingengine.server.FuzzingServer.readState;
import static org.zlab.upfuzz.nyx.MiniClientMain.runTheTests;

public class FuzzingClient {
    static Logger logger = LogManager.getLogger(FuzzingClient.class);

    public Executor executor;
    public Path configDirPath;
    private LibnyxInterface libnyx = null;

    // If the cluster cannot start up for 3 times, it's serious
    int CLUSTER_START_RETRY = 3; // stop retry for now

    FuzzingClient() {
        if (Config.getConf().testSingleVersion) {
            configDirPath = Paths.get(System.getProperty("user.dir"),
                    Config.getConf().configDir,
                    Config.getConf().originalVersion);
        } else {
            configDirPath = Paths.get(System.getProperty("user.dir"),
                    Config.getConf().configDir, Config.getConf().originalVersion
                            + "_" + Config.getConf().upgradedVersion);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            executor.teardown();
            executor.upgradeTeardown();
        }));
        if (Config.getConf().nyxMode) {
            this.libnyx = new LibnyxInterface(
                    Paths.get("/tmp", RandomStringUtils.randomAlphanumeric(8))
                            .toAbsolutePath().toString(),
                    Paths.get("/tmp", RandomStringUtils.randomAlphanumeric(8))
                            .toAbsolutePath().toString(),
                    0);
            try {
                FileUtils.copyFile(
                        Main.upfuzzConfigFilePath.toFile(),
                        Paths.get(this.libnyx.getSharedir(), "config.json")
                                .toFile(),
                        false);
            } catch (IOException e) {
                // e.printStackTrace();
                logger.info(
                        "[NyxMode] config.json unable to copy into sharedir");
                logger.info("[NyxMode] Disabling Nyx Mode");
                Config.getConf().nyxMode = false; // disable nyx
            }
            try {
                FileUtils.copyFile(
                        Paths.get("./", Config.getConf().nyxFuzzSH).toFile(), // fuzz_no_pt.sh
                                                                              // script
                                                                              // location
                        Paths.get(this.libnyx.getSharedir(), "fuzz_no_pt.sh")
                                .toFile(),
                        false);
            } catch (IOException e) {
                // e.printStackTrace();
                logger.info(
                        "[NyxMode] fuzz_no_pt.sh unable to copy into sharedir");
                logger.info("[NyxMode] Disabling Nyx Mode");
                Config.getConf().nyxMode = false; // disable nyx
            }
            try {
                FileUtils.copyFile(
                        Paths.get("./", "nyx_mode", "config.ron").toFile(),
                        Paths.get(this.libnyx.getSharedir(), "config.ron")
                                .toFile(),
                        false);
            } catch (IOException e) {
                // e.printStackTrace();
                logger.info(
                        "[NyxMode] config.ron unable to copy into sharedir");
                logger.info("[NyxMode] Disabling Nyx Mode");
                Config.getConf().nyxMode = false; // disable nyx
            }
            try {
                FileUtils.copyFile(
                        Paths.get("./", "nyx_mode", "packer", "packer",
                                "linux_x86_64-userspace", "bin64", "hget_no_pt")
                                .toFile(),
                        Paths.get(this.libnyx.getSharedir(), "hget_no_pt")
                                .toFile(),
                        false);
            } catch (IOException e) {
                // e.printStackTrace();
                logger.info(
                        "[NyxMode] hget_no_pt unable to copy into sharedir");
                logger.info("[NyxMode] Disabling Nyx Mode");
                Config.getConf().nyxMode = false; // disable nyx
            }
            try {
                FileUtils.copyFile(
                        Paths.get("./", "nyx_mode", "packer", "packer",
                                "linux_x86_64-userspace", "bin64", "hcat_no_pt")
                                .toFile(),
                        Paths.get(this.libnyx.getSharedir(), "hcat_no_pt")
                                .toFile(),
                        false);
            } catch (IOException e) {
                // e.printStackTrace();
                logger.info(
                        "[NyxMode] hget_no_pt unable to copy into sharedir");
                logger.info("[NyxMode] Disabling Nyx Mode");
                Config.getConf().nyxMode = false; // disable nyx
            }
            // Copy over C Agent and MiniClient.jar
            try {
                FileUtils.copyFile(
                        new File("build/libs/c_agent"),
                        Paths.get(this.libnyx.getSharedir(), "c_agent")
                                .toFile(),
                        false);
                FileUtils.copyFile(
                        new File(
                                "build/libs/MiniClient.jar"),
                        Paths.get(this.libnyx.getSharedir(), "MiniClient.jar")
                                .toFile(),
                        false);
            } catch (IOException e) {
                logger.info(
                        "[NyxMode] unable to copy agent or MiniClient.jar into sharedir");
                logger.info("[NyxMode] Disabling Nyx Mode");
                Config.getConf().nyxMode = false; // disable nyx
            }
        }
    }

    public void start() throws InterruptedException {
        Thread clientThread = new Thread(new FuzzingClientSocket(this));
        clientThread.start();
        clientThread.join();
    }

    public static Executor initExecutor(int nodeNum,
            Set<String> targetSystemStates,
            Path configPath) {
        String system = Config.getConf().system;
        if (system.equals("cassandra")) {
            return new CassandraExecutor(nodeNum, targetSystemStates,
                    configPath);
        } else if (system.equals("hdfs")) {
            return new HdfsExecutor(nodeNum, targetSystemStates,
                    configPath);
        } else if (system.equals("hbase")) {
            return new HBaseExecutor(nodeNum, targetSystemStates,
                    configPath);
        }
        throw new RuntimeException(String.format(
                "System %s is not supported yet, supported system: cassandra, hdfs, hbase",
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

    public StackedFeedbackPacket executeStackedTestPacket(
            StackedTestPacket stackedTestPacket) {
        if (Config.getConf().nyxMode) {
            return executeStackedTestPacketNyx(stackedTestPacket);
        } else {
            return executeStackedTestPacketRegular(stackedTestPacket);
        }
    }

    private Path previousConfigPath = null;

    // Helper move it into utils later
    private static boolean isSameConfig(Path configPath1, Path configPath2) {
        Path oriConfigPath1 = configPath1.resolve("oriconfig");
        assert oriConfigPath1.toFile().isDirectory();
        Path oriConfigPath2 = configPath2.resolve("oriconfig");
        assert oriConfigPath2.toFile().isDirectory();
        for (File file : oriConfigPath1.toFile().listFiles()) {
            File file2 = oriConfigPath2.resolve(file.getName()).toFile();
            if (file2.exists()) {
                try (
                        InputStream s1 = new FileInputStream(file);
                        InputStream s2 = new FileInputStream(file2)) {
                    if (!IOUtils.contentEquals(s1, s2)) {
                        return false;
                    }
                } catch (IOException e) {
                    return false;
                }
            } else {
                return false; // mismatch in names, config is different
            }
        }

        if (!Config.getConf().testSingleVersion) {
            Path upConfigPath1 = configPath1.resolve("upconfig");
            assert upConfigPath1.toFile().isDirectory();
            Path upConfigPath2 = configPath2.resolve("upconfig");
            assert upConfigPath2.toFile().isDirectory();
            for (File file : upConfigPath1.toFile().listFiles()) {
                File file2 = upConfigPath2.resolve(file.getName()).toFile();
                if (file2.exists()) {
                    try (
                            InputStream s1 = new FileInputStream(file);
                            InputStream s2 = new FileInputStream(file2)) {
                        if (!IOUtils.contentEquals(s1, s2)) {
                            return false;
                        }
                    } catch (IOException e) {
                        return false;
                    }
                } else {
                    return false; // mismatch in names, config is different
                }
            }
        }

        return true;
    }

    public StackedFeedbackPacket executeStackedTestPacketNyx(
            StackedTestPacket stackedTestPacket) {
        Path configPath = Paths.get(configDirPath.toString(),
                stackedTestPacket.configFileName);
        logger.info("[HKLOG] configPath = " + configPath);

        // config verification - do we really want this?, maybe just skip config
        // verification TODO
        // if (Config.getConf().verifyConfig) {
        // boolean validConfig = verifyConfig(configPath);
        // if (!validConfig) {
        // logger.error(
        // "problem with configuration! system cannot start up");
        // return null;
        // }
        // }
        // TODO write a compare method
        boolean sameConfigAsLastTime = false;
        if (this.previousConfigPath != null) {
            sameConfigAsLastTime = isSameConfig(this.previousConfigPath,
                    configPath);
        }
        if (this.previousConfigPath == null || !sameConfigAsLastTime) {
            // the miniClient will setup the distributed system according to the
            // defaultStackedTestPacket and the config
            Path defaultStackedTestPath = Paths.get(this.libnyx.getSharedir(),
                    "stackedTestPackets",
                    "defaultStackedPacket.ser");
            Path sharedConfigPath = Paths.get(this.libnyx.getSharedir(),
                    "archive.tar.gz");
            try {
                // Created sharedir/stackedTestPackets directory
                Paths.get(this.libnyx.getSharedir(), "stackedTestPackets")
                        .toFile().mkdir();
                // Copy the default stacked packet
                Utilities.writeObjectToFile(defaultStackedTestPath.toFile(),
                        stackedTestPacket);

                // Copy the config file to the sharedir
                // Zip the config into a zip file
                Process tar = Utilities.exec(
                        new String[] { "tar",
                                "-czf", "archive.tar.gz",
                                "./", },
                        configPath.toFile());
                tar.waitFor();

                System.out.println(configPath
                        .resolve("archive.tar.gz").toAbsolutePath().toString());
                FileUtils.copyFile(
                        configPath.resolve("archive.tar.gz")
                                .toFile(),
                        sharedConfigPath.toFile(), true);

            } catch (IOException e) {
                e.printStackTrace();
                return null;
            } catch (InterruptedException e) {
                // zip failed
                e.printStackTrace();
                return null;
            }
        }
        if (this.previousConfigPath == null) {
            this.libnyx.nyxNew();
        } else if (!sameConfigAsLastTime) {
            this.libnyx.nyxShutdown();
            this.libnyx.nyxNew();
        }
        this.previousConfigPath = configPath;

        // Now write the stackedTestPacket to be used for actual tests
        String stackedTestFileLocation = "stackedTestPackets/"
                + RandomStringUtils.randomAlphanumeric(8) + ".ser";
        Path stackedTestPath = Paths.get(this.libnyx.getSharedir(),
                stackedTestFileLocation);

        try {
            Utilities.writeObjectToFile(stackedTestPath.toFile(),
                    stackedTestPacket);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        // tell the nyx agent where to find the stackedTestPacket
        this.libnyx.setInput(stackedTestFileLocation);

        this.libnyx.nyxExec();

        // get feedback file from hpush dir (in workdir)
        Path stackedFeedbackPath = Paths.get(
                this.libnyx.getWorkdir(),
                "dump",
                "stackedFeedbackPacket.ser");

        // convert it to StackedFeedbackPacket
        StackedFeedbackPacket stackedFeedbackPacket;
        try (DataInputStream in = new DataInputStream(new FileInputStream(
                stackedFeedbackPath.toAbsolutePath().toString()))) {
            int intType = in.readInt();
            if (intType == -1) {
                logger.info("Executor startup error!");
                return null;
            }
            if (intType != PacketType.StackedFeedbackPacket.value) {
                logger.info("Incorrect packet type hit");
                return null;
            }
            stackedFeedbackPacket = StackedFeedbackPacket.read(in);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return stackedFeedbackPacket;
    }

    /**
     * start the old version system, execute and count the coverage of all
     * test cases of stackedFeedbackPacket, perform an upgrade process, check
     * the (1) upgrade process failed (2) result inconsistency
     * @param stackedTestPacket the stacked test packets from server
     */
    public StackedFeedbackPacket executeStackedTestPacketRegular(
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
            logger.info("[Debugging Mode] Start up the cluster only");
            try {
                Thread.sleep(7200 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            logger.info("[Debugging Mode] System exit");
            System.exit(1);
        }

        StackedFeedbackPacket stackedFeedbackPacket = runTheTests(executor,
                stackedTestPacket);

        tearDownExecutor();
        return stackedFeedbackPacket;
    }

    public FullStopFeedbackPacket executeFullStopPacket(
            FullStopPacket fullStopPacket) {
        int nodeNum = fullStopPacket.getNodeNum();

        logger.debug("full stop: \n");
        // logger.debug(fullStopPacket.fullStopUpgrade);

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

        // LOG checking1
        Map<Integer, LogInfo> logInfoBeforeUpgrade = null;
        if (Config.getConf().enableLogCheck) {
            logger.info("[HKLOG] error log checking");
            logInfoBeforeUpgrade = executor.grepLogInfo();
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
                    .checkResultConsistency(oriResult, upResult, true);
            if (!compareRes.left) {
                fullStopFeedbackPacket.isInconsistent = true;
                fullStopFeedbackPacket.inconsistencyReport = genInconsistencyReport(
                        executor.executorID, fullStopPacket.configFileName,
                        compareRes.right, recordFullStopPacket(fullStopPacket));

                logger.info("Execution ID = " + executor.executorID
                        + "\ninconsistency: " + compareRes.right);
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
            assert logInfoBeforeUpgrade != null;
            Map<Integer, LogInfo> logInfo = extractErrorLog(executor,
                    logInfoBeforeUpgrade);
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
        // logger.debug(testPlanPacket.testPlan);

        String testPlanPacketStr = recordTestPlanPacket(testPlanPacket);
        int nodeNum = testPlanPacket.getNodeNum();

        // read states
        Set<String> targetSystemStates = null;
        if (Config.getConf().enableStateComp) {
            Path targetSystemStatesPath = Paths.get(
                    System.getProperty("user.dir"),
                    Config.getConf().targetSystemStateFile);
            try {
                targetSystemStates = readState(targetSystemStatesPath);
            } catch (IOException e) {
                logger.error("Not tracking system state");
                e.printStackTrace();
                System.exit(1);
            }
        }

        Path configPath = Paths.get(configDirPath.toString(),
                testPlanPacket.configFileName);
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
        executor = initExecutor(testPlanPacket.getNodeNum(), targetSystemStates,
                configPath);
        boolean startUpStatus = startUpExecutor();
        if (!startUpStatus) {
            return null;
        }

        // LOG checking1
        Map<Integer, LogInfo> logInfoBeforeUpgrade = null;
        if (Config.getConf().enableLogCheck) {
            logger.info("[HKLOG] error log checking");
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
                testPlanPacket.systemID, testPlanPacket.configFileName,
                testPlanPacket.testPacketID, testPlanFeedBacks);
        testPlanFeedbackPacket.fullSequence = testPlanPacketStr;

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
            testPlanFeedbackPacket.isEventFailed = true;

            testPlanFeedbackPacket.eventFailedReport = genTestPlanFailureReport(
                    executor.eventIdx, executor.executorID,
                    testPlanPacket.configFileName,
                    testPlanPacketStr);
            testPlanFeedbackPacket.isInconsistent = false;
            testPlanFeedbackPacket.inconsistencyReport = "";
        } else {

            // Test single version
            if (Config.getConf().testSingleVersion) {
                try {
                    ExecutionDataStore[] oriCoverages = executor
                            .collectCoverageSeparate("original");
                    if (oriCoverages != null) {
                        for (int nodeIdx = 0; nodeIdx < nodeNum; nodeIdx++) {
                            testPlanFeedbackPacket.feedBacks[nodeIdx].originalCodeCoverage = oriCoverages[nodeIdx];
                        }
                    }
                } catch (Exception e) {
                    // Cannot collect code coverage in the upgraded version
                    testPlanFeedbackPacket.isEventFailed = true;
                    testPlanFeedbackPacket.eventFailedReport = genOriCoverageCollFailureReport(
                            executor.executorID, testPlanPacket.configFileName,
                            recordTestPlanPacket(testPlanPacket)) + "Exception:"
                            + e;
                    tearDownExecutor();
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
                    // logger.debug("[HKLOG] full-stop results = \n"
                    // + testPlanPacket.testPlan.validationReadResultsOracle);
                    // logger.debug("[HKLOG] rolling upgrade results = \n"
                    // + testPlanReadResults);
                    compareRes = executor
                            .checkResultConsistency(
                                    testPlanPacket.testPlan.validationReadResultsOracle,
                                    testPlanReadResults, false);
                    if (!compareRes.left) {
                        testPlanFeedbackPacket.isInconsistent = true;
                        testPlanFeedbackPacket.inconsistencyReport = genTestPlanInconsistencyReport(
                                executor.executorID,
                                testPlanPacket.configFileName,
                                compareRes.right, testPlanPacketStr);
                    }
                } else {
                    logger.debug("validationReadResultsOracle is empty!");
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
                            executor.executorID, testPlanPacket.configFileName,
                            recordTestPlanPacket(testPlanPacket)) + "Exception:"
                            + e;
                    tearDownExecutor();
                    return testPlanFeedbackPacket;
                }
            }
        }

        // LOG checking2
        if (Config.getConf().enableLogCheck) {
            if (Config.getConf().testSingleVersion) {
                logger.info("[HKLOG] error log checking");
                Map<Integer, LogInfo> logInfo = extractErrorLog(executor,
                        logInfoBeforeUpgrade);
                if (hasERRORLOG(logInfo)) {
                    testPlanFeedbackPacket.hasERRORLog = true;
                    testPlanFeedbackPacket.errorLogReport = genErrorLogReport(
                            executor.executorID, testPlanPacket.configFileName,
                            logInfo);
                }
            } else {
                logger.info("[HKLOG] error log checking");
                assert logInfoBeforeUpgrade != null;
                Map<Integer, LogInfo> logInfo = extractErrorLog(executor,
                        logInfoBeforeUpgrade);
                if (hasERRORLOG(logInfo)) {
                    testPlanFeedbackPacket.hasERRORLog = true;
                    testPlanFeedbackPacket.errorLogReport = genErrorLogReport(
                            executor.executorID, testPlanPacket.configFileName,
                            logInfo);
                }
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
                    .executeCommands(tp.validationCommandSequenceList);
            testID2oriResults.put(tp.testPacketID, oriResult);
        }

        StackedFeedbackPacket stackedFeedbackPacket = new StackedFeedbackPacket(
                stackedTestPacket.configFileName,
                Utilities.extractTestIDs(stackedTestPacket));
        stackedFeedbackPacket.fullSequence = mixedTestPacketStr;

        // LOG checking1
        Map<Integer, LogInfo> logInfoBeforeUpgrade = null;
        if (Config.getConf().enableLogCheck) {
            logger.info("[HKLOG] error log checking");
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
                        .executeCommands(tp.validationCommandSequenceList);
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
                                testID2upResults.get(tp.testPacketID), true);

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
            logger.info("[HKLOG] error log checking");
            assert logInfoBeforeUpgrade != null;
            Map<Integer, LogInfo> logInfo = extractErrorLog(executor,
                    logInfoBeforeUpgrade);
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
            executor.teardown();
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

    public static String genInconsistencyReport(String executorID,
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

    public static String genUpgradeFailureReport(String executorID,
            String configFileName) {
        return "[Upgrade Failed]\n" +
                "executionId = " + executorID + "\n" +
                "ConfigIdx = " + configFileName + "\n";
    }

    public static String genDowngradeFailureReport(String executorID,
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

    public static String genErrorLogReport(String executorID,
            String configFileName,
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

    public static String recordStackedTestPacket(
            StackedTestPacket stackedTestPacket) {
        StringBuilder sb = new StringBuilder();
        for (TestPacket tp : stackedTestPacket.getTestPacketList()) {
            for (String cmdStr : tp.originalCommandSequenceList) {
                sb.append(cmdStr).append("\n");
            }
            sb.append("\n");
            for (String cmdStr : tp.validationCommandSequenceList) {
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

    public static String recordSingleTestPacket(TestPacket tp) {
        StringBuilder sb = new StringBuilder();
        sb.append("[Original Command Sequence]\n");
        for (String commandStr : tp.originalCommandSequenceList) {
            sb.append(commandStr).append("\n");
        }
        sb.append("\n\n");
        sb.append("[Read Command Sequence]\n");
        for (String commandStr : tp.validationCommandSequenceList) {
            sb.append(commandStr).append("\n");
        }
        return sb.toString();
    }

    public static Map<Integer, LogInfo> filterErrorLog(
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

    public static boolean hasERRORLOG(Map<Integer, LogInfo> logInfo) {
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

    public static Map<Integer, LogInfo> extractErrorLog(Executor executor,
            Map<Integer, LogInfo> logInfoBeforeUpgrade) {
        if (Config.getConf().filterLogBeforeUpgrade) {
            return FuzzingClient.filterErrorLog(
                    logInfoBeforeUpgrade,
                    executor.grepLogInfo());
        } else {
            return executor.grepLogInfo();
        }
    }
}
