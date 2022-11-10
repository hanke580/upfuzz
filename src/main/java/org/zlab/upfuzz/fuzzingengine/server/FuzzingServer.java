package org.zlab.upfuzz.fuzzingengine.server;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jacoco.core.data.ExecutionDataStore;
import org.zlab.upfuzz.CommandPool;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.cassandra.CassandraCommandPool;
import org.zlab.upfuzz.cassandra.CassandraExecutor;
import org.zlab.upfuzz.cassandra.CassandraState;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.fuzzingengine.FeedBack;
import org.zlab.upfuzz.fuzzingengine.Fuzzer;
import org.zlab.upfuzz.fuzzingengine.packet.*;
import org.zlab.upfuzz.fuzzingengine.executor.Executor;
import org.zlab.upfuzz.fuzzingengine.testplan.FullStopUpgrade;
import org.zlab.upfuzz.fuzzingengine.testplan.TestPlan;
import org.zlab.upfuzz.fuzzingengine.testplan.event.Event;
import org.zlab.upfuzz.fuzzingengine.testplan.event.command.ShellCommand;
import org.zlab.upfuzz.fuzzingengine.testplan.event.fault.*;
import org.zlab.upfuzz.fuzzingengine.testplan.event.upgradeop.FinalizeUpgrade;
import org.zlab.upfuzz.fuzzingengine.testplan.event.upgradeop.HDFSStopSNN;
import org.zlab.upfuzz.fuzzingengine.testplan.event.upgradeop.PrepareUpgrade;
import org.zlab.upfuzz.fuzzingengine.testplan.event.upgradeop.UpgradeOp;
import org.zlab.upfuzz.hdfs.HdfsCommandPool;
import org.zlab.upfuzz.hdfs.HdfsExecutor;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.Pair;
import org.zlab.upfuzz.utils.Utilities;

public class FuzzingServer {
    static Logger logger = LogManager.getLogger(FuzzingServer.class);
    static Random rand = new Random();

    // Seed Corpus (tuple(Seed, Info))
    public Corpus corpus = new Corpus();
    public List<String> configFileNames = new LinkedList<>();
    public TestPlanCorpus testPlanCorpus = new TestPlanCorpus();
    public FullStopCorpus fullStopCorpus = new FullStopCorpus();

    private int testID = 0;
    private int finishedTestID = 0;

    public CommandPool commandPool;
    public Executor executor;
    public Class<? extends State> stateClass;

    // seeds: sent to client and waiting for Feedback
    private final Map<Integer, Seed> testID2Seed;
    private final Map<Integer, TestPlan> testID2TestPlan;

    public final Queue<StackedTestPacket> stackedTestPackets;
    public final Queue<FullStopPacket> fullStopPackets;
    private final Queue<TestPlanPacket> testPlanPackets;

    public Set<String> targetSystemStates = null;

    // When merge new branches, increase this number
    public static int originalCoveredBranches = 0;
    public static int originalProbeNum = 0;
    public static int upgradedCoveredBranches = 0;
    public static int upgradedProbeNum = 0;

    public static List<Pair<Integer, Integer>> originalCoverageAlongTime = new ArrayList<>(); // time:
    public static List<Pair<Integer, Integer>> upgradedCoverageAlongTime = new ArrayList<>(); // time:

    public static long lastTimePoint = 0;
    public long startTime;

    ExecutionDataStore curOriCoverage;
    ExecutionDataStore curUpCoverage;

    public static int round = 0;
    public static int epoch = 0;
    public static int crashID = 0;

    boolean isFullStopUpgrade = true;

    public FuzzingServer() {
        testID2Seed = new HashMap<>();
        testID2TestPlan = new HashMap<>();
        stackedTestPackets = new LinkedList<>();
        fullStopPackets = new LinkedList<>();
        testPlanPackets = new LinkedList<>();
        curOriCoverage = new ExecutionDataStore();
        curUpCoverage = new ExecutionDataStore();

        startTime = TimeUnit.SECONDS.convert(System.nanoTime(),
                TimeUnit.NANOSECONDS);
    }

