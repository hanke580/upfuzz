package org.zlab.upfuzz.fuzzingengine.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jacoco.core.data.ExecutionDataStore;
import org.zlab.ocov.tracker.ObjectGraphCoverage;
import org.zlab.ocov.tracker.Runtime;
import org.zlab.upfuzz.CommandPool;
import org.zlab.upfuzz.CommandSequence;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.cassandra.CassandraCommandPool;
import org.zlab.upfuzz.cassandra.CassandraConfigGen;
import org.zlab.upfuzz.cassandra.CassandraExecutor;
import org.zlab.upfuzz.cassandra.CassandraState;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.fuzzingengine.FeedBack;
import org.zlab.upfuzz.fuzzingengine.configgen.ConfigGen;
import org.zlab.upfuzz.fuzzingengine.packet.*;
import org.zlab.upfuzz.fuzzingengine.executor.Executor;
import org.zlab.upfuzz.fuzzingengine.server.testtracker.TestTrackerGraph;
import org.zlab.upfuzz.fuzzingengine.testplan.FullStopUpgrade;
import org.zlab.upfuzz.fuzzingengine.testplan.TestPlan;
import org.zlab.upfuzz.fuzzingengine.testplan.event.Event;
import org.zlab.upfuzz.fuzzingengine.testplan.event.command.ShellCommand;
import org.zlab.upfuzz.fuzzingengine.testplan.event.downgradeop.DowngradeOp;
import org.zlab.upfuzz.fuzzingengine.testplan.event.fault.*;
import org.zlab.upfuzz.fuzzingengine.testplan.event.upgradeop.FinalizeUpgrade;
import org.zlab.upfuzz.fuzzingengine.testplan.event.upgradeop.HDFSStopSNN;
import org.zlab.upfuzz.fuzzingengine.testplan.event.upgradeop.PrepareUpgrade;
import org.zlab.upfuzz.fuzzingengine.testplan.event.upgradeop.UpgradeOp;
import org.zlab.upfuzz.hbase.HBaseConfigGen;
import org.zlab.upfuzz.hdfs.HdfsCommandPool;
import org.zlab.upfuzz.hdfs.HdfsConfigGen;
import org.zlab.upfuzz.hdfs.HdfsExecutor;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.hbase.HBaseCommandPool;
import org.zlab.upfuzz.hbase.HBaseExecutor;
import org.zlab.upfuzz.hbase.HBaseState;
import org.zlab.upfuzz.utils.Pair;
import org.zlab.upfuzz.utils.Utilities;

public class FuzzingServer {
    static Logger logger = LogManager.getLogger(FuzzingServer.class);
    static Random rand = new Random();

    // Debug
    public List<String> fixedWriteCommands;
    public List<String> fixedValidationCommands;

    // Target system
    public CommandPool commandPool;
    public Executor executor;
    public Class<? extends State> stateClass;

    // Corpus
    public Corpus corpus;

    public TestPlanCorpus testPlanCorpus = new TestPlanCorpus();
    public FullStopCorpus fullStopCorpus = new FullStopCorpus();
    private final Map<Integer, Seed> testID2Seed;
    private final Map<Integer, TestPlan> testID2TestPlan;

    // Next packet for execution
    public final Queue<StackedTestPacket> stackedTestPackets;
    public final Queue<FullStopPacket> fullStopPackets;
    private final Queue<TestPlanPacket> testPlanPackets;

    // Fuzzing Status
    public int firstMutationSeedNum = 0;

    private static int seedID = 0;
    private int testID = 0;
    private int finishedTestID = 0;
    public static int round = 0;
    public static int failureId = 0;
    public static int fullStopCrashNum = 0;
    public static int eventCrashNum = 0;
    public static int inconsistencyNum = 0;
    public static int errorLogNum = 0;
    boolean isFullStopUpgrade = true;
    private int finishedTestIdAgentGroup1 = 0;
    private int finishedTestIdAgentGroup2 = 0;

    private int oriNewFormatNum = 0;
    private int upNewFormatNum = 0;

    // This can be replaced by queue size
    private int newVersionDeltaCountForBranchCoverage = 0;
    private int newVersionDeltaCountForFormatCoverage = 0;

    private int executionCountWithOnlyNewBranchDelta = 0;
    private int executionCountWithOnlyNewFormatDelta = 0;
    private int executionCountWithOnlyNewBranchCoverage = 0;
    private int executionCountWithOnlyNewFormatCoverage = 0;
    private int executionCountWithoutAnyNewCoverage = 0;

    // Config mutation
    ConfigGen configGen;
    public Path configDirPath;

    // Format Coverage
    // Execute a test in old version
    private ObjectGraphCoverage oriObjCoverage;
    // Execute a test in new version
    private ObjectGraphCoverage upObjCoverage;

    // System state comparison
    public Set<String> targetSystemStates = new HashSet<>();

    // Coverage
    // before upgrade
    public static int oriCoveredBranches = 0;
    public static int oriProbeNum = 0;
    // after upgrade
    public static int upCoveredBranchesAfterUpgrade = 0;
    public static int upProbeNumAfterUpgrade = 0;
    // before downgrade
    public static int upCoveredBranches = 0;
    public static int upProbeNum = 0;
    // after downgrade
    public static int oriCoveredBranchesAfterDowngrade = 0;
    public static int oriProbeNumAfterDowngrade = 0;

    public static List<Pair<Integer, Integer>> oriBCAlongTime = new ArrayList<>(); // time:
    public static List<Pair<Integer, Integer>> upBCAlongTimeAfterUpgrade = new ArrayList<>(); // time:
    public static List<Pair<Integer, Integer>> upBCCoverageAlongTime = new ArrayList<>(); // time:
    public static List<Pair<Integer, Integer>> oriBCAlongTimeAfterDowngrade = new ArrayList<>(); // time:

    public static long lastTimePoint = 0;
    public BlockingQueue<StackedTestPacket> stackedTestPacketsQueueVersionDelta;
    public InterestingTestsCorpus testBatchCorpus;
    public long startTime;
    public long lastRoundIntroducingVersionDelta;

    // Execute a test in old version
    ExecutionDataStore curOriCoverage;
    // Coverage after upgrade to new version
    ExecutionDataStore curUpCoverageAfterUpgrade;

    // Execute a test in new version
    ExecutionDataStore curUpCoverage;
    // Coverage after downgrade to old version
    ExecutionDataStore curOriCoverageAfterDowngrade;

    TestTrackerGraph graph = new TestTrackerGraph();

    // Calculate cumulative probabilities
    double[] cumulativeSeedChoiceProbabilities = new double[6];
    double[] cumulativeTestChoiceProbabilities = new double[4];

    Set<Integer> mutatedSeedIds = new HashSet<>();
    Set<Integer> insignificantInconsistenciesIn = new HashSet<>();
    Map<Integer, Double> seedChoiceProbabilities;
    Map<Integer, Double> testChoiceProbabilities;

    List<Integer> versionDeltaInducedTpIds = new ArrayList<>();
    List<Integer> branchVersionDeltaInducedTpIds = new ArrayList<>();
    List<Integer> formatVersionDeltaInducedTpIds = new ArrayList<>();
    List<Integer> onlyNewBranchCoverageInducedTpIds = new ArrayList<>();
    List<Integer> onlyNewFormatCoverageInducedTpIds = new ArrayList<>();

    List<Integer> nonInterestingTpIds = new ArrayList<>();

    public FuzzingServer() {
        if (Config.getConf().useVersionDelta) {
            if (Config.getConf().versionDeltaApproach == 2) {
                corpus = new CorpusVersionDeltaSixQueue();
            } else {
                corpus = new CorpusVersionDeltaFourQueue();
            }
        } else {
            if (Config.getConf().useFormatCoverage)
                corpus = new CorpusNonVersionDelta();
            else
                corpus = new CorpusDefault();
        }
        testID2Seed = new HashMap<>();
        testID2TestPlan = new HashMap<>();
        stackedTestPackets = new LinkedList<>();
        fullStopPackets = new LinkedList<>();
        testPlanPackets = new LinkedList<>();
        curOriCoverage = new ExecutionDataStore();
        curUpCoverage = new ExecutionDataStore();
        curOriCoverageAfterDowngrade = new ExecutionDataStore();
        curUpCoverageAfterUpgrade = new ExecutionDataStore();

        if (Config.getConf().useFormatCoverage) {
            // FIXME: add isSerialized path
            // Now: only support testing upgrade!

            // Construct info folder:
            // there should 3 folder
            Path oriFormatInfoFolder = Paths.get("configInfo")
                    .resolve(Config.getConf().originalVersion);
            Path upFormatInfoFolder = Paths.get("configInfo")
                    .resolve(Config.getConf().upgradedVersion);

            // We will also have another folder to provide information between
            // versions

            if (!oriFormatInfoFolder.toFile().exists()
                    || !upFormatInfoFolder.toFile().exists()) {
                throw new RuntimeException(
                        "oriFormatInfoFolder or upFormatInfoFolder is not specified in the configuration file "
                                +
                                "while format coverage is enabled");
            }
            oriObjCoverage = new ObjectGraphCoverage(
                    oriFormatInfoFolder.resolve(
                            Config.getConf().baseClassInfoFileName),
                    oriFormatInfoFolder.resolve(
                            Config.getConf().topObjectsFileName),
                    oriFormatInfoFolder.resolve(
                            Config.getConf().comparableClassesFileName),
                    null,
                    null,
                    null);
            if (Config.getConf().useVersionDelta) {
                upObjCoverage = new ObjectGraphCoverage(
                        upFormatInfoFolder.resolve(
                                Config.getConf().baseClassInfoFileName),
                        upFormatInfoFolder.resolve(
                                Config.getConf().topObjectsFileName),
                        upFormatInfoFolder.resolve(
                                Config.getConf().comparableClassesFileName),
                        null,
                        null,
                        null);
            }
            Runtime.initWriter();
        }

        /*
         * ----- TODO? ------
         * Version delta coverage init
         */

        if (Config.getConf().testSingleVersion) {
            configDirPath = Paths.get(System.getProperty("user.dir"),
                    Config.getConf().configDir,
                    Config.getConf().originalVersion);
        } else {
            configDirPath = Paths.get(System.getProperty("user.dir"),
                    Config.getConf().configDir, Config.getConf().originalVersion
                            + "_" + Config.getConf().upgradedVersion);
        }

        startTime = TimeUnit.SECONDS.convert(System.nanoTime(),
                TimeUnit.NANOSECONDS);

    }