    private void init() {
        if (Config.getConf().initSeedDir != null) {
            corpus.initCorpus(Paths.get(Config.getConf().initSeedDir));
        }

        // maintain the num of configuration files
        // read all configurations file name in a list
        Path configDirPath = Paths.get(System.getProperty("user.dir"),
                Config.getConf().configDir, Config.getConf().originalVersion
                        + "_" + Config.getConf().upgradedVersion);
        File[] configFiles = configDirPath.toFile().listFiles();
        for (File file : configFiles) {
            if (file.isDirectory()) {
                configFileNames.add(file.getName());
            }
        }
        logger.info("config test num: " + configFileNames.size());

        if (Config.getConf().system.equals("cassandra")) {
            executor = new CassandraExecutor();
            commandPool = new CassandraCommandPool();
            stateClass = CassandraState.class;

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    Utilities.exec(
                            new String[] { "bin/nodetool", "stopdaemon" },
                            Config.getConf().oldSystemPath);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }));

        } else if (Config.getConf().system.equals("hdfs")) {
            executor = new HdfsExecutor();
            commandPool = new HdfsCommandPool();
            stateClass = HdfsState.class;

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    Utilities.exec(new String[] { "sbin/stop-dfs.sh" },
                            Config.getConf().oldSystemPath);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }));
        }

        Path targetSystemStatesPath = Paths.get(System.getProperty("user.dir"),
                Config.getConf().targetSystemStateFile);
        try {
            targetSystemStates = readState(targetSystemStatesPath);
        } catch (IOException e) {
            logger.error("Not tracking system state");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void start() {
        init();
        new Thread(new FuzzingServerSocket(this)).start();
        // new Thread(new FuzzingServerDispatcher(this)).start();
    }

    public synchronized Packet getOneTest() {
        if (Config.getConf().testingMode == 0) {
            if (stackedTestPackets.isEmpty())
                fuzzOne();
            assert !stackedTestPackets.isEmpty();
            return stackedTestPackets.poll();
        } else if (Config.getConf().testingMode == 1) {

            if (testPlanPackets.isEmpty() && !fuzzTestPlan()) {
                fuzzFullStopUpgrade();
                return fullStopPackets.poll();
            }

            assert !testPlanPackets.isEmpty();
            return testPlanPackets.poll();
        } else if (Config.getConf().testingMode == 2) {
            return generateMixedTestPacket();
        } else if (Config.getConf().testingMode == 3) {
            // execute example testplan
            logger.info("execute example test plan");
            if (testPlanPackets.isEmpty())
                generateExampleTestplanPacket();
            return testPlanPackets.poll();
        } else if (Config.getConf().testingMode == 4) {
            // test full-stop and rolling upgrade iteratively
            if (isFullStopUpgrade) {
                if (stackedTestPackets.isEmpty())
                    fuzzOne();
                assert !stackedTestPackets.isEmpty();
                return stackedTestPackets.poll();
            } else {
                return generateMixedTestPacket();
            }
        }
        throw new RuntimeException(
                String.format("testing Mode [%d] is not in correct scope",
                        Config.getConf().testingMode));
    }

    public MixedTestPacket generateMixedTestPacket() {
        StackedTestPacket stackedTestPacket;
        TestPlanPacket testPlanPacket;

        if (stackedTestPackets.isEmpty())
            fuzzOne();
        stackedTestPacket = stackedTestPackets.poll();

        if (testPlanPackets.isEmpty())
            fuzzTestPlan();
        testPlanPacket = testPlanPackets.poll();

        if (testPlanPacket == null) {
            logger.error("hklog null testPlanPacket");
        }

        return new MixedTestPacket(stackedTestPacket, testPlanPacket);

    }

    public void fuzzOne() {
        // Pick one test case from the corpus, fuzz it for mutationEpoch
        // Add the new tests into the stackedTestPackets
        // All packets have been dispatched, now fuzz next seed
        Seed seed = corpus.getSeed();
        round++;
        StackedTestPacket stackedTestPacket;

        String configFileName = null;
        if (!configFileNames.isEmpty()) {
            configFileName = configFileNames
                    .get(rand.nextInt(configFileNames.size()));
        }

        logger.info("configFileName = " + configFileName);

        if (seed == null) {
            // corpus is empty, random generate one test packet and wait
            stackedTestPacket = new StackedTestPacket(Config.getConf().nodeNum,
                    configFileName);
            for (int i = 0; i < Config.getConf().STACKED_TESTS_NUM; i++) {
                seed = Executor.generateSeed(commandPool, stateClass);
                if (seed != null) {
                    testID2Seed.put(testID, seed);
                    stackedTestPacket.addTestPacket(seed, testID++);
                }
            }
            if (stackedTestPacket.size() != 0) {
                stackedTestPackets.add(stackedTestPacket);
            } else {
                logger.error("failed to generate any test packet");
            }
        } else {
            // get a seed from corpus, now fuzz it for an epoch
            stackedTestPacket = new StackedTestPacket(Config.getConf().nodeNum,
                    configFileName);
            for (int i = 0; i < Config.getConf().mutationEpoch; i++) {
                // logger.info("Generating " + i + " packet");
                if (i != 0 && i % Config.getConf().STACKED_TESTS_NUM == 0) {
                    stackedTestPackets.add(stackedTestPacket);
                    stackedTestPacket = new StackedTestPacket(
                            Config.getConf().nodeNum, configFileName);
                }
                // Mutation
                // If mutation fails, drop this mutation, so the
                // testpack size might decrease...
                Seed mutateSeed = SerializationUtils.clone(seed);
                if (mutateSeed.mutate(commandPool, stateClass)) {
                    testID2Seed.put(testID, mutateSeed);
                    stackedTestPacket.addTestPacket(mutateSeed, testID++);
                } else {
                    logger.info("Mutation failed");
                    i--;
                }
            }
            if (stackedTestPacket.size() != 0) {
                stackedTestPackets.add(stackedTestPacket);
            }
        }
    }

    private boolean fuzzTestPlan() {

        // We should first try to mutate the test plan, but there
        // should still be possibility for generating a new test plan
        // Mutate a testplan
        TestPlan testPlan = testPlanCorpus.getTestPlan();

        if (testPlan != null) {
            for (int i = 0; i < Config.getConf().testPlanMutationEpoch; i++) {
                TestPlan mutateTestPlan = SerializationUtils.clone(testPlan);
                mutateTestPlan.mutate();
                logger.info("mutate a test plan");
                testID2TestPlan.put(testID, testPlan);
                testPlanPackets.add(new TestPlanPacket(
                        Config.getConf().system,
                        testID++, testPlan));
            }
        } else {

            // Not disable system state comparison

            FullStopSeed fullStopSeed;
            Seed seed;
            if (corpus.isEmpty()) {
                // random generate a fullStopSeed
                seed = Executor.generateSeed(commandPool, stateClass);
            } else {
                seed = corpus.peekSeed();
            }
            fullStopSeed = new FullStopSeed(seed, Config.getConf().nodeNum,
                    null);

            // Generate several test plan...
            for (int i = 0; i < Config.getConf().testPlanGenerationNum; i++) {
                testPlan = generateTestPlan(fullStopSeed);
                testID2TestPlan.put(testID, testPlan);
                testPlanPackets.add(new TestPlanPacket(
                        Config.getConf().system,
                        testID++, testPlan));
            }
        }
        return true;
    }

    public boolean fuzzFullStopUpgrade() {
        FullStopSeed fullStopSeed = fullStopCorpus.getSeed();
        round++;
        if (fullStopSeed == null) {
            // corpus is empty, generate some
            Seed seed = Executor.generateSeed(commandPool, stateClass);

            if (seed != null) {
                FullStopUpgrade fullStopUpgrade = new FullStopUpgrade(
                        Config.getConf().nodeNum,
                        seed.originalCommandSequence.getCommandStringList(),
                        seed.validationCommandSequnece.getCommandStringList(),
                        targetSystemStates);
                testID2Seed.put(testID, seed);
                fullStopPackets.add(new FullStopPacket(Config.getConf().system,
                        testID++, fullStopUpgrade));
            } else {
                logger.error("Seed is null");
            }
        } else {
            // Get a full-stop seed, mutate it and create some new seeds
            Seed seed = fullStopSeed.seed;

            for (int i = 0; i < Config.getConf().mutationEpoch; i++) {
                Seed mutateSeed = SerializationUtils.clone(seed);
                if (mutateSeed.mutate(commandPool, stateClass)) {
                    FullStopUpgrade fullStopUpgrade = new FullStopUpgrade(
                            Config.getConf().nodeNum,
                            seed.originalCommandSequence.getCommandStringList(),
                            seed.validationCommandSequnece
                                    .getCommandStringList(),
                            targetSystemStates);
                    testID2Seed.put(testID, mutateSeed);
                    fullStopPackets
                            .add(new FullStopPacket(Config.getConf().system,
                                    testID++,
                                    fullStopUpgrade));
                } else {
                    logger.info("Mutation failed");
                    i--;
                }
            }
        }
        return true;
    }

    public FeedBack mergeCoverage(FeedBack[] feedBacks) {
        FeedBack fb = new FeedBack();
        if (feedBacks == null) {
            return fb;
        }
        for (FeedBack feedBack : feedBacks) {
            if (feedBack.originalCodeCoverage != null)
                fb.originalCodeCoverage.merge(feedBack.originalCodeCoverage);
            if (feedBack.upgradedCodeCoverage != null)
                fb.upgradedCodeCoverage.merge(feedBack.upgradedCodeCoverage);
        }
        return fb;
    }

    public TestPlan generateTestPlan(FullStopSeed fullStopSeed) {
        // Some systems might have special requirements for
        // upgrade, like HDFS needs to upgrade NN first

        int nodeNum = fullStopSeed.nodeNum;

        int faultNum = rand.nextInt(Config.getConf().faultMaxNum + 1);
        List<Pair<Fault, FaultRecover>> faultPairs = Fault
                .randomGenerateFaults(nodeNum, faultNum);

        List<Event> upgradeOps = new LinkedList<>();
        for (int i = 0; i < nodeNum; i++) {
            upgradeOps.add(new UpgradeOp(i));
        }
        if (Config.getConf().shuffleUpgradeOrder) {
            Collections.shuffle(upgradeOps);
        }
        if (Config.getConf().system.equals("hdfs")) {
            upgradeOps.add(0, new HDFSStopSNN());
        }
        upgradeOps.add(0, new PrepareUpgrade());

        List<Event> upgradeOpAndFaults = interleaveFaultAndUpgradeOp(faultPairs,
                upgradeOps);

        if (Config.getConf().useExampleTestPlan) {
            // DEBUG USE
            logger.info("use example test plan");

            List<Event> exampleEvents = new LinkedList<>();
            // nodeNum should be 3
            assert nodeNum == 3;
            // for (int i = 0; i < Config.getConf().nodeNum - 1; i++) {
            // exampleEvents.add(new UpgradeOp(i));
            // }
            exampleEvents.add(new PrepareUpgrade());
            if (Config.getConf().system.equals("hdfs")) {
                exampleEvents.add(new HDFSStopSNN());
            }
            exampleEvents.add(new UpgradeOp(0));

            // exampleEvents.add(new ShellCommand("dfs -touchz /tmp"));
            // exampleEvents.add(new RestartFailure(0));

            exampleEvents.add(new UpgradeOp(1));
            exampleEvents.add(new UpgradeOp(2));
            // exampleEvents.add(new LinkFailure(0, 1));

            // exampleEvents.add(new LinkFailureRecover(0, 1));

            // exampleEvents.add(new UpgradeOp(2));
            // exampleEvents.add(new UpgradeOp(3));
            // exampleEvents.add(0, new LinkFailure(1, 2));
            return new TestPlan(nodeNum, exampleEvents, targetSystemStates,
                    fullStopSeed.targetSystemStateResults);
        }

        // TODO: If the node is current down, we should switch to
        // another node for execution.
        // Randomly interleave the commands with the upgradeOp&faults
        List<Event> shellCommands = new LinkedList<>();
        if (fullStopSeed.seed != null)
            shellCommands = ShellCommand.seedCmd2Events(fullStopSeed.seed);
        else
            logger.error("empty full stop seed");

        List<Event> events = interleaveWithOrder(upgradeOpAndFaults,
                shellCommands);

        events.add(events.size(), new FinalizeUpgrade());
        return new TestPlan(nodeNum, events, targetSystemStates,
                fullStopSeed.targetSystemStateResults);
    }

    public void generateExampleTestplanPacket() {
        testPlanPackets.add(new TestPlanPacket(
                Config.getConf().system,
                testID++, generateExampleTestPlan()));
    }

    public TestPlan generateExampleTestPlan() {
        List<Event> exampleEvents = new LinkedList<>();
        int nodeNum = 3;
        exampleEvents.add(new PrepareUpgrade());
        if (Config.getConf().system.equals("hdfs")) {
            exampleEvents.add(new HDFSStopSNN());
        }
        exampleEvents.add(new UpgradeOp(0));
        exampleEvents.add(new UpgradeOp(1));
        exampleEvents.add(new UpgradeOp(2));

        Set<String> targetSystemStates = new HashSet<>();
        Map<Integer, Map<String, String>> oracle = new HashMap<>();

        return new TestPlan(nodeNum, exampleEvents, targetSystemStates,
                oracle);
    }

    public synchronized void updateStatus(
            FullStopFeedbackPacket fullStopFeedbackPacket) {
        // TODO: update status for test plan feed back
        FeedBack fb = mergeCoverage(fullStopFeedbackPacket.feedBacks);

        // Print states
        for (Integer nodeId : fullStopFeedbackPacket.systemStates.keySet()) {
            logger.info("node" + nodeId + " states: ");
            Map<String, String> states = fullStopFeedbackPacket.systemStates
                    .get(nodeId);
            for (String stateName : states.keySet()) {
                logger.info(String.format("state[%s] = %s", stateName,
                        Utilities.decodeString(states.get(stateName))));
            }
            logger.info("");
        }

        boolean addToCorpus = false;
        if (Config.getConf().useFeedBack && Utilities.hasNewBits(curOriCoverage,
                fb.originalCodeCoverage)) {
            addToCorpus = true;
            curOriCoverage.merge(fb.originalCodeCoverage);
        }

        if (Config.getConf().useFeedBack && Utilities.hasNewBits(curUpCoverage,
                fb.upgradedCodeCoverage)) {
            addToCorpus = true;
            curUpCoverage.merge(fb.upgradedCodeCoverage);
        }

        Path crashSubDir = createCrashSubDir();
        if (fullStopFeedbackPacket.isEventFailed) {
            String sb = "Event Failed\n";
            sb += fullStopFeedbackPacket.eventFailedReport;
            Path crashReport = Paths.get(
                    crashSubDir.toString(),
                    "crash_" + fullStopFeedbackPacket.testPacketID + ".report");
            Utilities.write2TXT(crashReport.toFile(), sb, false);
            crashID++;
        } else if (fullStopFeedbackPacket.isInconsistent) {
            String sb = "Result Inconsistent\n";
            sb += fullStopFeedbackPacket.inconsistencyReport;
            Path crashReport = Paths.get(
                    crashSubDir.toString(),
                    "crash_" + fullStopFeedbackPacket.testPacketID + ".report");
            Utilities.write2TXT(crashReport.toFile(), sb, false);
            crashID++;
        }
        testID2Seed.remove(fullStopFeedbackPacket.testPacketID);

        if (addToCorpus) {
            fullStopCorpus.addSeed(new FullStopSeed(
                    testID2Seed.get(fullStopFeedbackPacket.testPacketID),
                    fullStopFeedbackPacket.nodeNum,
                    fullStopFeedbackPacket.systemStates));

            logger.info("[HKLOG] system state = "
                    + fullStopFeedbackPacket.systemStates);

            // Update the coveredBranches to the newest value
            Pair<Integer, Integer> curOriCoverageStatus = Utilities
                    .getCoverageStatus(curOriCoverage);
            originalCoveredBranches = curOriCoverageStatus.left;
            originalProbeNum = curOriCoverageStatus.right;

            Pair<Integer, Integer> curUpCoverageStatus = Utilities
                    .getCoverageStatus(curUpCoverage);
            upgradedCoveredBranches = curUpCoverageStatus.left;
            upgradedProbeNum = curUpCoverageStatus.right;
        }

        Long timeElapsed = TimeUnit.SECONDS.convert(
                System.nanoTime(), TimeUnit.NANOSECONDS) - startTime;
        if (timeElapsed - lastTimePoint > Config.getConf().timeInterval ||
                lastTimePoint == 0) {
            // Insert a record (time: coverage)
            originalCoverageAlongTime.add(
                    new Pair(timeElapsed, originalCoveredBranches));
            upgradedCoverageAlongTime.add(
                    new Pair(timeElapsed, upgradedCoveredBranches));
            lastTimePoint = timeElapsed;
        }
        finishedTestID++;
        printInfo();
        System.out.println();
    }

    public synchronized void updateStatus(
            TestPlanFeedbackPacket testPlanFeedbackPacket) {
        // TODO: update status for test plan feed back
        // Do we utilize the feedback?
        // Do we mutate the test plan?

        FeedBack fb = mergeCoverage(testPlanFeedbackPacket.feedBacks);

        if (Config.getConf().useFeedBack && Utilities.hasNewBits(curOriCoverage,
                fb.upgradedCodeCoverage)) {
            // Add to test plan corpus?

            // Do not maintain a test plan for now

            // testPlanCorpus.addTestPlan(
            // testID2TestPlan.get(testPlanFeedbackPacket.testPacketID));

            curOriCoverage.merge(fb.upgradedCodeCoverage);
        }

        if (testPlanFeedbackPacket.isEventFailed) {
            // event execution failed
            Path crashSubDir = createCrashSubDir();

            String sb = "[Event Execution Failed]\n";
            sb += testPlanFeedbackPacket.eventFailedReport;
            Path crashReport = Paths.get(
                    crashSubDir.toString(),
                    "crash_" + testPlanFeedbackPacket.testPacketID + ".report");
            Utilities.write2TXT(crashReport.toFile(), sb, false);
            crashID++;
        } else if (testPlanFeedbackPacket.isInconsistent) {
            // TODO: Log the inconsistency
        }
        testID2TestPlan.remove(testPlanFeedbackPacket.testPacketID);

        // Update the coveredBranches to the newest value
        Pair<Integer, Integer> curOriCoverageStatus = Utilities
                .getCoverageStatus(curOriCoverage);
        originalCoveredBranches = curOriCoverageStatus.left;
        originalProbeNum = curOriCoverageStatus.right;

        Pair<Integer, Integer> curUpCoverageStatus = Utilities
                .getCoverageStatus(curUpCoverage);
        upgradedCoveredBranches = curUpCoverageStatus.left;
        upgradedProbeNum = curUpCoverageStatus.right;

        finishedTestID++;
        printInfo();
        System.out.println();

    }

    public synchronized void updateStatus(
            StackedFeedbackPacket stackedFeedbackPacket) {
        if (stackedFeedbackPacket.isUpgradeProcessFailed) {
            // Write Bug report (Upgrade Process Failure)
            Path crashSubDir = createCrashSubDir();

            String sb = "";
            sb += "Upgrade Process Failed\n";
            sb += stackedFeedbackPacket.stackedCommandSequenceStr;
            Path crashReport = Paths.get(crashSubDir.toString(),
                    "crash.report");
            Utilities.write2TXT(crashReport.toFile(), sb, false);
            crashID++;
        }

        logger.info("update Status");

        FuzzingServerHandler.printClientNum();

        logger.info("fp size = " + stackedFeedbackPacket.getFpList().size());

        Path crashSubDir = null;
        for (FeedbackPacket feedbackPacket : stackedFeedbackPacket
                .getFpList()) {
            finishedTestID++;
            if (Config.getConf().useFeedBack) {
                boolean addToCorpus = false;
                // Merge all the feedbacks
                FeedBack fb = mergeCoverage(feedbackPacket.feedBacks);
                if (Utilities.hasNewBits(
                        curOriCoverage,
                        fb.originalCodeCoverage)) {
                    // Write Seed to Disk + Add to Corpus
                    curOriCoverage.merge(
                            fb.originalCodeCoverage);
                    addToCorpus = true;
                }
                if (Utilities.hasNewBits(curUpCoverage,
                        fb.upgradedCodeCoverage)) {
                    curUpCoverage.merge(
                            fb.upgradedCodeCoverage);
                    addToCorpus = true;
                }
                if (addToCorpus) {
                    Seed seed = testID2Seed.get(feedbackPacket.testPacketID);
                    Fuzzer.saveSeed(seed.originalCommandSequence,
                            seed.validationCommandSequnece);
                    corpus.addSeed(seed);

                }
            }

            if (!stackedFeedbackPacket.isUpgradeProcessFailed &&
                    feedbackPacket.isInconsistent) {
                // Write Bug report (Inconsistency)
                if (crashSubDir == null) {
                    crashSubDir = createCrashSubDir();
                }

                String sb = "";
                sb += "Result Inconsistency between two versions\n";
                sb += feedbackPacket.inconsistencyReport;
                Path crashReport = Paths.get(
                        crashSubDir.toString(),
                        "crash_" + feedbackPacket.testPacketID + ".report");

                // FIXME: Uncomment this when the results lost problem is fixed
                Utilities.write2TXT(crashReport.toFile(), sb, false);
                crashID++;
            }
            // Remove the seed from the waiting list
            testID2Seed.remove(feedbackPacket.testPacketID);
        }

        // Update the coveredBranches to the newest value
        Pair<Integer, Integer> curOriCoverageStatus = Utilities
                .getCoverageStatus(curOriCoverage);
        originalCoveredBranches = curOriCoverageStatus.left;
        originalProbeNum = curOriCoverageStatus.right;

        Pair<Integer, Integer> curUpCoverageStatus = Utilities
                .getCoverageStatus(curUpCoverage);
        upgradedCoveredBranches = curUpCoverageStatus.left;
        upgradedProbeNum = curUpCoverageStatus.right;

        Long timeElapsed = TimeUnit.SECONDS.convert(
                System.nanoTime(), TimeUnit.NANOSECONDS) - startTime;
        if (timeElapsed - lastTimePoint > Config.getConf().timeInterval ||
                lastTimePoint == 0) {
            // Insert a record (time: coverage)
            originalCoverageAlongTime.add(
                    new Pair(timeElapsed, originalCoveredBranches));
            upgradedCoverageAlongTime.add(
                    new Pair(timeElapsed, upgradedCoveredBranches));
            lastTimePoint = timeElapsed;
        }
        printInfo();
        System.out.println();
    }

    private Path createCrashSubDir() {
        while (Paths.get(Config.getConf().crashDir, "crash_" + epoch)
                .toFile()
                .exists()) {
            epoch++;
        }
        Path crashSubDir = Paths.get(Config.getConf().crashDir,
                "crash_" + epoch);
        crashSubDir.toFile().mkdir();
        return crashSubDir;
    }

    public void printInfo() {
        long timeElapsed = TimeUnit.SECONDS.convert(
                System.nanoTime(), TimeUnit.NANOSECONDS) - startTime;

        logger.info(
                "\n\n------------------- Executing one fuzzing test -------------------"
                        + "[Fuzz Status]\n"
                        +
                        "================================================================="
                        + "====================================================\n"
                        + "|"
                        + "Queue Size = " + corpus.queue.size() + "|"
                        + "Round = " + round + "|"
                        + "Crash Found = " + crashID + "|"
                        + "Current Test ID = " + testID + "|"
                        + "Finished Test Num = " + finishedTestID + "|"
                        + "Covered oriBranches Num = " + originalCoveredBranches
                        + "|"
                        + "Total oriBranch Num = " + originalProbeNum + "|"
                        + "Covered upBranches Num = " + upgradedCoveredBranches
                        + "|"
                        + "Total upBranch Num = " + upgradedProbeNum + "|"
                        + "Time Elapsed = " + timeElapsed + "s" + "|"
                        + "\n"
                        +
                        "-----------------------------------------------------------------"
                        + "----------------------------------------------------");

        // Print the coverage status
        // for (Pair<Integer, Integer> timeCoveragePair :
        // originalCoverageAlongTime) {
        // System.out.println("TIME: " + timeCoveragePair.left + "s"
        // + "\t\t Orginal Coverage: " + timeCoveragePair.right + "/"
        // + originalProbeNum + "\t\t percentage: "
        // + (float) timeCoveragePair.right / originalProbeNum + "%");
        // }

        // for (Pair<Integer, Integer> timeCoveragePair :
        // upgradedCoverageAlongTime) {
        // System.out.println("TIME: " + timeCoveragePair.left + "ms"
        // + "\t\t Upgraded Coverage: " + timeCoveragePair.right + "/"
        // + upgradedProbeNum + "\t\t percentage: "
        // + (float) timeCoveragePair.right / upgradedProbeNum + "%");
        // }
        System.out.println();
    }

    public static List<Event> interleaveFaultAndUpgradeOp(
            List<Pair<Fault, FaultRecover>> faultPairs,
            List<Event> upgradeOps) {
        // Upgrade op can happen with fault
        // E.g. isolate node1 -> upgrade node1 -> recover node1
        List<Event> upgradeOpAndFaults = new LinkedList<>(upgradeOps);
        for (Pair<Fault, FaultRecover> faultPair : faultPairs) {
            int pos1 = rand.nextInt(upgradeOpAndFaults.size() + 1);
            upgradeOpAndFaults.add(pos1, faultPair.left);
            int pos2 = Utilities.randWithRange(rand, pos1 + 1,
                    upgradeOpAndFaults.size() + 1);
            if (faultPair.left instanceof NodeFailure) {
                // the recover must be in the front of node upgrade
                int nodeIndex = ((NodeFailure) faultPair.left).nodeIndex;
                int nodeUpgradePos = 0;
                for (; nodeUpgradePos < upgradeOpAndFaults
                        .size(); nodeUpgradePos++) {
                    if (upgradeOpAndFaults
                            .get(nodeUpgradePos) instanceof UpgradeOp
                            && ((UpgradeOp) upgradeOpAndFaults.get(
                                    nodeUpgradePos)).nodeIndex == nodeIndex) {
                        break;
                    }
                }
                assert nodeUpgradePos != pos1;
                if (nodeUpgradePos > pos1
                        && nodeUpgradePos < upgradeOpAndFaults.size()) {
                    if (faultPair.right == null) {
                        upgradeOpAndFaults.remove(nodeUpgradePos);
                        continue;
                    }
                    pos2 = Utilities.randWithRange(rand, pos1 + 1,
                            nodeUpgradePos + 1);
                }
            }
            if (faultPair.right != null)
                upgradeOpAndFaults.add(pos2, faultPair.right);
        }

        return upgradeOpAndFaults;
    }

    public static List<Event> interleaveWithOrder(List<Event> events1,
            List<Event> events2) {
        // Merge two lists but still maintain the inner order
        // Prefer to execute events2 first. Not uniform distribution
        List<Event> events = new LinkedList<>();

        int size1 = events1.size();
        int size2 = events2.size();
        int totalEventSize = size1 + size2;
        int upgradeOpAndFaultsIdx = 0;
        int commandIdx = 0;
        for (int i = 0; i < totalEventSize; i++) {
            // Magic Number: Prefer to execute commands first
            // Also make the commands more separate
            if (Utilities.oneOf(rand, 3)) {
                if (upgradeOpAndFaultsIdx < events1.size())
                    events.add(events1.get(upgradeOpAndFaultsIdx++));
                else
                    break;
            } else {
                if (commandIdx < size2)
                    events.add(events2
                            .get(commandIdx++));
                else
                    break;
            }
        }
        if (upgradeOpAndFaultsIdx < size1) {
            for (int i = upgradeOpAndFaultsIdx; i < size1; i++) {
                events.add(events1.get(i));
            }
        } else if (commandIdx < size2) {
            for (int i = commandIdx; i < size2; i++) {
                events.add(events2.get(i));
            }
        }
        return events;
    }

    public static Set<String> readState(Path filePath)
            throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, List<String>> rawClass2States = mapper
                .readValue(filePath.toFile(), HashMap.class);
        Set<String> states = new HashSet<>();
        for (String className : rawClass2States.keySet()) {
            for (String fieldName : rawClass2States.get(className)) {
                states.add(className + "." + fieldName);
            }
        }
        return states;
    }
}