    private void init() {
        /**
         * Force GC every 10 minutes, using format coverage would incur a
         * large amount of objects which need to be GC quickly. Otherwise,
         * the memory usage could burst to 20+GB.
         */
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            System.gc();
            logger.info("[GC] Server Garbage Collection invoked");
        }, Config.getConf().gcInterval, Config.getConf().gcInterval,
                TimeUnit.MINUTES);

        if (Config.getConf().initSeedDir != null) {
            corpus.initCorpus(Paths.get(Config.getConf().initSeedDir));
        }
        stackedTestPacketsQueueVersionDelta = new LinkedBlockingQueue<>();
        testBatchCorpus = new InterestingTestsCorpus();

        // maintain the num of configuration files
        // read all configurations file name in a list
        if (Config.getConf().system.equals("cassandra")) {
            executor = new CassandraExecutor();
            commandPool = new CassandraCommandPool();
            stateClass = CassandraState.class;
            configGen = new CassandraConfigGen();
        } else if (Config.getConf().system.equals("hdfs")) {
            executor = new HdfsExecutor();
            commandPool = new HdfsCommandPool();
            stateClass = HdfsState.class;
            configGen = new HdfsConfigGen();
        } else if (Config.getConf().system.equals("hbase")) {
            executor = new HBaseExecutor();
            commandPool = new HBaseCommandPool();
            stateClass = HBaseState.class;
            configGen = new HBaseConfigGen();
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

        seedChoiceProbabilities = new HashMap<>();
        seedChoiceProbabilities.put(0,
                Config.getConf().formatDeltaSeedChoiceProb);
        seedChoiceProbabilities.put(1,
                Config.getConf().branchDeltaSeedChoiceProb);
        seedChoiceProbabilities.put(2,
                Config.getConf().formatCovSeedChoiceProb);
        seedChoiceProbabilities.put(3,
                Config.getConf().branchCovSeedChoiceProb);
        seedChoiceProbabilities.put(4,
                Config.getConf().branchCovAfterUpgSeedChoiceProb);
        seedChoiceProbabilities.put(5,
                Config.getConf().branchCovAfterDowngSeedChoiceProb);

        testChoiceProbabilities = new HashMap<>();
        testChoiceProbabilities.put(0,
                Config.getConf().formatVersionDeltaChoiceProb);
        testChoiceProbabilities.put(1,
                Config.getConf().branchVersionDeltaChoiceProb);
        testChoiceProbabilities.put(2,
                Config.getConf().formatCoverageChoiceProb);
        testChoiceProbabilities.put(3,
                Config.getConf().branchCoverageChoiceProb);

        cumulativeSeedChoiceProbabilities[0] = seedChoiceProbabilities.get(0);
        for (int i = 1; i < seedChoiceProbabilities.keySet().size(); i++) {
            cumulativeSeedChoiceProbabilities[i] = cumulativeSeedChoiceProbabilities[i
                    - 1]
                    + seedChoiceProbabilities.get(i);
        }

        cumulativeTestChoiceProbabilities[0] = testChoiceProbabilities.get(0);
        for (int i = 1; i < testChoiceProbabilities.keySet().size(); i++) {
            cumulativeTestChoiceProbabilities[i] = cumulativeTestChoiceProbabilities[i
                    - 1]
                    + testChoiceProbabilities.get(i);
        }
    }

    public void start() {
        init();
        new Thread(new FuzzingServerSocket(this)).start();
        // new Thread(new FuzzingServerDispatcher(this)).start();
    }

    public synchronized StackedTestPacket getOneBatch() {
        int randomIndex = rand.nextInt(testBatchCorpus.configFiles.size());
        String configFileName = testBatchCorpus
                .getConfigFileByIndex(randomIndex);
        StackedTestPacket stackedTestPacket = new StackedTestPacket(
                Config.getConf().nodeNum, configFileName);
        logger.info("[HKLOG] config file name: " + configFileName);
        for (int i = 0; i < (int) Config.getConf().batchSizeInGroup2; i++) {
            if (!testBatchCorpus
                    .noInterestingTestsForThisConfig(configFileName)) {
                int testTypeInt = getSeedOrTestType(
                        cumulativeTestChoiceProbabilities);
                if (!testBatchCorpus.intermediateBuffer[testTypeInt]
                        .containsKey(configFileName)) {
                    testTypeInt = getNextBestTestType(testChoiceProbabilities,
                            configFileName);
                }
                TestPacket testPacket = null;
                if (testTypeInt != -1) {
                    testPacket = testBatchCorpus.getPacket(
                            InterestingTestsCorpus.TestType
                                    .values()[testTypeInt],
                            configFileName);
                }
                if (testPacket != null) {
                    stackedTestPacket.addTestPacket(testPacket);
                }
            } else {
                TestPacket testPacket = testBatchCorpus.getPacket(
                        InterestingTestsCorpus.TestType.LOW_PRIORITY,
                        configFileName);
                try {
                    if (testPacket != null) {
                        stackedTestPacket
                                .addTestPacket(testPacket);
                    }
                } catch (Exception e) {
                    logger.debug(
                            "Not enough test packets in the buffer yet for this config, trying with a smaller batch in this execution");
                }
            }
        }
        if (testBatchCorpus.areAllQueuesEmptyForThisConfig(configFileName)) {
            testBatchCorpus.configFiles.remove(configFileName);
        }
        stackedTestPacket.curOriCoverage = curOriCoverage;
        stackedTestPacket.curUpCoverage = curUpCoverage;
        stackedTestPacket.curOriObjCoverage = oriObjCoverage;
        stackedTestPacket.curUpObjCoverage = upObjCoverage;
        stackedTestPacket.clientGroupForVersionDelta = 2;

        logger.info("[HKLOG] sending batch size to agent group 2: "
                + stackedTestPacket.getTestPacketList().size());
        return stackedTestPacket;
    }

    public synchronized Packet getOneTest() {
        if (Config.getConf().testingMode == 0) {
            if (stackedTestPackets.isEmpty()) {
                fuzzOne();
            }
            assert !stackedTestPackets.isEmpty();
            StackedTestPacket stackedTestPacket = stackedTestPackets.poll();
            if (Config.getConf().useVersionDelta
                    && (Config.getConf().versionDeltaApproach == 2)) {
                stackedTestPacket.clientGroupForVersionDelta = 1;
            }

            // Debug: use the fixed command
            if (Config.getConf().useFixedCommand) {
                if (fixedWriteCommands == null
                        || fixedValidationCommands == null) {
                    Path commandPath = Paths.get(System.getProperty("user.dir"),
                            "examplecase");
                    fixedWriteCommands = readCommands(
                            commandPath.resolve("commands.txt"));
                    fixedValidationCommands = readCommands(
                            commandPath.resolve("validcommands.txt"));
                }
                for (TestPacket tp : stackedTestPacket.getTestPacketList()) {
                    System.out.println(
                            "[Eval] use fixed commands from examplecase/commands.txt");
                    tp.originalCommandSequenceList = fixedWriteCommands;
                    tp.validationCommandSequenceList = fixedValidationCommands;
                }
            }
            return stackedTestPacket;
        } else if (Config.getConf().testingMode == 1) {
            // always execute one test case
            // to verify whether a bug exists
            return generateExampleFullStopPacket();
        } else if (Config.getConf().testingMode == 2) {
            return generateMixedTestPacket();
        } else if (Config.getConf().testingMode == 3) {
            logger.info("execute example test plan");
            return generateExampleTestplanPacket();
        } else if (Config.getConf().testingMode == 4) {
            // test full-stop and rolling upgrade iteratively
            Packet packet;
            if (isFullStopUpgrade
                    || (testPlanPackets.isEmpty() && !fuzzTestPlan())) {
                if (stackedTestPackets.isEmpty())
                    fuzzOne();
                assert !stackedTestPackets.isEmpty();
                packet = stackedTestPackets.poll();
            } else {
                if (testPlanPackets.isEmpty())
                    fuzzTestPlan();
                packet = testPlanPackets.poll();
                // TestPlanPacket tp = (TestPlanPacket) packet;
                // logger.info("val cmd size = "
                // + tp.testPlan.validationCommands.size());
                // logger.info("val oracle size = "
                // + tp.testPlan.validationReadResultsOracle.size());
            }
            isFullStopUpgrade = !isFullStopUpgrade;
            return packet;
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

        while (testPlanPackets.isEmpty())
            fuzzTestPlan();
        testPlanPacket = testPlanPackets.poll();

        return new MixedTestPacket(stackedTestPacket, testPlanPacket);

    }

    public static Seed getSeedVersionDelta(
            CorpusVersionDeltaSixQueue corpusVersionDeltaSixQueue,
            double[] cumulativeSeedChoiceProbabilities,
            Map<Integer, Double> seedChoiceProbabilities) {
        Seed seed = null;
        int corpusType = getSeedOrTestType(cumulativeSeedChoiceProbabilities);
        if ((Config.getConf().branchCovSeedChoiceProb > 0)
                || (Config.getConf().formatCovSeedChoiceProb > 0)
                || (Config.getConf().branchDeltaSeedChoiceProb > 0)
                || (Config.getConf().formatDeltaSeedChoiceProb > 0)
                || (Config.getConf().branchCovAfterUpgSeedChoiceProb > 0)
                || (Config.getConf().branchCovAfterDowngSeedChoiceProb > 0)) {
            if (Config.getConf().debug) {
                logger.debug("[HKLOG] Chosen seed of coverage type: "
                        + (corpusType == 2 ? "version delta" : "format/code"));
            }

            logger.info(corpusVersionDeltaSixQueue.getSize(
                    CorpusVersionDeltaSixQueue.QueueType.values()[corpusType]));
            if (corpusVersionDeltaSixQueue
                    .getSize(CorpusVersionDeltaSixQueue.QueueType
                            .values()[corpusType]) == 0
                    && !corpusVersionDeltaSixQueue.areAllQueuesEmpty()) {
                corpusType = getNextBestSeedType(corpusVersionDeltaSixQueue,
                        seedChoiceProbabilities);
            }
            seed = corpusVersionDeltaSixQueue.getSeed(
                    CorpusVersionDeltaSixQueue.QueueType.values()[corpusType]);
            if (seed != null) {
                // logger.info("From corpus type: " + corpusType
                // + ", obtained seed id: " + seed.testID);
                if (Config.getConf().debug) {
                    logger.debug(
                            "[HKLOG] Got seed id " + seed.testID
                                    + " from corpus type: "
                                    + corpusType);
                }
            }
        }
        return seed;
    }

    public void fuzzOne() {
        // Pick one test case from the corpus, fuzz it for mutationEpoch
        // Add the new tests into the stackedTestPackets
        // All packets have been dispatched, now fuzz next seed

        Seed seed = null;
        if (Config.getConf().useVersionDelta) {
            if (Config.getConf().versionDeltaApproach == 2) {
                seed = getSeedVersionDelta((CorpusVersionDeltaSixQueue) corpus,
                        cumulativeSeedChoiceProbabilities,
                        seedChoiceProbabilities);
            } else {
                seed = corpus.getSeed();
            }
        } else {
            // 95% to pick a seed, 5% generate a new one
            if (rand.nextDouble() < Config.getConf().getSeedFromCorpusRatio)
                seed = corpus.getSeed();
        }

        round++;
        StackedTestPacket stackedTestPacket;

        int configIdx = 0;
        if (seed == null) {
            logger.debug("[fuzzOne] generate a random seed");
            configIdx = configGen.generateConfig();
            String configFileName = "test" + configIdx;
            // corpus is empty, random generate one test packet and wait
            stackedTestPacket = new StackedTestPacket(
                    Config.getConf().nodeNum,
                    configFileName);
            if (Config.getConf().useVersionDelta
                    && Config.getConf().versionDeltaApproach == 2) {
                stackedTestPacket.clientGroupForVersionDelta = 1;
            }
            stackedTestPacket.setCurOriCoverage(curOriCoverage);
            stackedTestPacket.setCurUpCoverage(curUpCoverage);
            stackedTestPacket.setCurOriObjCoverage(oriObjCoverage);
            stackedTestPacket.setCurUpObjCoverage(upObjCoverage);
            for (int i = 0; i < Config.getConf().STACKED_TESTS_NUM; i++) {
                seed = Executor.generateSeed(commandPool, stateClass,
                        configIdx, testID);
                if (seed != null) {
                    mutatedSeedIds.add(testID);
                    graph.addNode(-1, seed); // random generate seed
                    testID2Seed.put(testID, seed);
                    stackedTestPacket.addTestPacket(seed, testID++);
                }
            }
            if (stackedTestPacket.size() == 0) {
                throw new RuntimeException(
                        "Fuzzing Server failed to generate and tests");
            }
            stackedTestPackets.add(stackedTestPacket);
        } else {
            mutatedSeedIds.add(seed.testID);
            logger.debug(
                    "[fuzzOne] fuzz a seed from corpus, stackedTestPackets size = "
                            + stackedTestPackets.size());
            /**
             *  Get a seed from corpus, now fuzz it for an epoch
             *  The seed contains a specific configuration to trigger new coverage
             *  1. Fix the config, mutate command sequences
             *      a. Mutate command sequences
             *      b. Random generate new command sequences
             *  2. Fix the command sequence
             *      a. Mutate the configs (not supported yet)
             *      b. Random generate new configs
             *  3. Mutate both config and command sequence (violent, disabled)
             */

            // 1.a Fix config, mutate command sequences
            if (seed.configIdx == -1)
                configIdx = configGen.generateConfig();
            else
                configIdx = seed.configIdx;

            int mutationEpoch;
            int randGenEpoch;
            if (firstMutationSeedNum < Config
                    .getConf().firstMutationSeedLimit) {
                mutationEpoch = Config.getConf().firstSequenceMutationEpoch;
                randGenEpoch = Config.getConf().firstSequenceRandGenEpoch;
            } else {
                mutationEpoch = Config.getConf().sequenceMutationEpoch;
                randGenEpoch = Config.getConf().sequenceRandGenEpoch;
            }

            if (Config.getConf().debug) {
                logger.debug(String.format(
                        "mutationEpoch = %s, firstMutationSeedNum = %s",
                        mutationEpoch, firstMutationSeedNum));
            }
            String configFileName = "test" + configIdx;
            stackedTestPacket = new StackedTestPacket(Config.getConf().nodeNum,
                    configFileName);
            if (Config.getConf().useVersionDelta
                    && Config.getConf().versionDeltaApproach == 2) {
                stackedTestPacket.clientGroupForVersionDelta = 1;
            }
            stackedTestPacket.setCurOriCoverage(curOriCoverage);
            stackedTestPacket.setCurUpCoverage(curUpCoverage);
            stackedTestPacket.setCurOriObjCoverage(oriObjCoverage);
            stackedTestPacket.setCurUpObjCoverage(upObjCoverage);
            for (int i = 0; i < mutationEpoch; i++) {
                if (i != 0 && i % Config.getConf().STACKED_TESTS_NUM == 0) {
                    stackedTestPackets.add(stackedTestPacket);
                    stackedTestPacket = new StackedTestPacket(
                            Config.getConf().nodeNum,
                            configFileName);
                    if (Config.getConf().useVersionDelta
                            && Config.getConf().versionDeltaApproach == 2) {
                        stackedTestPacket.clientGroupForVersionDelta = 1;
                    }
                    stackedTestPacket.setCurOriCoverage(curOriCoverage);
                    stackedTestPacket.setCurUpCoverage(curUpCoverage);
                    stackedTestPacket.setCurOriObjCoverage(oriObjCoverage);
                    stackedTestPacket.setCurUpObjCoverage(upObjCoverage);
                }
                Seed mutateSeed = SerializationUtils.clone(seed);
                if (mutateSeed.mutate(commandPool, stateClass)) {
                    // logger.info("[HKLOG] Mutated seed of id: " + testID);
                    // mutatedSeedIds.add(testID);
                    mutateSeed.testID = testID; // update testID after mutation
                    graph.addNode(seed.testID, mutateSeed);
                    testID2Seed.put(testID, mutateSeed);
                    stackedTestPacket.addTestPacket(mutateSeed, testID++);
                } else {
                    logger.debug("Mutation failed");
                    i--;
                }
            }
            // last test packet
            if (stackedTestPacket.size() != 0) {
                stackedTestPackets.add(stackedTestPacket);
            }
            // 1.b Fix config, random generate new command sequences
            stackedTestPacket = new StackedTestPacket(Config.getConf().nodeNum,
                    configFileName);
            for (int i = 0; i < randGenEpoch; i++) {
                if (i != 0 && i % Config.getConf().STACKED_TESTS_NUM == 0) {
                    stackedTestPackets.add(stackedTestPacket);
                    stackedTestPacket = new StackedTestPacket(
                            Config.getConf().nodeNum, configFileName);
                }
                Seed randGenSeed = Executor.generateSeed(commandPool,
                        stateClass,
                        configIdx, testID);
                if (randGenSeed != null) {
                    graph.addNode(seed.testID, randGenSeed);
                    testID2Seed.put(testID, randGenSeed);
                    stackedTestPacket.addTestPacket(randGenSeed, testID++);
                } else {
                    logger.debug("Random seed generation failed");
                    i--;
                }
            }
            // last test packet
            if (stackedTestPacket.size() != 0) {
                stackedTestPackets.add(stackedTestPacket);
            }

            if (configGen.enable && (!(Config.getConf().useVersionDelta)
                    || (round - lastRoundIntroducingVersionDelta > Config
                            .getConf().limitConfigForVersionDeltaRound))) {
                int configMutationEpoch;
                if (firstMutationSeedNum < Config
                        .getConf().firstMutationSeedLimit)
                    configMutationEpoch = Config
                            .getConf().firstConfigMutationEpoch;
                else
                    configMutationEpoch = Config.getConf().configMutationEpoch;

                for (int configMutationIdx = 0; configMutationIdx < configMutationEpoch; configMutationIdx++) {
                    configIdx = configGen.generateConfig();
                    configFileName = "test" + configIdx;
                    stackedTestPacket = new StackedTestPacket(
                            Config.getConf().nodeNum,
                            configFileName);
                    if (Config.getConf().useVersionDelta
                            && Config.getConf().versionDeltaApproach == 2) {
                        stackedTestPacket.clientGroupForVersionDelta = 1;
                    }
                    stackedTestPacket.setCurOriCoverage(curOriCoverage);
                    stackedTestPacket.setCurUpCoverage(curUpCoverage);
                    stackedTestPacket.setCurOriObjCoverage(oriObjCoverage);
                    stackedTestPacket.setCurUpObjCoverage(upObjCoverage);
                    // put the seed into it
                    Seed mutateSeed = SerializationUtils.clone(seed);
                    mutateSeed.configIdx = configIdx;
                    mutateSeed.testID = testID; // update testID after mutation
                    graph.addNode(seed.testID, mutateSeed);
                    testID2Seed.put(testID, mutateSeed);

                    /**
                     * We shouldn't add more tests for this batch, since it's only
                     * testing the configuration mutation, this batch would be 1.
                     * If we add more tests, actually we already think that this
                     * config is interesting, however, we shouldn't do that.
                     */
                    stackedTestPacket.addTestPacket(mutateSeed, testID++);
                    // add mutated seeds (Mutate sequence&config)
                    if (Config.getConf().paddingStackedTestPackets) {
                        for (int i = 1; i < Config
                                .getConf().STACKED_TESTS_NUM; i++) {
                            mutateSeed = SerializationUtils.clone(seed);
                            mutateSeed.configIdx = configIdx;
                            if (mutateSeed.mutate(commandPool, stateClass)) {
                                mutateSeed.testID = testID;
                                graph.addNode(seed.testID, mutateSeed);
                                testID2Seed.put(testID, mutateSeed);
                                stackedTestPacket.addTestPacket(mutateSeed,
                                        testID++);
                            } else {
                                logger.debug("Mutation failed");
                                i--;
                            }
                        }
                    }
                    stackedTestPackets.add(stackedTestPacket);
                }
            }
            firstMutationSeedNum++;
            logger.debug("[fuzzOne] mutate done, stackedTestPackets size = "
                    + stackedTestPackets.size());
        }
    }

    private boolean fuzzTestPlan() {
        // We should first try to mutate the test plan, but there
        // should still be possibility for generating a new test plan
        // Mutate a testplan
        TestPlan testPlan = testPlanCorpus.getTestPlan();

        if (testPlan != null) {
            for (int i = 0; i < Config.getConf().testPlanMutationEpoch; i++) {
                TestPlan mutateTestPlan = null;
                int j = 0;
                for (; j < Config.getConf().testPlanMutationRetry; j++) {
                    mutateTestPlan = SerializationUtils.clone(testPlan);
                    mutateTestPlan.mutate();
                    if (testPlanVerifier(mutateTestPlan.getEvents(),
                            testPlan.nodeNum)) {
                        break;
                    }
                }
                // Always failed mutating this test plan
                if (j == Config.getConf().testPlanMutationRetry)
                    return false;
                testID2TestPlan.put(testID, mutateTestPlan);

                int configIdx = configGen.generateConfig();
                String configFileName = "test" + configIdx;

                testPlanPackets.add(new TestPlanPacket(
                        Config.getConf().system,
                        testID++, configFileName, mutateTestPlan));
            }
        } else {
            FullStopSeed fullStopSeed = fullStopCorpus.getSeed();
            if (fullStopSeed == null) {
                // return false, cannot fuzz test plan
                return false;
            }

            // Generate several test plan...
            for (int i = 0; i < Config.getConf().testPlanGenerationNum; i++) {

                // FIXME: possible forever loop
                while ((testPlan = generateTestPlan(fullStopSeed)) == null)
                    ;

                testID2TestPlan.put(testID, testPlan);

                int configIdx = configGen.generateConfig();
                String configFileName = "test" + configIdx;

                testPlanPackets.add(new TestPlanPacket(
                        Config.getConf().system,
                        testID++, configFileName, testPlan));
            }
        }
        return true;
    }

    public boolean fuzzFullStopUpgrade() {
        FullStopSeed fullStopSeed = fullStopCorpus.getSeed();
        round++;
        if (fullStopSeed == null) {
            // corpus is empty, generate some
            int configIdx = configGen.generateConfig();
            String configFileName = "test" + configIdx;
            Seed seed = Executor.generateSeed(commandPool, stateClass,
                    configIdx, testID);
            if (seed != null) {

                FullStopUpgrade fullStopUpgrade = new FullStopUpgrade(
                        Config.getConf().nodeNum,
                        seed.originalCommandSequence.getCommandStringList(),
                        seed.validationCommandSequence.getCommandStringList(),
                        targetSystemStates);
                testID2Seed.put(testID, seed);
                fullStopPackets.add(new FullStopPacket(Config.getConf().system,
                        testID++, configFileName, fullStopUpgrade));
            } else {
                logger.error("Seed is null");
            }
        } else {
            // Get a full-stop seed, mutate it and create some new seeds
            Seed seed = fullStopSeed.seed;

            for (int i = 0; i < Config.getConf().sequenceMutationEpoch; i++) {
                Seed mutateSeed = SerializationUtils.clone(seed);
                if (mutateSeed.mutate(commandPool, stateClass)) {
                    FullStopUpgrade fullStopUpgrade = new FullStopUpgrade(
                            Config.getConf().nodeNum,
                            seed.originalCommandSequence.getCommandStringList(),
                            seed.validationCommandSequence
                                    .getCommandStringList(),
                            targetSystemStates);
                    testID2Seed.put(testID, mutateSeed);
                    int configIdx = configGen.generateConfig();
                    String configFileName = "test" + configIdx;
                    fullStopPackets
                            .add(new FullStopPacket(Config.getConf().system,
                                    testID++, configFileName,
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

    public FeedBack mergeCoverage2(FeedBack[] feedBacks, String type) {
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

    public FeedBack mergeCoverageVersionDeltaGroup1(FeedBack[] feedBacks) {
        FeedBack fb = new FeedBack();
        if (feedBacks == null) {
            return fb;
        }
        for (FeedBack feedBack : feedBacks) {
            if (feedBack.originalCodeCoverage != null)
                fb.originalCodeCoverage.merge(feedBack.originalCodeCoverage);
        }
        return fb;
    }

    public Packet generateExampleTestplanPacket() {
        // Modify configID for debugging
        int configIdx = configGen.generateConfig();
        String configFileName = "test" + configIdx;

        return new TestPlanPacket(
                Config.getConf().system, testID++, configFileName,
                generateExampleTestPlan());
    }

    public Packet generateExampleFullStopPacket() {
        Path commandPath = Paths.get(System.getProperty("user.dir"),
                "examplecase");
        List<String> commands = readCommands(
                commandPath.resolve("commands.txt"));
        List<String> validcommands = readCommands(
                commandPath.resolve("validcommands.txt"));
        // configuration path
        String configFileName = readConfigFileName(
                commandPath.resolve("configFileName.txt"));
        if (configFileName == null) {
            logger.info(String.format(
                    "File %s is empty or could not be read the config idx%n",
                    commandPath.resolve("configFileName.txt")));
            System.out.println("Use new configuration");
            int configIdx = configGen.generateConfig();
            configFileName = "test" + configIdx;
        } else {
            logger.info("Use specified configuration: " + configFileName);
        }

        Set<String> targetSystemStates = new HashSet<>();

        logger.info("commands size = " + commands.size());
        logger.info("validcommands size = " + validcommands.size());

        FullStopUpgrade fullStopUpgrade = new FullStopUpgrade(
                Config.getConf().nodeNum,
                commands,
                validcommands,
                targetSystemStates);
        // TODO: Change this to the configIdx you want to test

        FullStopPacket fullStopPacket = new FullStopPacket(
                Config.getConf().system,
                testID++, configFileName, fullStopUpgrade);
        logger.info("config path = " + configFileName);
        return fullStopPacket;
    }

    public TestPlan generateExampleTestPlan() {
        int nodeNum = Config.getConf().nodeNum;
        if (Config.getConf().system.equals("hdfs"))
            nodeNum = 4;

        List<Event> events = EventParser.construct();

        logger.debug("example test plan size = " + events.size());

        Set<String> targetSystemStates = new HashSet<>();
        Map<Integer, Map<String, String>> oracle = new HashMap<>();
        Path commandPath = Paths.get(System.getProperty("user.dir"),
                "examplecase");
        List<String> validcommands = readCommands(
                commandPath.resolve("validcommands.txt"));
        List<String> validationReadResultsOracle = new LinkedList<>();

        return new TestPlan(nodeNum, events, targetSystemStates,
                oracle, validcommands, validationReadResultsOracle);
    }

    public TestPlan generateTestPlan(FullStopSeed fullStopSeed) {
        // Some systems might have special requirements for
        // upgrade, like HDFS needs to upgrade NN.
        int nodeNum = fullStopSeed.nodeNum;

        if (Config.getConf().useExampleTestPlan)
            return constructExampleTestPlan(fullStopSeed, nodeNum);

        // -----------fault----------
        int faultNum = rand.nextInt(Config.getConf().faultMaxNum + 1);
        List<Pair<Fault, FaultRecover>> faultPairs = Fault
                .randomGenerateFaults(nodeNum, faultNum);

        List<Event> upgradeOps = new LinkedList<>();
        if (!Config.getConf().testSingleVersion
                && !Config.getConf().fullStopUpgradeWithFaults) {
            for (int i = 0; i < nodeNum; i++) {
                upgradeOps.add(new UpgradeOp(i));
            }
            if (Config.getConf().shuffleUpgradeOrder) {
                Collections.shuffle(upgradeOps);
            }
            // -----------downgrade----------
            if (Config.getConf().testDowngrade) {
                upgradeOps = addDowngrade(upgradeOps);
            }

            // -----------prepare----------
            if (Config.getConf().system.equals("hdfs")) {
                upgradeOps.add(0, new HDFSStopSNN());
            } else {
                // FIXME: Move prepare to the start up stage
                upgradeOps.add(0, new PrepareUpgrade());
            }
        }

        List<Event> upgradeOpAndFaults = interleaveFaultAndUpgradeOp(faultPairs,
                upgradeOps);

        if (!testPlanVerifier(upgradeOpAndFaults, nodeNum)) {
            return null;
        }

        // Randomly interleave the commands with the upgradeOp&faults
        List<Event> shellCommands = new LinkedList<>();
        if (fullStopSeed.seed != null)
            shellCommands = ShellCommand.seedWriteCmd2Events(fullStopSeed.seed);
        else
            logger.error("empty full stop seed");

        List<Event> events = interleaveWithOrder(upgradeOpAndFaults,
                shellCommands);

        if (!Config.getConf().testSingleVersion
                && !Config.getConf().fullStopUpgradeWithFaults)
            events.add(events.size(), new FinalizeUpgrade());

        return new TestPlan(nodeNum, events, targetSystemStates,
                fullStopSeed.targetSystemStateResults,
                fullStopSeed.seed.validationCommandSequence
                        .getCommandStringList(),
                fullStopSeed.validationReadResults);
    }

    public TestPlan constructExampleTestPlan(FullStopSeed fullStopSeed,
            int nodeNum) {
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
                fullStopSeed.targetSystemStateResults, new LinkedList<>(),
                new LinkedList<>());
    }

    public static boolean testPlanVerifier(List<Event> events, int nodeNum) {
        // check connection status to the seed node
        boolean[][] connection = new boolean[nodeNum][nodeNum];
        for (int i = 0; i < nodeNum; i++) {
            for (int j = 0; j < nodeNum; j++) {
                connection[i][j] = true;
            }
        }
        // Check the connection with the seed node
        for (Event event : events) {
            if (event instanceof IsolateFailure) {
                int nodeIdx = ((IsolateFailure) event).nodeIndex;
                for (int i = 0; i < nodeNum; i++) {
                    if (i != nodeIdx)
                        connection[i][nodeIdx] = false;
                }
                for (int i = 0; i < nodeNum; i++) {
                    if (i != nodeIdx)
                        connection[nodeIdx][i] = false;
                }
            } else if (event instanceof IsolateFailureRecover) {
                int nodeIdx = ((IsolateFailureRecover) event).nodeIndex;
                for (int i = 0; i < nodeNum; i++) {
                    if (i != nodeIdx)
                        connection[i][nodeIdx] = true;
                }
                for (int i = 0; i < nodeNum; i++) {
                    if (i != nodeIdx)
                        connection[nodeIdx][i] = true;
                }
            } else if (event instanceof LinkFailure) {
                int nodeIdx1 = ((LinkFailure) event).nodeIndex1;
                int nodeIdx2 = ((LinkFailure) event).nodeIndex2;
                connection[nodeIdx1][nodeIdx2] = false;
                connection[nodeIdx2][nodeIdx1] = false;
            } else if (event instanceof LinkFailureRecover) {
                int nodeIdx1 = ((LinkFailureRecover) event).nodeIndex1;
                int nodeIdx2 = ((LinkFailureRecover) event).nodeIndex2;
                connection[nodeIdx1][nodeIdx2] = true;
                connection[nodeIdx2][nodeIdx1] = true;
            } else if (event instanceof UpgradeOp
                    || event instanceof DowngradeOp
                    || event instanceof RestartFailure
                    || event instanceof NodeFailureRecover) {
                int nodeIdx;
                if (event instanceof UpgradeOp)
                    nodeIdx = ((UpgradeOp) event).nodeIndex;
                else if (event instanceof DowngradeOp)
                    nodeIdx = ((DowngradeOp) event).nodeIndex;
                else if (event instanceof RestartFailure)
                    nodeIdx = ((RestartFailure) event).nodeIndex;
                else
                    nodeIdx = ((NodeFailureRecover) event).nodeIndex;
                if (nodeIdx == 0)
                    continue;

                if (Config.getConf().system.equals("hdfs")
                        || Config.getConf().system.equals("cassandra")) {
                    // This could be removed if failover is implemented
                    if (!connection[nodeIdx][0])
                        return false;
                } else {
                    int connectedPeerNum = 0;
                    for (int i = 0; i < nodeNum; i++) {
                        if (i != nodeIdx) {
                            if (connection[nodeIdx][i]) {
                                connectedPeerNum++;
                            }
                        }
                    }
                    if (connectedPeerNum == 0) {
                        return false;
                    }
                }
            }
        }

        boolean isSeedAlive = true;
        // Cannot upgrade if seed node is down
        // Cannot execute commands if seed node is down
        // TODO: If we have failure mechanism, we can remove this check
        for (Event event : events) {
            if (event instanceof NodeFailure) {
                int nodeIdx = ((NodeFailure) event).nodeIndex;
                if (nodeIdx == 0)
                    isSeedAlive = false;
            } else if (event instanceof NodeFailureRecover) {
                int nodeIdx = ((NodeFailureRecover) event).nodeIndex;
                if (nodeIdx == 0) {
                    isSeedAlive = true;
                }
            } else if (event instanceof RestartFailure) {
                int nodeIdx = ((RestartFailure) event).nodeIndex;
                if (nodeIdx == 0) {
                    isSeedAlive = true;
                }
            } else if (event instanceof UpgradeOp) {
                int nodeIdx = ((UpgradeOp) event).nodeIndex;
                if (nodeIdx == 0) {
                    isSeedAlive = true;
                } else if (!isSeedAlive) {
                    return false;
                }
            } else if (event instanceof ShellCommand) {
                if (!Config.getConf().failureOver) {
                    if (!isSeedAlive)
                        return false;
                }
            }
        }

        // Check double failure injection (NodeFailure[0] -x-> LinkFailure[0])
        boolean[] nodeState = new boolean[nodeNum];
        for (int i = 0; i < nodeNum; i++)
            nodeState[i] = true;
        for (Event event : events) {
            if (event instanceof NodeFailure) {
                int nodeIdx = ((NodeFailure) event).nodeIndex;
                nodeState[nodeIdx] = false;
            } else if (event instanceof NodeFailureRecover) {
                int nodeIdx = ((NodeFailureRecover) event).nodeIndex;
                nodeState[nodeIdx] = true;
            } else if (event instanceof RestartFailure) {
                int nodeIdx = ((RestartFailure) event).nodeIndex;
                nodeState[nodeIdx] = true;
            } else if (event instanceof UpgradeOp) {
                int nodeIdx = ((UpgradeOp) event).nodeIndex;
                nodeState[nodeIdx] = true;
            } else if (event instanceof LinkFailure) {
                int nodeIdx1 = ((LinkFailure) event).nodeIndex1;
                int nodeIdx2 = ((LinkFailure) event).nodeIndex2;
                if (!nodeState[nodeIdx1] || !nodeState[nodeIdx2])
                    return false;
            } else if (event instanceof LinkFailureRecover) {
                int nodeIdx1 = ((LinkFailureRecover) event).nodeIndex1;
                int nodeIdx2 = ((LinkFailureRecover) event).nodeIndex2;
                if (!nodeState[nodeIdx1] || !nodeState[nodeIdx2])
                    return false;
            } else if (event instanceof IsolateFailure) {
                int nodeIdx = ((IsolateFailure) event).nodeIndex;
                if (!nodeState[nodeIdx]) {
                    return false;
                }
            } else if (event instanceof IsolateFailureRecover) {
                int nodeIdx = ((IsolateFailureRecover) event).nodeIndex;
                if (!nodeState[nodeIdx]) {
                    return false;
                }
            }
        }

        // hdfs specific, no restart failure between STOPSNN and UpgradeSNN
        if (Config.getConf().system.equals("hdfs")) {
            boolean metStopSNN = false;
            for (Event event : events) {
                if (event instanceof HDFSStopSNN) {
                    metStopSNN = true;
                } else if (event instanceof RestartFailure) {
                    int nodeIdx = ((RestartFailure) event).nodeIndex;
                    if (metStopSNN && nodeIdx == 1) {
                        return false;
                    }
                } else if (event instanceof UpgradeOp) {
                    int nodeIdx = ((UpgradeOp) event).nodeIndex;
                    if (nodeIdx == 1) {
                        // checked the process between STOPSNN and UpgradeSNN
                        break;
                    }
                }
            }
        }
        return true;
    }

    public static List<String> readCommands(Path path) {
        List<String> strings = new LinkedList<>();
        try {
            BufferedReader br = new BufferedReader(
                    new FileReader(path.toFile()));
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.isEmpty())
                    strings.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return strings;
    }

    public static String readConfigFileName(Path path) {
        String configFileName = readFirstLine(path);

        if (configFileName != null) {
            return configFileName;
        } else {
            return null;
        }
    }

    private static String readFirstLine(Path path) {
        try (BufferedReader br = new BufferedReader(
                new FileReader(path.toFile()))) {
            return br.readLine(); // Only read the first line
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
            return null;
        }
    }

    private void updateBCStatus() {
        updateBCStatusOri();
        updateBCStatusUpAfterUpgrade();
        updateBCStatusUp();
        updateBCStatusOriAfterDowngrade();
        // updateBCStatusAlongTime();
    }

    private void updateBCStatusOri() {
        Pair<Integer, Integer> coverageStatus = Utilities
                .getCoverageStatus(
                        curOriCoverage);
        oriCoveredBranches = coverageStatus.left;
        oriProbeNum = coverageStatus.right;
    }

    private void updateBCStatusUp() {
        Pair<Integer, Integer> coverageStatus = Utilities
                .getCoverageStatus(
                        curUpCoverage);
        upCoveredBranches = coverageStatus.left;
        upProbeNum = coverageStatus.right;
    }

    private void updateBCStatusOriAfterDowngrade() {
        Pair<Integer, Integer> coverageStatus = Utilities
                .getCoverageStatus(
                        curOriCoverageAfterDowngrade);
        oriCoveredBranchesAfterDowngrade = coverageStatus.left;
        oriProbeNumAfterDowngrade = coverageStatus.right;
    }

    private void updateBCStatusUpAfterUpgrade() {
        Pair<Integer, Integer> coverageStatus = Utilities
                .getCoverageStatus(
                        curUpCoverageAfterUpgrade);
        upCoveredBranchesAfterUpgrade = coverageStatus.left;
        upProbeNumAfterUpgrade = coverageStatus.right;
    }

    private void updateBCStatusAlongTime() {
        Long timeElapsed = TimeUnit.SECONDS.convert(
                System.nanoTime(), TimeUnit.NANOSECONDS) - startTime;
        if (timeElapsed - lastTimePoint > Config.getConf().timeInterval ||
                lastTimePoint == 0) {
            // Insert a record (time: coverage)
            oriBCAlongTime.add(
                    new Pair(timeElapsed, oriCoveredBranches));
            upBCAlongTimeAfterUpgrade.add(
                    new Pair(timeElapsed, upCoveredBranchesAfterUpgrade));
            upBCCoverageAlongTime.add(
                    new Pair(timeElapsed, upCoveredBranches));
            oriBCAlongTimeAfterDowngrade.add(
                    new Pair(timeElapsed, oriCoveredBranchesAfterDowngrade));
            lastTimePoint = timeElapsed;
        }
    }

    public synchronized void updateStatus(
            FullStopFeedbackPacket fullStopFeedbackPacket) {
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
        if ((Config.getConf().useBranchCoverage)
                && Utilities.hasNewBits(curOriCoverage,
                        fb.originalCodeCoverage)) {
            addToCorpus = true;
            curOriCoverage.merge(fb.originalCodeCoverage);
        }

        if ((Config.getConf().useBranchCoverage)
                && Utilities.hasNewBits(curUpCoverageAfterUpgrade,
                        fb.upgradedCodeCoverage)) {
            addToCorpus = true;
            curUpCoverageAfterUpgrade.merge(fb.upgradedCodeCoverage);
        }

        if (addToCorpus) {
            fullStopCorpus.addSeed(new FullStopSeed(
                    testID2Seed.get(fullStopFeedbackPacket.testPacketID),
                    Config.getConf().nodeNum,
                    fullStopFeedbackPacket.systemStates, new LinkedList<>()));
        }

        Path failureDir;
        if (fullStopFeedbackPacket.isUpgradeProcessFailed
                || fullStopFeedbackPacket.isInconsistent
                || fullStopFeedbackPacket.hasERRORLog) {
            failureDir = createFailureDir(
                    fullStopFeedbackPacket.configFileName);
            saveFullSequence(failureDir, fullStopFeedbackPacket.fullSequence);
            if (fullStopFeedbackPacket.isUpgradeProcessFailed) {
                saveFullStopCrashReport(failureDir,
                        fullStopFeedbackPacket.upgradeFailureReport,
                        fullStopFeedbackPacket.testPacketID);
            }
            if (fullStopFeedbackPacket.isInconsistent) {
                saveInconsistencyReport(failureDir,
                        fullStopFeedbackPacket.testPacketID,
                        fullStopFeedbackPacket.inconsistencyReport);
            }
            if (fullStopFeedbackPacket.hasERRORLog) {
                saveErrorReport(failureDir,
                        fullStopFeedbackPacket.errorLogReport,
                        fullStopFeedbackPacket.testPacketID);
            }
        }
        testID2Seed.remove(fullStopFeedbackPacket.testPacketID);

        finishedTestID++;
        printInfo();
        System.out.println();
    }

    public static void addSeedToCorpus(Corpus corpus,
            Seed seed, int score, boolean newOriBC,
            boolean newOriFC, boolean newBCAfterUpgrade) {
        seed.score = score;
        corpus.addSeed(seed, newOriBC, newOriFC, newBCAfterUpgrade);
    }

    public static void addSeedToCorpus(Corpus corpus,
            Seed seed, int score,
            boolean newOriBC, boolean newUpBC, boolean newOriFC,
            boolean newUpFC, boolean newBCAfterUpgrade,
            boolean newBCAfterDowngrade) {
        seed.score = score;
        corpus.addSeed(seed, newOriBC, newUpBC, newOriFC, newUpFC,
                newBCAfterUpgrade, newBCAfterDowngrade);
    }

    // Deprecated soon
    public static void addSeedToCorpus(CorpusVersionDeltaSixQueue corpus,
            Map<Integer, Seed> testID2Seed, int testPacketID,
            int score, CorpusVersionDeltaSixQueue.QueueType queueType) {
        Seed seed = testID2Seed.get(testPacketID);
        seed.score = score;
        assert testPacketID == seed.testID;
        logger.debug("Add seed " + testPacketID + " to queue " + queueType);
        corpus.addSeed(seed, queueType);
    }

    public synchronized void updateStatus(
            TestPlanFeedbackPacket testPlanFeedbackPacket) {

        FeedBack fb = mergeCoverage(testPlanFeedbackPacket.feedBacks);
        boolean addToCorpus = false;
        if (Config.getConf().useBranchCoverage) {
            if (Utilities.hasNewBits(curOriCoverage,
                    fb.originalCodeCoverage)) {
                addToCorpus = true;
                curOriCoverage.merge(fb.originalCodeCoverage);
            }
            if (Utilities.hasNewBits(curUpCoverageAfterUpgrade,
                    fb.upgradedCodeCoverage)) {
                addToCorpus = true;
                curUpCoverageAfterUpgrade.merge(fb.upgradedCodeCoverage);
            }
            if (addToCorpus) {
                testPlanCorpus.addTestPlan(
                        testID2TestPlan
                                .get(testPlanFeedbackPacket.testPacketID));
            }

        }

        Path failureDir;
        if (testPlanFeedbackPacket.isEventFailed
                || testPlanFeedbackPacket.isInconsistent
                || testPlanFeedbackPacket.hasERRORLog) {
            failureDir = createFailureDir(
                    testPlanFeedbackPacket.configFileName);
            saveFullSequence(failureDir, testPlanFeedbackPacket.fullSequence);
            if (testPlanFeedbackPacket.isEventFailed) {
                saveEventCrashReport(failureDir,
                        testPlanFeedbackPacket.testPacketID,
                        testPlanFeedbackPacket.eventFailedReport);
            }
            if (testPlanFeedbackPacket.isInconsistent) {
                saveInconsistencyReport(failureDir,
                        testPlanFeedbackPacket.testPacketID,
                        testPlanFeedbackPacket.inconsistencyReport);
            }
            if (testPlanFeedbackPacket.hasERRORLog) {
                saveErrorReport(failureDir,
                        testPlanFeedbackPacket.errorLogReport,
                        testPlanFeedbackPacket.testPacketID);
            }
        }
        testID2TestPlan.remove(testPlanFeedbackPacket.testPacketID);

        finishedTestID++;
        printInfo();
        System.out.println();
    }

    public synchronized void updateStatus(
            StackedFeedbackPacket stackedFeedbackPacket) {

        if (stackedFeedbackPacket.skipped) {
            // upgrade process is skipped
            logger.info("upgrade process is skipped");
        }

        Path failureDir = null;

        int startTestID = 0;
        int endTestID = 0;
        if (stackedFeedbackPacket.getFpList().size() > 0) {
            startTestID = stackedFeedbackPacket.getFpList().get(0).testPacketID;
            endTestID = stackedFeedbackPacket.getFpList()
                    .get(stackedFeedbackPacket.getFpList().size()
                            - 1).testPacketID;
        }

        if (stackedFeedbackPacket.isUpgradeProcessFailed) {
            failureDir = createFailureDir(stackedFeedbackPacket.configFileName);
            saveFullSequence(failureDir, stackedFeedbackPacket.fullSequence);
            saveFullStopCrashReport(failureDir,
                    stackedFeedbackPacket.upgradeFailureReport, startTestID,
                    endTestID);
            finishedTestID++;
        }
        FuzzingServerHandler.printClientNum();
        for (FeedbackPacket feedbackPacket : stackedFeedbackPacket
                .getFpList()) {
            finishedTestID++;
            int score = 0;

            boolean newOldVersionBranchCoverage = false;
            boolean newNewVersionBranchCoverage = false;
            boolean newFormatCoverage = false;

            // Merge all the feedbacks
            FeedBack fb = mergeCoverage(feedbackPacket.feedBacks);
            if (Config.getConf().debugCoverage) {
                if (Utilities.hasNewBitsDebug(
                        curOriCoverage,
                        fb.originalCodeCoverage)) {
                    // Write Seed to Disk + Add to Corpus
                    curOriCoverage.merge(
                            fb.originalCodeCoverage);
                    newOldVersionBranchCoverage = true;
                }
                if (Utilities.hasNewBitsDebug(curUpCoverageAfterUpgrade,
                        fb.upgradedCodeCoverage)) {
                    curUpCoverageAfterUpgrade.merge(
                            fb.upgradedCodeCoverage);
                    newNewVersionBranchCoverage = true;
                }
            } else {
                if (Utilities.hasNewBits(
                        curOriCoverage,
                        fb.originalCodeCoverage)) {
                    // Write Seed to Disk + Add to Corpus
                    curOriCoverage.merge(
                            fb.originalCodeCoverage);
                    newOldVersionBranchCoverage = true;
                }
                if (Utilities.hasNewBits(curUpCoverageAfterUpgrade,
                        fb.upgradedCodeCoverage)) {
                    curUpCoverageAfterUpgrade.merge(
                            fb.upgradedCodeCoverage);
                    newNewVersionBranchCoverage = true;
                }
            }

            // format coverage
            if (Config.getConf().useFormatCoverage) {
                if (feedbackPacket.formatCoverage != null) {
                    if (oriObjCoverage.merge(feedbackPacket.formatCoverage,
                            feedbackPacket.testPacketID)) {
                        // learned format is updated
                        logger.info("New format coverage for test "
                                + feedbackPacket.testPacketID);
                        oriNewFormatNum++;
                        newFormatCoverage = true;
                    }
                } else {
                    logger.info("Null format coverage");
                }
            }

            addSeedToCorpus(corpus,
                    testID2Seed.get(feedbackPacket.testPacketID),
                    score, newOldVersionBranchCoverage,
                    newFormatCoverage, newNewVersionBranchCoverage);

            graph.updateNodeCoverage(feedbackPacket.testPacketID,
                    newOldVersionBranchCoverage, newNewVersionBranchCoverage,
                    newFormatCoverage);

            if (feedbackPacket.isInconsistent) {
                if (failureDir == null) {
                    failureDir = createFailureDir(
                            stackedFeedbackPacket.configFileName);
                    saveFullSequence(failureDir,
                            stackedFeedbackPacket.fullSequence);
                }
                saveInconsistencyReport(failureDir,
                        feedbackPacket.testPacketID,
                        feedbackPacket.inconsistencyReport);
            }
        }
        // update testid2Seed, no use anymore
        for (int testID : stackedFeedbackPacket.testIDs) {
            testID2Seed.remove(testID);
        }
        if (stackedFeedbackPacket.hasERRORLog) {
            if (failureDir == null) {
                failureDir = createFailureDir(
                        stackedFeedbackPacket.configFileName);
                saveFullSequence(failureDir,
                        stackedFeedbackPacket.fullSequence);
            }
            saveErrorReport(failureDir,
                    stackedFeedbackPacket.errorLogReport, startTestID,
                    endTestID);
        }
        printInfo();
        System.out.println();
    }

    // Used for single group mechanism: always upgrade
    public synchronized void analyzeFeedbackFromVersionDelta(
            VersionDeltaFeedbackPacketApproach1 versionDeltaFeedbackPacket) {
        logger.debug("analyzeFeedbackFromVersionDelta");
        Path failureDir = null;

        int startTestID = 0;
        int endTestID = 0;

        List<FeedbackPacket> versionDeltaFeedbackPacketsUp = versionDeltaFeedbackPacket.stackedFeedbackPacketUpgrade.fpList;
        List<FeedbackPacket> versionDeltaFeedbackPacketsDown = versionDeltaFeedbackPacket.stackedFeedbackPacketDowngrade.fpList;
        String configFileName = versionDeltaFeedbackPacket.stackedFeedbackPacketUpgrade.configFileName;
        String fullSequence = versionDeltaFeedbackPacket.stackedFeedbackPacketUpgrade.fullSequence;

        if (versionDeltaFeedbackPacketsUp.size() > 0) {
            startTestID = versionDeltaFeedbackPacketsUp
                    .get(0).testPacketID;
            endTestID = versionDeltaFeedbackPacketsUp
                    .get(versionDeltaFeedbackPacketsUp.size()
                            - 1).testPacketID;
        }

        if (versionDeltaFeedbackPacket.stackedFeedbackPacketUpgrade.isUpgradeProcessFailed) {
            failureDir = createFailureDir(
                    configFileName);
            saveFullSequence(failureDir, fullSequence);
            saveFullStopCrashReport(failureDir,
                    versionDeltaFeedbackPacket.stackedFeedbackPacketUpgrade.upgradeFailureReport,
                    startTestID,
                    endTestID);
            finishedTestID++;
            finishedTestIdAgentGroup2++;
        }

        if (versionDeltaFeedbackPacket.stackedFeedbackPacketDowngrade.isDowngradeProcessFailed) {
            failureDir = createFailureDir(configFileName);
            saveFullSequence(failureDir, fullSequence);
            saveFullStopCrashReport(failureDir,
                    versionDeltaFeedbackPacket.stackedFeedbackPacketDowngrade.downgradeFailureReport,
                    startTestID,
                    endTestID);
            finishedTestID++;
            finishedTestIdAgentGroup2++;
        }
        FuzzingServerHandler.printClientNum();

        for (FeedbackPacket fp : versionDeltaFeedbackPacketsUp) {
            if (fp.isInconsistencyInsignificant) {
                insignificantInconsistenciesIn.add(fp.testPacketID);
            }
        }
        for (FeedbackPacket fp : versionDeltaFeedbackPacketsDown) {
            if (fp.isInconsistencyInsignificant) {
                insignificantInconsistenciesIn.add(fp.testPacketID);
            }
        }

        int feedbackLength = versionDeltaFeedbackPacketsUp.size();
        System.out.println("feedback length: " + feedbackLength);

        for (int i = 0; i < feedbackLength; i++) {
            FeedbackPacket versionDeltaFeedbackPacketUp = versionDeltaFeedbackPacketsUp
                    .get(i);
            FeedbackPacket versionDeltaFeedbackPacketDown = versionDeltaFeedbackPacketsDown
                    .get(i);
            assert versionDeltaFeedbackPacketUp.testPacketID == versionDeltaFeedbackPacketDown.testPacketID;
            int testPacketID = versionDeltaFeedbackPacketUp.testPacketID;

            finishedTestID++;
            finishedTestIdAgentGroup2++;
            int score = 0;

            // Branch coverage
            boolean newOriBC = false;
            boolean newUpBCAfterUpgrade = false;

            boolean newUpBC = false;
            boolean newOriBCAfterDowngrade = false;
            // Format coverage
            boolean newOriFC = false;
            boolean newUpFC = false;

            // Merge all the feedbacks
            FeedBack fbUpgrade = mergeCoverage(
                    versionDeltaFeedbackPacketUp.feedBacks);
            FeedBack fbDowngrade = mergeCoverage(
                    versionDeltaFeedbackPacketDown.feedBacks);

            if (Utilities.hasNewBits(
                    curOriCoverage,
                    fbUpgrade.originalCodeCoverage)) {
                curOriCoverage.merge(fbUpgrade.originalCodeCoverage);
                newOriBC = true;
            }
            if (Utilities.hasNewBits(
                    curUpCoverage,
                    fbDowngrade.originalCodeCoverage)) {
                curUpCoverage.merge(fbDowngrade.originalCodeCoverage);
                newUpBC = true;
            }
            if (Utilities.hasNewBits(
                    curUpCoverageAfterUpgrade,
                    fbUpgrade.upgradedCodeCoverage)) {
                curUpCoverageAfterUpgrade.merge(
                        fbUpgrade.upgradedCodeCoverage);
                newUpBCAfterUpgrade = true;
            }
            if (Utilities.hasNewBits(curOriCoverageAfterDowngrade,
                    fbDowngrade.downgradedCodeCoverage)) {
                curOriCoverageAfterDowngrade.merge(
                        fbDowngrade.downgradedCodeCoverage);
                newOriBCAfterDowngrade = true;
            }
            if (Config.getConf().useFormatCoverage) {
                if (versionDeltaFeedbackPacketUp.formatCoverage != null) {
                    if (oriObjCoverage.merge(
                            versionDeltaFeedbackPacketUp.formatCoverage,
                            testPacketID)) {
                        // learned format is updated
                        logger.info("New format coverage for test "
                                + testPacketID);
                        oriNewFormatNum++;
                        newOriFC = true;
                    }
                } else {
                    logger.info("Null format coverage");
                }
                if (versionDeltaFeedbackPacketDown.formatCoverage != null) {
                    if (upObjCoverage.merge(
                            versionDeltaFeedbackPacketDown.formatCoverage,
                            testPacketID)) {
                        // learned format is updated
                        logger.info("New format coverage for test "
                                + testPacketID);
                        upNewFormatNum++;
                        newUpFC = true;
                    }
                } else {
                    logger.info("Null format coverage");
                }
            }

            // Update graph
            graph.updateNodeCoverage(testPacketID,
                    newOriBC, newUpBCAfterUpgrade, newUpBC,
                    newOriBCAfterDowngrade, newOriFC, newUpFC);

            if (versionDeltaFeedbackPacketUp.isInconsistent) {
                if (failureDir == null) {
                    failureDir = createFailureDir(
                            configFileName);
                    saveFullSequence(failureDir,
                            fullSequence);
                }
                saveInconsistencyReport(failureDir,
                        testPacketID,
                        versionDeltaFeedbackPacketUp.inconsistencyReport);
            }

            addSeedToCorpus(corpus, testID2Seed.get(testPacketID),
                    score, newOriBC, newUpBC, newOriFC, newUpFC,
                    newUpBCAfterUpgrade, newOriBCAfterDowngrade);
        }
        // update testid2Seed, no use anymore
        for (int testID : versionDeltaFeedbackPacket.stackedFeedbackPacketUpgrade.testIDs) {
            testID2Seed.remove(testID);
        }

        // process upgrade failure report
        if (versionDeltaFeedbackPacket.stackedFeedbackPacketUpgrade.hasERRORLog) {
            if (failureDir == null) {
                failureDir = createFailureDir(configFileName);
                saveFullSequence(failureDir, fullSequence);
            }
            saveErrorReport(failureDir,
                    versionDeltaFeedbackPacket.stackedFeedbackPacketUpgrade.errorLogReport,
                    startTestID,
                    endTestID, true);
        }
        if (versionDeltaFeedbackPacket.stackedFeedbackPacketDowngrade.hasERRORLog) {
            if (failureDir == null) {
                failureDir = createFailureDir(configFileName);
                saveFullSequence(failureDir, fullSequence);
            }
            saveErrorReport(failureDir,
                    versionDeltaFeedbackPacket.stackedFeedbackPacketDowngrade.errorLogReport,
                    startTestID,
                    endTestID, false);
        }
        logger.debug(
                "analyzeFeedbackFromVersionDelta done, prepare to printInfo");
        printInfo();
        System.out.println();
    }

    public synchronized void analyzeFeedbackFromVersionDeltaGroup1(
            VersionDeltaFeedbackPacketApproach2 versionDeltaFeedbackPacket) {
        int startTestID = 0;
        int endTestID = 0;
        if (versionDeltaFeedbackPacket.stackedFeedbackPacketUpgrade.getFpList()
                .size() > 0) {
            startTestID = versionDeltaFeedbackPacket.stackedFeedbackPacketUpgrade
                    .getFpList()
                    .get(0).testPacketID;
            endTestID = versionDeltaFeedbackPacket.stackedFeedbackPacketUpgrade
                    .getFpList()
                    .get(versionDeltaFeedbackPacket.stackedFeedbackPacketUpgrade
                            .getFpList().size()
                            - 1).testPacketID;
        }

        FuzzingServerHandler.printClientNum();

        List<FeedbackPacket> versionDeltaFeedbackPacketsUp = versionDeltaFeedbackPacket.stackedFeedbackPacketUpgrade
                .getFpList();
        List<FeedbackPacket> versionDeltaFeedbackPacketsDown = versionDeltaFeedbackPacket.stackedFeedbackPacketDowngrade
                .getFpList();

        int feedbackLength = versionDeltaFeedbackPacketsUp.size();
        System.out.println("feedback length: " + feedbackLength);

        Path failureDir = null;

        for (int i = 0; i < feedbackLength; i++) {
            // handle invariant
            TestPacket testPacket = versionDeltaFeedbackPacket.tpList.get(i);
            FeedbackPacket versionDeltaFeedbackPacketUp = versionDeltaFeedbackPacketsUp
                    .get(i);
            FeedbackPacket versionDeltaFeedbackPacketDown = versionDeltaFeedbackPacketsDown
                    .get(i);
            finishedTestID++;
            finishedTestIdAgentGroup1++;
            int score = 0;

            boolean addToCorpus = false;
            boolean addToFormatCoverageCorpus = false;
            boolean addToVersionDeltaCorpusForBranchCoverage = false;
            boolean addToVersionDeltaCorpusForFormatCoverage = false;

            boolean newOldVersionBranchCoverageBeforeUpgrade = false;
            boolean newNewVersionBranchCoverageBeforeDowngrade = false;
            boolean oriNewFormat = false;
            boolean upNewFormat = false;

            // Merge all the feedbacks
            FeedBack fbUpgrade = mergeCoverage(
                    versionDeltaFeedbackPacketUp.feedBacks);
            FeedBack fbDowngrade = mergeCoverage(
                    versionDeltaFeedbackPacketDown.feedBacks);

            // priority feature is disabled
            // logger.info("Checking new bits for upgrade feedback");
            if (Utilities.hasNewBits(
                    curOriCoverage,
                    fbUpgrade.originalCodeCoverage)) {
                // Write Seed to Disk + Add to Corpus
                curOriCoverage.merge(
                        fbUpgrade.originalCodeCoverage);
                newOldVersionBranchCoverageBeforeUpgrade = true;
            }
            // logger.info("Checking new bits for downgrade feedback");
            if (Utilities.hasNewBits(curUpCoverage,
                    fbDowngrade.originalCodeCoverage)) {
                curUpCoverage.merge(
                        fbDowngrade.originalCodeCoverage);
                newNewVersionBranchCoverageBeforeDowngrade = true;
            }

            // format coverage
            if (Config.getConf().useFormatCoverage) {
                if (oriObjCoverage.merge(
                        versionDeltaFeedbackPacketUp.formatCoverage,
                        versionDeltaFeedbackPacketUp.testPacketID)) {
                    // learned format is updated
                    logger.info("oriNewFormatNum: "
                            + versionDeltaFeedbackPacketUp.testPacketID);
                    oriNewFormatNum++;
                    oriNewFormat = true;
                }
                if (upObjCoverage.merge(
                        versionDeltaFeedbackPacketDown.formatCoverage,
                        versionDeltaFeedbackPacketDown.testPacketID)) {
                    // learned format is updated
                    logger.info("upNewFormat: "
                            + versionDeltaFeedbackPacketUp.testPacketID);
                    upNewFormatNum++;
                    upNewFormat = true;
                }
            }

            if (Config.getConf().debug) {
                logger.info("[HKLOG] newOldVersionBranchCoverageBeforeUpgrade: "
                        + newOldVersionBranchCoverageBeforeUpgrade);
                logger.info(
                        "[HKLOG] newNewVersionBranchCoverageBeforeDowngrade: "
                                + newNewVersionBranchCoverageBeforeDowngrade);
                logger.info("[HKLOG] oriNewFormat: "
                        + oriNewFormat);
                logger.info("[HKLOG] upNewFormat: "
                        + upNewFormat);
            }

            boolean hasFeedbackInducedBranchVersionDelta = newOldVersionBranchCoverageBeforeUpgrade
                    ^ newNewVersionBranchCoverageBeforeDowngrade;
            boolean hasFeedbackInducedFormatVersionDelta = oriNewFormat
                    ^ upNewFormat;
            boolean hasFeedbackInducedNewBranchCoverage = newOldVersionBranchCoverageBeforeUpgrade
                    || newNewVersionBranchCoverageBeforeDowngrade;
            boolean hasFeedbackInducedNewFormatCoverage = oriNewFormat
                    || upNewFormat;
            boolean isFeedbackInteresting = hasFeedbackInducedNewBranchCoverage
                    || hasFeedbackInducedNewFormatCoverage;

            if (Config.getConf().useBranchCoverage
                    && (Config.getConf().branchCoverageChoiceProb > 0)) {
                if (hasFeedbackInducedBranchVersionDelta) {
                    logger.info("Adding to code coverage corpus: "
                            + versionDeltaFeedbackPacketUp.testPacketID);
                    logger.info(
                            "new old version branch coverage before upgrade: "
                                    + newOldVersionBranchCoverageBeforeUpgrade);
                    logger.info(
                            "new new version branch coverage before downgrade: "
                                    + newNewVersionBranchCoverageBeforeDowngrade);
                    addToVersionDeltaCorpusForBranchCoverage = true;
                    newVersionDeltaCountForBranchCoverage += 1;
                } else if (hasFeedbackInducedNewBranchCoverage) {
                    addToCorpus = true;
                    executionCountWithOnlyNewBranchCoverage += 1;
                }
            }

            if (Config.getConf().useFormatCoverage
                    && (Config.getConf().formatCoverageChoiceProb > 0)) {
                if (hasFeedbackInducedFormatVersionDelta) {
                    logger.debug("Add test "
                            + versionDeltaFeedbackPacketUp.testPacketID
                            + " to format coverage corpus " + "oriNewFormat = "
                            + oriNewFormat + " upNewFormat = " + upNewFormat);
                    addToVersionDeltaCorpusForFormatCoverage = true;
                    newVersionDeltaCountForFormatCoverage += 1;
                } else if (hasFeedbackInducedNewFormatCoverage) {
                    addToFormatCoverageCorpus = true;
                    executionCountWithOnlyNewFormatCoverage += 1;
                }
            }

            if (hasFeedbackInducedBranchVersionDelta
                    ^ hasFeedbackInducedFormatVersionDelta) {
                if (hasFeedbackInducedBranchVersionDelta) {
                    testBatchCorpus.addPacket(testPacket,
                            InterestingTestsCorpus.TestType.BRANCH_COVERAGE_VERSION_DELTA,
                            versionDeltaFeedbackPacket.stackedFeedbackPacketUpgrade.configFileName);
                    branchVersionDeltaInducedTpIds
                            .add(versionDeltaFeedbackPacket.tpList
                                    .get(i).testPacketID);
                } else {
                    testBatchCorpus.addPacket(testPacket,
                            InterestingTestsCorpus.TestType.FORMAT_COVERAGE_VERSION_DELTA,
                            versionDeltaFeedbackPacket.stackedFeedbackPacketUpgrade.configFileName);
                    formatVersionDeltaInducedTpIds
                            .add(versionDeltaFeedbackPacket.tpList
                                    .get(i).testPacketID);
                }
            } else {
                if (hasFeedbackInducedBranchVersionDelta
                        && hasFeedbackInducedFormatVersionDelta) {
                    testBatchCorpus.addPacket(testPacket,
                            InterestingTestsCorpus.TestType.FORMAT_COVERAGE_VERSION_DELTA,
                            versionDeltaFeedbackPacket.stackedFeedbackPacketUpgrade.configFileName);
                    versionDeltaInducedTpIds
                            .add(versionDeltaFeedbackPacket.tpList
                                    .get(i).testPacketID);
                } else {
                    if (hasFeedbackInducedNewBranchCoverage
                            ^ hasFeedbackInducedNewFormatCoverage) {
                        if (hasFeedbackInducedNewFormatCoverage) {
                            testBatchCorpus.addPacket(testPacket,
                                    InterestingTestsCorpus.TestType.FORMAT_COVERAGE,
                                    versionDeltaFeedbackPacket.stackedFeedbackPacketUpgrade.configFileName);
                            onlyNewFormatCoverageInducedTpIds.add(
                                    versionDeltaFeedbackPacket.tpList
                                            .get(i).testPacketID);
                        } else {
                            testBatchCorpus.addPacket(testPacket,
                                    InterestingTestsCorpus.TestType.BRANCH_COVERAGE_BEFORE_VERSION_CHANGE,
                                    versionDeltaFeedbackPacket.stackedFeedbackPacketDowngrade.configFileName);
                            onlyNewBranchCoverageInducedTpIds.add(
                                    versionDeltaFeedbackPacket.tpList
                                            .get(i).testPacketID);
                        }
                    } else {
                        if (hasFeedbackInducedNewBranchCoverage
                                && hasFeedbackInducedNewFormatCoverage) {
                            testBatchCorpus.addPacket(testPacket,
                                    InterestingTestsCorpus.TestType.FORMAT_COVERAGE,
                                    versionDeltaFeedbackPacket.stackedFeedbackPacketDowngrade.configFileName);
                            onlyNewFormatCoverageInducedTpIds.add(
                                    versionDeltaFeedbackPacket.tpList
                                            .get(i).testPacketID);
                            onlyNewBranchCoverageInducedTpIds.add(
                                    versionDeltaFeedbackPacket.tpList
                                            .get(i).testPacketID);
                        } else {
                            if (addNonInterestingTestsToBuffer(
                                    rand.nextDouble(),
                                    Config.getConf().nonInterestingTestsUpgradeProb)) {
                                logger.info("non interesting test packet "
                                        + testPacket.testPacketID
                                        + " chosen to be upgraded");
                                testBatchCorpus.addPacket(testPacket,
                                        InterestingTestsCorpus.TestType.LOW_PRIORITY,
                                        versionDeltaFeedbackPacket.stackedFeedbackPacketDowngrade.configFileName);
                            } else {
                                logger.info("non interesting test packet "
                                        + testPacket.testPacketID
                                        + " will not be upgraded");
                            }
                            nonInterestingTpIds.add(
                                    versionDeltaFeedbackPacket.tpList
                                            .get(i).testPacketID);
                        }
                    }
                }
            }

            System.out.println(versionDeltaFeedbackPacketUp.testPacketID);
            graph.updateNodeCoverage(versionDeltaFeedbackPacketUp.testPacketID,
                    newOldVersionBranchCoverageBeforeUpgrade,
                    false,
                    newNewVersionBranchCoverageBeforeDowngrade,
                    false,
                    oriNewFormat, upNewFormat);

            if (addToCorpus) {
                addSeedToCorpus((CorpusVersionDeltaSixQueue) corpus,
                        testID2Seed,
                        versionDeltaFeedbackPacketUp.testPacketID, score,
                        CorpusVersionDeltaSixQueue.QueueType.BC);
            }
            if (addToFormatCoverageCorpus) {
                addSeedToCorpus((CorpusVersionDeltaSixQueue) corpus,
                        testID2Seed,
                        versionDeltaFeedbackPacketUp.testPacketID, score,
                        CorpusVersionDeltaSixQueue.QueueType.FC);
            }
            if (addToVersionDeltaCorpusForBranchCoverage) {
                addSeedToCorpus((CorpusVersionDeltaSixQueue) corpus,
                        testID2Seed,
                        versionDeltaFeedbackPacketUp.testPacketID, score,
                        CorpusVersionDeltaSixQueue.QueueType.BC_VD);
            }
            if (addToVersionDeltaCorpusForFormatCoverage) {
                addSeedToCorpus((CorpusVersionDeltaSixQueue) corpus,
                        testID2Seed,
                        versionDeltaFeedbackPacketUp.testPacketID, score,
                        CorpusVersionDeltaSixQueue.QueueType.FC_VD);
            }
        }
        // update testid2Seed, no use anymore
        // for (int testID : versionDeltaFeedbackPacket.testIDs) {
        // if (!versionDeltaInducedTpIds.contains(testID)) {
        // testID2Seed.remove(testID);
        // }
        // }

        if (versionDeltaFeedbackPacket.stackedFeedbackPacketUpgrade.hasERRORLog) {
            if (failureDir == null) {
                failureDir = createFailureDir(
                        versionDeltaFeedbackPacket.stackedFeedbackPacketUpgrade.configFileName);
                saveFullSequence(failureDir,
                        versionDeltaFeedbackPacket.stackedFeedbackPacketUpgrade.fullSequence);
            }
            saveErrorReport(failureDir,
                    versionDeltaFeedbackPacket.stackedFeedbackPacketUpgrade.errorLogReport,
                    startTestID,
                    endTestID, true);
        }

        if (versionDeltaFeedbackPacket.stackedFeedbackPacketDowngrade.hasERRORLog) {
            if (failureDir == null) {
                failureDir = createFailureDir(
                        versionDeltaFeedbackPacket.stackedFeedbackPacketDowngrade.configFileName);
                saveFullSequence(failureDir,
                        versionDeltaFeedbackPacket.stackedFeedbackPacketDowngrade.fullSequence);
            }
            saveErrorReport(failureDir,
                    versionDeltaFeedbackPacket.stackedFeedbackPacketDowngrade.errorLogReport,
                    startTestID,
                    endTestID, false);
        }

        Integer[] branchVersionDeltaInducedArray = branchVersionDeltaInducedTpIds
                .toArray(new Integer[0]);
        Integer[] formatVersionDeltaInducedArray = formatVersionDeltaInducedTpIds
                .toArray(new Integer[0]);
        Integer[] branchCoverageInducedArray = onlyNewBranchCoverageInducedTpIds
                .toArray(new Integer[0]);

        // Print array using toString() method
        System.out.println();

        if (Config.getConf().debug) {
            logger.info("[HKLOG] branch coverage induced in "
                    + java.util.Arrays.toString(branchCoverageInducedArray));
            logger.info("[HKLOG] branch version delta induced in "
                    + java.util.Arrays
                            .toString(branchVersionDeltaInducedArray));
            logger.info("[HKLOG] format version delta induced in "
                    + java.util.Arrays
                            .toString(formatVersionDeltaInducedArray));
        }

        System.out.println();

        if (!testBatchCorpus.configFiles
                .contains(
                        versionDeltaFeedbackPacket.stackedFeedbackPacketUpgrade.configFileName)) {
            testBatchCorpus
                    .addConfigFile(
                            versionDeltaFeedbackPacket.stackedFeedbackPacketDowngrade.configFileName);
        }
        printInfo();
        System.out.println();

        // StackedTestPacket stackedTestPacketVersionDelta = new
        // StackedTestPacket(
        // versionDeltaFeedbackPacket.nodeNum,
        // versionDeltaFeedbackPacket.configFileName);
        // StackedTestPacket stackedTestPacketBranchCoverage = new
        // StackedTestPacket(
        // versionDeltaFeedbackPacket.nodeNum,
        // versionDeltaFeedbackPacket.configFileName);
        // StackedTestPacket stackedTestPacketFormatCoverage = new
        // StackedTestPacket(
        // versionDeltaFeedbackPacket.nodeNum,
        // versionDeltaFeedbackPacket.configFileName);

        if (Config.getConf().debug) {
            String reportDir = "fullSequences/lessPriority";
            if (formatVersionDeltaInducedTpIds.size() > 0
                    || branchVersionDeltaInducedTpIds.size() > 0) {
                reportDir = "fullSequences/versionDelta";
            }
            if (onlyNewFormatCoverageInducedTpIds.size() > 0) {
                reportDir = "fullSequences/formatCoverage";
            }
            if (onlyNewBranchCoverageInducedTpIds.size() > 0) {
                reportDir = "fullSequences/branchCoverage";
            }
            String reportName = "fullSequence_" + endTestID + ".txt";

            saveFullSequenceBasedOnType(reportDir, reportName,
                    versionDeltaFeedbackPacket.stackedFeedbackPacketUpgrade.fullSequence);
        }
        // addOneBatchToSharedQueue(versionDeltaFeedbackPacket);
    }

    public synchronized void analyzeFeedbackFromVersionDeltaGroup2(
            VersionDeltaFeedbackPacketApproach2 versionDeltaFeedbackPacket) {

        if (versionDeltaFeedbackPacket.stackedFeedbackPacketUpgrade.skipped) {
            // upgrade process is skipped
            logger.info("upgrade process is skipped");
        }

        if (versionDeltaFeedbackPacket.stackedFeedbackPacketDowngrade.skipped) {
            // upgrade process is skipped
            logger.info("downgrade process is skipped");
        }

        Path failureDir = null;

        int startTestID = 0;
        int endTestID = 0;
        if (versionDeltaFeedbackPacket.stackedFeedbackPacketUpgrade.getFpList()
                .size() > 0) {
            startTestID = versionDeltaFeedbackPacket.stackedFeedbackPacketUpgrade
                    .getFpList()
                    .get(0).testPacketID;
            endTestID = versionDeltaFeedbackPacket.stackedFeedbackPacketUpgrade
                    .getFpList()
                    .get(versionDeltaFeedbackPacket.stackedFeedbackPacketUpgrade
                            .getFpList().size()
                            - 1).testPacketID;
        }

        if (versionDeltaFeedbackPacket.stackedFeedbackPacketUpgrade.isUpgradeProcessFailed) {
            failureDir = createFailureDir(
                    versionDeltaFeedbackPacket.stackedFeedbackPacketUpgrade.configFileName);
            saveFullSequence(failureDir,
                    versionDeltaFeedbackPacket.stackedFeedbackPacketUpgrade.fullSequence);
            saveFullStopCrashReport(failureDir,
                    versionDeltaFeedbackPacket.stackedFeedbackPacketUpgrade.upgradeFailureReport,
                    startTestID,
                    endTestID, true);
            if (versionDeltaFeedbackPacket.stackedFeedbackPacketUpgrade.isDowngradeProcessFailed) {
                saveFullStopCrashReport(failureDir,
                        versionDeltaFeedbackPacket.stackedFeedbackPacketDowngrade.downgradeFailureReport,
                        startTestID,
                        endTestID, false);
            }
            finishedTestID++;
            finishedTestIdAgentGroup2++;
        } else if (versionDeltaFeedbackPacket.stackedFeedbackPacketDowngrade.isDowngradeProcessFailed) {
            failureDir = createFailureDir(
                    versionDeltaFeedbackPacket.stackedFeedbackPacketDowngrade.configFileName);
            saveFullSequence(failureDir,
                    versionDeltaFeedbackPacket.stackedFeedbackPacketDowngrade.fullSequence);
            saveFullStopCrashReport(failureDir,
                    versionDeltaFeedbackPacket.stackedFeedbackPacketDowngrade.downgradeFailureReport,
                    startTestID,
                    endTestID, false);
            finishedTestID++;
            finishedTestIdAgentGroup2++;
        }
        FuzzingServerHandler.printClientNum();

        List<FeedbackPacket> versionDeltaFeedbackPacketsUp = versionDeltaFeedbackPacket.stackedFeedbackPacketUpgrade
                .getFpList();
        List<FeedbackPacket> versionDeltaFeedbackPacketsDown = versionDeltaFeedbackPacket.stackedFeedbackPacketDowngrade
                .getFpList();

        for (FeedbackPacket fp : versionDeltaFeedbackPacketsUp) {
            if (fp.isInconsistencyInsignificant) {
                insignificantInconsistenciesIn.add(fp.testPacketID);
            }
        }

        for (FeedbackPacket fp : versionDeltaFeedbackPacketsDown) {
            if (fp.isInconsistencyInsignificant) {
                insignificantInconsistenciesIn.add(fp.testPacketID);
            }
        }

        int feedbackLength = versionDeltaFeedbackPacketsUp.size();
        System.out.println("feedback length: " + feedbackLength);
        for (int i = 0; i < feedbackLength; i++) {
            // handle invariant
            FeedbackPacket versionDeltaFeedbackPacketUp = versionDeltaFeedbackPacketsUp
                    .get(i);
            FeedbackPacket versionDeltaFeedbackPacketDown = versionDeltaFeedbackPacketsDown
                    .get(i);
            finishedTestID++;
            finishedTestIdAgentGroup2++;
            int score = 0;

            boolean addToCorpusAfterUpgrade = false;
            boolean addToCorpusAfterDowngrade = false;
            boolean newNewVersionBranchCoverageAfterUpgrade = false;
            boolean newOldVersionBranchCoverageAfterDowngrade = false;

            // Merge all the feedbacks
            FeedBack fbUpgrade = mergeCoverage(
                    versionDeltaFeedbackPacketUp.feedBacks);
            FeedBack fbDowngrade = mergeCoverage(
                    versionDeltaFeedbackPacketDown.feedBacks);

            // priority feature is disabled
            if (Utilities.hasNewBits(
                    curUpCoverageAfterUpgrade,
                    fbUpgrade.upgradedCodeCoverage)) {
                // Write Seed to Disk + Add to Corpus
                newNewVersionBranchCoverageAfterUpgrade = true;
            }
            if (Utilities.hasNewBits(curOriCoverageAfterDowngrade,
                    fbDowngrade.downgradedCodeCoverage)) {
                newOldVersionBranchCoverageAfterDowngrade = true;
            }

            curUpCoverageAfterUpgrade.merge(fbUpgrade.upgradedCodeCoverage);
            curOriCoverageAfterDowngrade
                    .merge(fbDowngrade.downgradedCodeCoverage);

            if (Config.getConf().useBranchCoverage
                    && (Config.getConf().branchCoverageChoiceProb > 0)) {
                if (newNewVersionBranchCoverageAfterUpgrade) {
                    System.out.println(
                            "Adding to code coverage corpus after upgrade: "
                                    + versionDeltaFeedbackPacketUp.testPacketID);
                    System.out.println(
                            "new new version branch coverage after upgrade: "
                                    + newNewVersionBranchCoverageAfterUpgrade);
                    addToCorpusAfterUpgrade = true;
                }
                if (newOldVersionBranchCoverageAfterDowngrade) {
                    System.out.println(
                            "Adding to code coverage corpus after upgrade: "
                                    + versionDeltaFeedbackPacketUp.testPacketID);
                    System.out.println(
                            "new old version branch coverage after downgrade: "
                                    + newOldVersionBranchCoverageAfterDowngrade);
                    addToCorpusAfterDowngrade = true;
                }
            }

            if (versionDeltaFeedbackPacketUp.isInconsistent) {
                if (failureDir == null) {
                    failureDir = createFailureDir(
                            versionDeltaFeedbackPacket.stackedFeedbackPacketUpgrade.configFileName);
                    saveFullSequence(failureDir,
                            versionDeltaFeedbackPacket.stackedFeedbackPacketUpgrade.fullSequence);
                }
                saveInconsistencyReport(failureDir,
                        versionDeltaFeedbackPacketUp.testPacketID,
                        versionDeltaFeedbackPacketUp.inconsistencyReport);
            }
            if (versionDeltaFeedbackPacketUp.isInconsistent) {
                if (failureDir == null) {
                    failureDir = createFailureDir(
                            versionDeltaFeedbackPacket.stackedFeedbackPacketUpgrade.configFileName);
                    saveFullSequence(failureDir,
                            versionDeltaFeedbackPacket.stackedFeedbackPacketUpgrade.fullSequence);
                }
                saveInconsistencyReport(failureDir,
                        versionDeltaFeedbackPacketUp.testPacketID,
                        versionDeltaFeedbackPacketUp.inconsistencyReport);
            }

            if (addToCorpusAfterUpgrade) {
                addSeedToCorpus((CorpusVersionDeltaSixQueue) corpus,
                        testID2Seed,
                        versionDeltaFeedbackPacketUp.testPacketID, score,
                        CorpusVersionDeltaSixQueue.QueueType.BC_After_Upgrade);
            }
            if (addToCorpusAfterDowngrade) {
                addSeedToCorpus((CorpusVersionDeltaSixQueue) corpus,
                        testID2Seed,
                        versionDeltaFeedbackPacketDown.testPacketID, score,
                        CorpusVersionDeltaSixQueue.QueueType.BC_After_Downgrade);
            }
        }
        // update testid2Seed, no use anymore
        for (TestPacket tp : versionDeltaFeedbackPacket.tpList) {
            testID2Seed.remove(tp.testPacketID);
        }
        if (versionDeltaFeedbackPacket.stackedFeedbackPacketUpgrade.hasERRORLog) {
            if (failureDir == null) {
                failureDir = createFailureDir(
                        versionDeltaFeedbackPacket.stackedFeedbackPacketUpgrade.configFileName);
                saveFullSequence(failureDir,
                        versionDeltaFeedbackPacket.stackedFeedbackPacketUpgrade.fullSequence);
            }
            saveErrorReport(failureDir,
                    versionDeltaFeedbackPacket.stackedFeedbackPacketUpgrade.errorLogReport,
                    startTestID,
                    endTestID, true);
        }
        if (versionDeltaFeedbackPacket.stackedFeedbackPacketDowngrade.hasERRORLog) {
            if (failureDir == null) {
                failureDir = createFailureDir(
                        versionDeltaFeedbackPacket.stackedFeedbackPacketDowngrade.configFileName);
                saveFullSequence(failureDir,
                        versionDeltaFeedbackPacket.stackedFeedbackPacketDowngrade.fullSequence);
            }
            saveErrorReport(failureDir,
                    versionDeltaFeedbackPacket.stackedFeedbackPacketDowngrade.errorLogReport,
                    startTestID,
                    endTestID, false);
        }

        printInfo();
        System.out.println();
    }

    /**
     * FailureIdx
     * - crash
     * - inconsistency
     * - error
     * @return path
     */
    private Path createFailureDir(String configFileName) {
        while (Paths
                .get(Config.getConf().failureDir,
                        "failure_" + failureId)
                .toFile().exists()) {
            failureId++;
        }
        Path failureSubDir = Paths.get(Config.getConf().failureDir,
                "failure_" + failureId++);
        failureSubDir.toFile().mkdir();
        copyConfig(failureSubDir, configFileName);
        return failureSubDir;
    }

    private void copyConfig(Path failureSubDir, String configFileName) {
        if (Config.getConf().debug)
            logger.info("[HKLOG] debug copy config, failureSubDir = "
                    + failureSubDir + " configFile = " + configFileName
                    + " configPath = " + configDirPath);
        if (configFileName == null || configFileName.isEmpty())
            return;
        Path configPath = Paths.get(configDirPath.toString(), configFileName);
        try {
            FileUtils.copyDirectory(configPath.toFile(),
                    failureSubDir.toFile());
        } catch (IOException e) {
            logger.error("config file not exist with exception: " + e);
        }
    }

    private Path createFullStopCrashSubDir(Path failureSubDir) {
        Path dir = failureSubDir.resolve("fullstop_crash");
        dir.toFile().mkdir();
        return dir;
    }

    private Path createEventCrashSubDir(Path failureSubDir) {
        Path dir = failureSubDir.resolve("event_crash");
        dir.toFile().mkdir();
        return dir;
    }

    private Path createInconsistencySubDir(Path failureSubDir) {
        Path inconsistencyDir = failureSubDir.resolve("inconsistency");
        inconsistencyDir.toFile().mkdir();
        return inconsistencyDir;
    }

    private Path createErrorSubDir(Path failureSubDir) {
        Path inconsistencyDir = failureSubDir.resolve("errorLog");
        inconsistencyDir.toFile().mkdir();
        return inconsistencyDir;
    }

    private void saveFullSequence(Path failureDir,
            String fullSequence) {
        Path crashReportPath = Paths.get(
                failureDir.toString(),
                "fullSequence.report");
        Utilities.write2TXT(crashReportPath.toFile(), fullSequence, false);
    }

    private void saveFullSequenceBasedOnType(String storageDir,
            String reportName,
            String fullSequence) {

        File storage = new File(storageDir);
        if (!storage.exists()) {
            storage.mkdirs();
        }

        Path fullSequenceReportPath = Paths.get(
                storageDir.toString(),
                reportName);
        Utilities.write2TXT(fullSequenceReportPath.toFile(), fullSequence,
                false);
    }

    private void saveFullStopCrashReport(Path failureDir,
            String report, int startTestID) {
        Path subDir = createFullStopCrashSubDir(failureDir);
        Path crashReportPath = Paths.get(
                subDir.toString(),
                String.format("fullstop_%d_crash.report", startTestID));
        Utilities.write2TXT(crashReportPath.toFile(), report, false);
        fullStopCrashNum++;
    }

    private void saveFullStopCrashReport(Path failureDir,
            String report, int startTestID, int endTestID) {
        Path subDir = createFullStopCrashSubDir(failureDir);
        Path crashReportPath = Paths.get(
                subDir.toString(),
                String.format("fullstop_%d_%d_crash.report", startTestID,
                        endTestID));
        Utilities.write2TXT(crashReportPath.toFile(), report, false);
        fullStopCrashNum++;
    }

    private void saveFullStopCrashReport(Path failureDir,
            String report, int startTestID, int endTestID, boolean isUpgrade) {
        Path subDir = createFullStopCrashSubDir(failureDir);
        Path crashReportPath = Paths.get(
                subDir.toString(),
                String.format("fullstop_%d_%d_%s_crash.report", startTestID,
                        endTestID, isUpgrade ? "upgrade" : "downgrade"));
        Utilities.write2TXT(crashReportPath.toFile(), report, false);
        fullStopCrashNum++;
    }

    private void saveEventCrashReport(Path failureDir, int testID,
            String report) {
        Path subDir = createEventCrashSubDir(failureDir);
        Path crashReportPath = Paths.get(
                subDir.toString(),
                "event_crash_" + testID + ".report");
        Utilities.write2TXT(crashReportPath.toFile(), report, false);
        eventCrashNum++;
    }

    private void saveInconsistencyReport(Path failureDir, int testID,
            String report) {
        Path inconsistencySubDir = createInconsistencySubDir(failureDir);
        Path crashReportPath = Paths.get(
                inconsistencySubDir.toString(),
                "inconsistency_" + testID + ".report");
        Utilities.write2TXT(crashReportPath.toFile(), report, false);
        inconsistencyNum++;
    }

    private void saveInconsistencyReport(Path failureDir, int testID,
            String report, boolean isUpgrade) {
        Path inconsistencySubDir = createInconsistencySubDir(failureDir);
        Path crashReportPath = Paths.get(
                inconsistencySubDir.toString(),
                "inconsistency_" + testID + "_"
                        + (isUpgrade ? "upgrade" : "downgrade") + ".report");
        Utilities.write2TXT(crashReportPath.toFile(), report, false);
        inconsistencyNum++;
    }

    private void saveErrorReport(Path failureDir, String report, int testID) {
        Path errorSubDir = createErrorSubDir(failureDir);
        Path reportPath = Paths.get(
                errorSubDir.toString(),
                String.format("error_%d.report", testID));
        Utilities.write2TXT(reportPath.toFile(), report, false);
        errorLogNum++;
    }

    // For version delta, since might need two error log files
    private void saveErrorReport(Path failureDir, String report,
            int startTestID, int endTestID, boolean isUpgrade) {
        Path errorSubDir = createErrorSubDir(failureDir);
        Path reportPath = Paths.get(
                errorSubDir.toString(),
                String.format("error_%d_%d_%s.report", startTestID, endTestID,
                        isUpgrade ? "upgrade" : "downgrade"));
        Utilities.write2TXT(reportPath.toFile(), report, false);
        errorLogNum++;
    }

    private void saveErrorReport(Path failureDir, String report,
            int startTestID, int endTestID) {
        Path errorSubDir = createErrorSubDir(failureDir);
        Path reportPath = Paths.get(
                errorSubDir.toString(),
                String.format("error_%d_%d.report", startTestID, endTestID));
        Utilities.write2TXT(reportPath.toFile(), report, false);
        errorLogNum++;
    }

    public void printInfo() {
        updateBCStatus();

        long timeElapsed = TimeUnit.SECONDS.convert(
                System.nanoTime(), TimeUnit.NANOSECONDS) - startTime;

        System.out.println("--------------------------------------------------"
                +
                " TestStatus ---------------------------------------------------------------");
        System.out.println("System: " + Config.getConf().system);
        if (Config.getConf().testSingleVersion) {
            System.out.println(
                    "Test single version: " + Config.getConf().originalVersion);
        } else {
            System.out.println("Upgrade Testing: "
                    + Config.getConf().originalVersion + "=>"
                    + Config.getConf().upgradedVersion);
        }
        System.out.println(
                "============================================================"
                        + "=================================================================");
        System.out.format("|%30s|%30s|%30s|%30s|\n",
                "cur testID : " + testID,
                "total exec : " + finishedTestID, "", "");

        if (Config.getConf().testSingleVersion) {
            System.out.format("|%30s|%30s|\n",
                    "run time : " + timeElapsed + "s",
                    "BC : " + oriCoveredBranches + "/"
                            + oriProbeNum);
        } else {
            System.out.format("|%30s|%30s|%30s|%30s|\n",
                    "run time : " + timeElapsed + "s",
                    "round : " + round,
                    "ori BC : " + oriCoveredBranches + "/"
                            + oriProbeNum,
                    "up BC upgrade : " + upCoveredBranchesAfterUpgrade
                            + "/"
                            + upProbeNumAfterUpgrade);
        }
        // Print queue info...
        corpus.printInfo();

        // Format Coverage Info...
        if (Config.getConf().useFormatCoverage
                && (Config.getConf().formatCoverageChoiceProb > 0)) {
            System.out.format("|%30s|%30s|\n",
                    "ori new format num : " + oriNewFormatNum,
                    "up new format num : " + upNewFormatNum);
        }
        // Version Delta Info
        if (Config.getConf().useVersionDelta
                && (Config.getConf().branchVersionDeltaChoiceProb > 0)) {
            System.out.format("|%30s|%30s|%30s|%30s|\n",
                    "exec group 1 : " + finishedTestIdAgentGroup1,
                    "exec group 2 : " + finishedTestIdAgentGroup2,
                    "up BC : " + upCoveredBranches + "/"
                            + upProbeNum,
                    "ori BC downgrade : "
                            + oriCoveredBranchesAfterDowngrade + "/"
                            + oriProbeNumAfterDowngrade);

            if (Config.getConf().debug) {
                System.out.format("|%30s|%30s|%30s|%30s|\n",
                        "both version delta : "
                                + (newVersionDeltaCountForBranchCoverage
                                        - executionCountWithOnlyNewBranchDelta),
                        "exec with branch cov : "
                                + executionCountWithOnlyNewBranchCoverage,
                        "exec with format cov : "
                                + executionCountWithOnlyNewFormatCoverage,
                        "");
            }
        }

        if (Config.getConf().debug)
            System.out.format("|%30s|%30s|\n",
                    "testID2Seed size : " + testID2Seed.size(),
                    "stackedTestPackets size : " + stackedTestPackets.size());

        System.out.println(
                "------------------------------------------------------------"
                        + "-----------------------------------------------------------------");
        // Failures
        System.out.format("|%30s|%30s|%30s|%30s|\n",
                "fullstop crash : " + fullStopCrashNum,
                "event crash : " + eventCrashNum,
                "inconsistency : " + inconsistencyNum,
                "error log : " + errorLogNum);
        System.out.println(
                "------------------------------------------------------------"
                        + "-----------------------------------------------------------------");
        System.out.println();

        if (Config.getConf().debug) {
            logger.info("[HKLOG] insignificant inconsistencies in: "
                    + insignificantInconsistenciesIn.toString());
            logger.info("[HKLOG: Seed tracker] Mutated Seed IDs: "
                    + mutatedSeedIds.toString());
            logger.info("[HKLOG] buffer details: ");
            testBatchCorpus.printCache();
        }
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

    public static int getSeedOrTestType(double[] cumulativeProbabilities) {
        // Generate a random number between 0 and 1
        double randomValue = rand.nextDouble();

        // Find the queue whose cumulative probability is greater than or equal
        // to the random value
        for (int i = 0; i < cumulativeProbabilities.length; i++) {
            if (randomValue <= cumulativeProbabilities[i]) {
                return i;
            }
        }

        // Should not reach here if probabilities are valid
        throw new IllegalStateException("Invalid probabilities");
    }

    public int getNextBestTestType(Map<Integer, Double> probabilities,
            String configFileName) {
        List<Map.Entry<Integer, Double>> sortedProbabilities = new ArrayList<>(
                probabilities.entrySet());
        sortedProbabilities.sort((entry1, entry2) -> Double
                .compare(entry2.getValue(), entry1.getValue()));

        // Iterate through sorted probabilities
        for (Map.Entry<Integer, Double> entry : sortedProbabilities) {
            int elementIndex = entry.getKey();
            if (!testBatchCorpus.isEmpty(InterestingTestsCorpus.TestType
                    .values()[elementIndex])
                    && testBatchCorpus.intermediateBuffer[elementIndex]
                            .containsKey(configFileName)) {
                return elementIndex; // Return the index of the non-empty
                                     // list which has a key for the
                                     // configFileName
            }
        }
        return -1;
    }

    public static int getNextBestSeedType(
            CorpusVersionDeltaSixQueue corpusVersionDeltaSixQueue,
            Map<Integer, Double> probabilities) {
        // Sort probabilities in descending order
        List<Map.Entry<Integer, Double>> sortedProbabilities = new ArrayList<>(
                probabilities.entrySet());
        sortedProbabilities.sort((entry1, entry2) -> Double
                .compare(entry2.getValue(), entry1.getValue()));

        // Iterate through sorted probabilities
        for (Map.Entry<Integer, Double> entry : sortedProbabilities) {
            int elementIndex = entry.getKey();

            if (!corpusVersionDeltaSixQueue
                    .isEmpty(CorpusVersionDeltaSixQueue.QueueType
                            .values()[elementIndex])) {
                return elementIndex; // Return the index of the non-empty
                                     // list
            }
        }

        // Throw exception if no non-empty list found
        throw new NoSuchElementException(
                "No non-empty list found based on probabilities");
    }

    public List<Event> interleaveWithOrder(List<Event> events1,
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

    public synchronized boolean addNonInterestingTestsToBuffer(
            double randomNumber, double probabilityThreshold) {
        return randomNumber >= probabilityThreshold;
    }

    /**
     * 1. find a position after the first upgrade operation
     * 2. collect all upgrade op node idx between [first_upgrade, pos]
     * 3. remove all the upgrade op after it
     * 4. downgrade all nodeidx collected
     */
    public List<Event> addDowngrade(List<Event> events) {
        // Add downgrade during the upgrade/when all nodes have been upgraded.
        List<Event> newEvents;
        // find first upgrade op
        int pos1 = 0;
        for (; pos1 < events.size(); pos1++) {
            if (events.get(pos1) instanceof UpgradeOp) {
                break;
            }
        }
        if (pos1 == events.size()) {
            throw new RuntimeException(
                    "no nodes are upgraded, cannot downgrade");
        }
        int pos2 = Utilities.randWithRange(rand, pos1 + 1, events.size() + 1);

        newEvents = events.subList(0, pos2);
        assert newEvents.size() == pos2;

        List<Integer> upgradeNodeIdxes = new LinkedList<>();
        for (int i = pos1; i < pos2; i++) {
            if (newEvents.get(i) instanceof UpgradeOp)
                upgradeNodeIdxes.add(((UpgradeOp) newEvents.get(i)).nodeIndex);
        }

        // downgrade in a reverse way
        upgradeNodeIdxes.sort(Collections.reverseOrder());
        // logger.info("upgrade = " + upgradeNodeIdxes);
        for (int nodeIdx : upgradeNodeIdxes) {
            newEvents.add(new DowngradeOp(nodeIdx));
        }
        return newEvents;
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

    public static void saveSeed(CommandSequence commandSequence,
            CommandSequence validationCommandSequence) {
        // Serialize the seed of the queue in to disk
        if (Config.getConf().corpusDir == null) {
            logger.debug("corpusDir is not provided, not saving seeds");
        } else {
            File corpusDir = new File(Config.getConf().corpusDir);
            if (!corpusDir.exists()) {
                corpusDir.mkdirs();
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Seed Id = ").append(seedID).append("\n");
            sb.append("Command Sequence\n");
            for (String commandStr : commandSequence.getCommandStringList()) {
                sb.append(commandStr);
                sb.append("\n");
            }
            sb.append("Read Command Sequence\n");
            for (String commandStr : validationCommandSequence
                    .getCommandStringList()) {
                sb.append(commandStr);
                sb.append("\n");
            }
            Path crashReportPath = Paths.get(Config.getConf().corpusDir,
                    "seed_" + seedID + ".txt");
            Utilities.write2TXT(crashReportPath.toFile(), sb.toString(), false);
            seedID++;
        }
    }
}
