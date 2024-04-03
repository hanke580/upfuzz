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
import org.zlab.upfuzz.hbase.general.STATUS;
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
    public Corpus corpus = new Corpus();
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

    private int newVersionDeltaCountForBranchCoverage = 0; // Use it in the
                                                           // UpdateStatus
                                                           // method,
                                                           // when VersionDelta
                                                           // is added to the
                                                           // implementation
    private int newVersionDeltaCountForFormatCoverage = 0;
    private int executionCountWithOnlyNewBranchDelta = 0;
    private int executionCountWithOnlyNewFormatDelta = 0;
    private int executionCountWithOnlyNewBranchCoverage = 0;
    private int executionCountWithOnlyNewFormatCoverage = 0;
    private int executionCountWithoutAnyNewCoverage = 0;

    // Config mutation
    ConfigGen configGen;
    public Path configDirPath;

    // Format coverage
    private ObjectGraphCoverage oriObjCoverage;
    private ObjectGraphCoverage upObjCoverage;

    // System state comparison
    public Set<String> targetSystemStates = new HashSet<>();

    // Coverage
    public static int originalCoveredBranches = 0;
    public static int originalProbeNum = 0;
    public static int upgradedCoveredBranches = 0;
    public static int upgradedProbeNum = 0;

    public static List<Pair<Integer, Integer>> originalCoverageAlongTime = new ArrayList<>(); // time:
    public static List<Pair<Integer, Integer>> upgradedCoverageAlongTime = new ArrayList<>(); // time:
    public static long lastTimePoint = 0;
    public BlockingQueue<StackedTestPacket> stackedTestPacketsQueueVersionDelta;
    public InterestingTestsCorpus testBatchCorpus;
    public long startTime;
    public long lastRoundIntroducingVersionDelta;

    ExecutionDataStore curOriCoverage;
    ExecutionDataStore curUpCoverage;
    ExecutionDataStore curUpCoverageBeforeDowngrade;
    ExecutionDataStore curOriCoverageAfterDowngrade;
    ExecutionDataStore curUpCoverageAfterUpgrade;

    TestTrackerGraph graph = new TestTrackerGraph();

    // ----------------- Evaluation -----------------
    /**
     * We start mutate boundary config after 20 tests, see what will happen,
     * use fixed command sequence
     */
    public static boolean startMutateBoundaryConfig = false;

    // Calculate cumulative probabilities
    double[] cumulativeSeedChoiceProbabilities = new double[6];

    double newCovAfterUpgSeedChoiceProb;
    double newCovAfterDowngSeedChoiceProb;
    double branchDeltaSeedChoiceProb;
    double formatDeltaSeedChoiceProb;
    double branchCovSeedChoiceProb;
    double formatCovSeedChoiceProb;

    double[] cumulativeTestChoiceProbabilities = new double[4];

    double branchCovProbability;
    double formatCovProbability;
    double branchVersionDeltaProbability;
    double formatVersionDeltaProbability;

    Set<Integer> mutatedSeedIds = new HashSet<>();
    Set<Integer> insignificantInconsistenciesIn = new HashSet<>();
    Map<Integer, Double> seedChoiceProbabilities;
    Map<Integer, Double> testChoiceProbabilities;

    List<Integer> versionDeltaInducedTpIds = new ArrayList<>();
    List<Integer> branchVersionDeltaInducedTpIds = new ArrayList<>();
    List<Integer> formatVersionDeltaInducedTpIds = new ArrayList<>();
    List<Integer> onlyNewBranchCoverageInducedTpIds = new ArrayList<>();
    List<Integer> onlyNewFormatCoverageInducedTpIds = new ArrayList<>();
    List<Integer> newCoverageAfterUpgradeInducedTpIds = new ArrayList<>();
    List<Integer> newCoverageAfterDowngradeInducedTpIds = new ArrayList<>();
    List<Integer> nonInterestingTpIds = new ArrayList<>();

    public FuzzingServer() {
        testID2Seed = new HashMap<>();
        testID2TestPlan = new HashMap<>();
        stackedTestPackets = new LinkedList<>();
        fullStopPackets = new LinkedList<>();
        testPlanPackets = new LinkedList<>();
        curOriCoverage = new ExecutionDataStore();
        curUpCoverage = new ExecutionDataStore();
        curUpCoverageBeforeDowngrade = new ExecutionDataStore();
        curOriCoverageAfterDowngrade = new ExecutionDataStore();
        curUpCoverageAfterUpgrade = new ExecutionDataStore();

        if (Config.getConf().useFormatCoverage) {
            // FIXME: add isSerialized path
            if (Config.getConf().oriFormatInfoFolder == null
                    || Config.getConf().upFormatInfoFolder == null) {
                throw new RuntimeException(
                        "oriFormatInfoFolder or upFormatInfoFolder is not specified in the configuration file "
                                +
                                "while format coverage is enabled");
            }
            oriObjCoverage = new ObjectGraphCoverage(
                    Paths.get(Config.getConf().oriFormatInfoFolder,
                            Config.getConf().baseClassInfoFileName),
                    Paths.get(Config.getConf().oriFormatInfoFolder,
                            Config.getConf().topObjectsFileName),
                    Paths.get(Config.getConf().oriFormatInfoFolder,
                            Config.getConf().comparableClassesFileName),
                    null,
                    null,
                    null);
            upObjCoverage = new ObjectGraphCoverage(
                    Paths.get(Config.getConf().upFormatInfoFolder,
                            Config.getConf().baseClassInfoFileName),
                    Paths.get(Config.getConf().upFormatInfoFolder,
                            Config.getConf().topObjectsFileName),
                    Paths.get(Config.getConf().upFormatInfoFolder,
                            Config.getConf().comparableClassesFileName),
                    null,
                    null,
                    null);
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

        branchCovProbability = Config.getConf().branchCoverageChoiceProb;
        formatCovProbability = Config.getConf().formatCoverageChoiceProb;
        branchVersionDeltaProbability = Config
                .getConf().branchVersionDeltaChoiceProb;
        formatVersionDeltaProbability = Config
                .getConf().formatVersionDeltaChoiceProb;

        branchCovSeedChoiceProb = Config.getConf().branchCovSeedChoiceProb;
        formatCovSeedChoiceProb = Config.getConf().formatCovSeedChoiceProb;
        branchDeltaSeedChoiceProb = Config.getConf().branchDeltaSeedChoiceProb;
        formatDeltaSeedChoiceProb = Config.getConf().formatDeltaSeedChoiceProb;
        newCovAfterUpgSeedChoiceProb = Config
                .getConf().branchCovAfterUpgSeedChoiceProb;
        newCovAfterDowngSeedChoiceProb = Config
                .getConf().branchCovAfterDowngSeedChoiceProb;
    }

    private void init() {
        /**
         * Force GC every 10 minutes, using format coverage would incur a
         * large amount of objects which need to be GC quickly. Otherwise,
         * the memory usage could burst to 20+GB.
         */
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            System.gc();
            logger.info("[GC] Garbage Collection invoked");
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
        seedChoiceProbabilities.put(0, formatDeltaSeedChoiceProb);
        seedChoiceProbabilities.put(1, branchDeltaSeedChoiceProb);
        seedChoiceProbabilities.put(2, formatCovSeedChoiceProb);
        seedChoiceProbabilities.put(3, branchCovSeedChoiceProb);
        seedChoiceProbabilities.put(4, newCovAfterUpgSeedChoiceProb);
        seedChoiceProbabilities.put(5, newCovAfterUpgSeedChoiceProb);

        testChoiceProbabilities = new HashMap<>();
        testChoiceProbabilities.put(0, formatVersionDeltaProbability);
        testChoiceProbabilities.put(1, branchVersionDeltaProbability);
        testChoiceProbabilities.put(2, formatCovProbability);
        testChoiceProbabilities.put(3, branchCovProbability);

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
        String configFileName = testBatchCorpus.getConfigFile();
        StackedTestPacket stackedTestPacket = new StackedTestPacket(
                Config.getConf().nodeNum, configFileName);
        logger.info("[HKLOG] config file name: " + configFileName);
        for (int i = 0; i < (int) (Config.getConf().batchSizePortionInGroup2
                * Config.getConf().STACKED_TESTS_NUM); i++) {
            if (!testBatchCorpus.noInterestingTests()) {
                int testType = getSeedOrTestType(
                        cumulativeTestChoiceProbabilities);
                if (testBatchCorpus.queues[testType].size() == 0) {
                    testType = getNextBestType(testChoiceProbabilities, 1);
                }
                TestPacket testPacket = testBatchCorpus.getPacket(
                        InterestingTestsCorpus.TestType.values()[testType]);
                if (testPacket != null) {
                    stackedTestPacket.addTestPacket(testPacket);
                }
            } else {
                TestPacket testPacket = testBatchCorpus.getPacket(
                        InterestingTestsCorpus.TestType.LOW_PRIORITY);
                try {
                    if (testPacket != null) {
                        stackedTestPacket
                                .addTestPacket(testPacket);
                    }
                } catch (Exception e) {
                    logger.error(
                            "Not enough test packets in the buffer yet, trying with a smaller batch in this execution");
                }
            }
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
                logger.info(
                        "[HKLOG: fuzzing server] No stacked test packets available now, calling fuzzOne()!");
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

    public void fuzzOne() {
        // Pick one test case from the corpus, fuzz it for mutationEpoch
        // Add the new tests into the stackedTestPackets
        // All packets have been dispatched, now fuzz next seed

        Seed seed = null;
        // if (rand.nextDouble() < Config.getConf().getSeedFromCorpusRatio)
        // seed = corpus.getSeed();
        int corpusType = getSeedOrTestType(cumulativeSeedChoiceProbabilities);
        if ((branchCovSeedChoiceProb > 0) || (formatCovSeedChoiceProb > 0)
                || (branchDeltaSeedChoiceProb > 0)
                || (formatDeltaSeedChoiceProb > 0)
                || (newCovAfterUpgSeedChoiceProb > 0)
                || (newCovAfterDowngSeedChoiceProb > 0)) {
            if (Config.getConf().debug) {
                logger.debug("[HKLOG] Chosen seed of coverage type: "
                        + (corpusType == 2 ? "version delta" : "format/code"));
            }
            logger.info(corpus.getSize(Corpus.QueueType.values()[corpusType]));
            if (corpus.getSize(Corpus.QueueType.values()[corpusType]) == 0
                    && !corpus.areAllQueuesEmpty()) {
                corpusType = getNextBestType(seedChoiceProbabilities, 0);
            }
            if (Config.getConf().debug) {
                logger.info("Before getting seed from the corpus: ");
                corpus.printCorpus();
            }
            seed = corpus.getSeed(Corpus.QueueType.values()[corpusType]);
            if (Config.getConf().debug) {
                logger.info("After getting seed from the corpus: ");
                corpus.printCorpus();
            }
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
        } else {
            seed = null;
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
                && Utilities.hasNewBits(curUpCoverage,
                        fb.upgradedCodeCoverage)) {
            addToCorpus = true;
            curUpCoverage.merge(fb.upgradedCodeCoverage);
        }

        if (addToCorpus) {
            fullStopCorpus.addSeed(new FullStopSeed(
                    testID2Seed.get(fullStopFeedbackPacket.testPacketID),
                    Config.getConf().nodeNum,
                    fullStopFeedbackPacket.systemStates, new LinkedList<>()));

            // logger.info("[HKLOG] system state = "
            // + fullStopFeedbackPacket.systemStates);

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

    public void addSeedToCorpus(Corpus corpus,
            Map<Integer, Seed> testID2Seed, FeedbackPacket feedbackPacket,
            int score, Corpus.QueueType queueType) {
        Seed seed = testID2Seed.get(feedbackPacket.testPacketID);
        seed.score = score;
        saveSeed(seed.originalCommandSequence,
                seed.validationCommandSequence);
        fullStopCorpus.addSeed(
                new FullStopSeed(seed, feedbackPacket.nodeNum,
                        new HashMap<>(),
                        feedbackPacket.validationReadResults));
        logger.debug("Add seed " + seed.testID + " to queue " + queueType);
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
            if (Utilities.hasNewBits(curUpCoverage,
                    fb.upgradedCodeCoverage)) {
                addToCorpus = true;
                curUpCoverage.merge(fb.upgradedCodeCoverage);
            }
            if (addToCorpus) {
                testPlanCorpus.addTestPlan(
                        testID2TestPlan
                                .get(testPlanFeedbackPacket.testPacketID));
            }

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
                if (Utilities.hasNewBitsDebug(curUpCoverage,
                        fb.upgradedCodeCoverage)) {
                    curUpCoverage.merge(
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
                if (Utilities.hasNewBits(curUpCoverage,
                        fb.upgradedCodeCoverage)) {
                    curUpCoverage.merge(
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

            if (Config.getConf().useFormatCoverage && newFormatCoverage) {
                // Some seeds might be added to both corpus
                addSeedToCorpus(corpus, testID2Seed, feedbackPacket, score,
                        Corpus.QueueType.FORMAT_COVERAGE);

            }
            if (Config.getConf().useBranchCoverage
                    && (newOldVersionBranchCoverage
                            || newNewVersionBranchCoverage)) {
                addSeedToCorpus(corpus, testID2Seed, feedbackPacket, score,
                        Corpus.QueueType.BRANCH_COVERAGE_BEFORE_VERSION_CHANGE);
            }

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

        // if (finishedTestID > 20) {
        // startMutateBoundaryConfig = true;
        // }

        printInfo();
        System.out.println();
    }

    public synchronized void analyzeFeedbackFromVersionDeltaGroup1(
            VersionDeltaFeedbackPacket versionDeltaFeedbackPacket) {
        int startTestID = 0;
        int endTestID = 0;
        if (versionDeltaFeedbackPacket.getFpList("up").size() > 0) {
            startTestID = versionDeltaFeedbackPacket.getFpList("up")
                    .get(0).testPacketID;
            endTestID = versionDeltaFeedbackPacket.getFpList("up")
                    .get(versionDeltaFeedbackPacket.getFpList("up").size()
                            - 1).testPacketID;
        }

        FuzzingServerHandler.printClientNum();

        List<FeedbackPacket> versionDeltaFeedbackPacketsUp = versionDeltaFeedbackPacket
                .getFpList("up");
        List<FeedbackPacket> versionDeltaFeedbackPacketsDown = versionDeltaFeedbackPacket
                .getFpList("down");

        int feedbackLength = versionDeltaFeedbackPacketsUp.size();
        System.out.println("feedback length: " + feedbackLength);

        Path failureDir = null;

        for (int i = 0; i < feedbackLength; i++) {
            // handle invariant
            TestPacket testPacket = versionDeltaFeedbackPacket.versionDeltaInducedTestPackets
                    .get(i);
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

            if (Config.getConf().debug) {
                logger.info("[HKLOG] branch coverage choice probability: "
                        + Config.getConf().branchCoverageChoiceProb);
                logger.info("[HKLOG] version delta choice probability: "
                        + Config.getConf().branchVersionDeltaChoiceProb);
                logger.info("[HKLOG] format coverage choice probability: "
                        + Config.getConf().formatCoverageChoiceProb);
            }

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
                            InterestingTestsCorpus.TestType.BRANCH_COVERAGE_VERSION_DELTA);
                    branchVersionDeltaInducedTpIds
                            .add(versionDeltaFeedbackPacket.testIDs.get(i));
                } else {
                    testBatchCorpus.addPacket(testPacket,
                            InterestingTestsCorpus.TestType.FORMAT_COVERAGE_VERSION_DELTA);
                    formatVersionDeltaInducedTpIds
                            .add(versionDeltaFeedbackPacket.testIDs.get(i));
                }
            } else {
                if (hasFeedbackInducedBranchVersionDelta
                        && hasFeedbackInducedFormatVersionDelta) {
                    testBatchCorpus.addPacket(testPacket,
                            InterestingTestsCorpus.TestType.FORMAT_COVERAGE_VERSION_DELTA);
                    versionDeltaInducedTpIds
                            .add(versionDeltaFeedbackPacket.testIDs.get(i));
                } else {
                    if (hasFeedbackInducedNewBranchCoverage
                            ^ hasFeedbackInducedNewFormatCoverage) {
                        if (hasFeedbackInducedNewFormatCoverage) {
                            testBatchCorpus.addPacket(testPacket,
                                    InterestingTestsCorpus.TestType.FORMAT_COVERAGE);
                            onlyNewFormatCoverageInducedTpIds.add(
                                    versionDeltaFeedbackPacket.testIDs.get(i));
                        } else {
                            testBatchCorpus.addPacket(testPacket,
                                    InterestingTestsCorpus.TestType.BRANCH_COVERAGE_BEFORE_VERSION_CHANGE);
                            onlyNewBranchCoverageInducedTpIds.add(
                                    versionDeltaFeedbackPacket.testIDs.get(i));
                        }
                    } else {
                        if (hasFeedbackInducedNewBranchCoverage
                                && hasFeedbackInducedNewFormatCoverage) {
                            testBatchCorpus.addPacket(testPacket,
                                    InterestingTestsCorpus.TestType.FORMAT_COVERAGE);
                            onlyNewFormatCoverageInducedTpIds.add(
                                    versionDeltaFeedbackPacket.testIDs.get(i));
                            onlyNewBranchCoverageInducedTpIds.add(
                                    versionDeltaFeedbackPacket.testIDs.get(i));
                        } else {
                            testBatchCorpus.addPacket(testPacket,
                                    InterestingTestsCorpus.TestType.LOW_PRIORITY);
                            nonInterestingTpIds.add(
                                    versionDeltaFeedbackPacket.testIDs.get(i));
                        }
                    }
                }
            }

            System.out.println(versionDeltaFeedbackPacketUp.testPacketID);
            graph.updateNodeCoverage(versionDeltaFeedbackPacketUp.testPacketID,
                    newOldVersionBranchCoverageBeforeUpgrade,
                    newNewVersionBranchCoverageBeforeDowngrade,
                    oriNewFormat);

            if (addToCorpus) {
                addSeedToCorpus(corpus, testID2Seed,
                        versionDeltaFeedbackPacketUp, score,
                        Corpus.QueueType.BRANCH_COVERAGE_BEFORE_VERSION_CHANGE);
            }
            if (addToFormatCoverageCorpus) {
                addSeedToCorpus(corpus, testID2Seed,
                        versionDeltaFeedbackPacketUp, score,
                        Corpus.QueueType.FORMAT_COVERAGE);
            }
            if (addToVersionDeltaCorpusForBranchCoverage) {
                addSeedToCorpus(corpus, testID2Seed,
                        versionDeltaFeedbackPacketUp, score,
                        Corpus.QueueType.BRANCH_COVERAGE_VERSION_DELTA);
            }
            if (addToVersionDeltaCorpusForFormatCoverage) {
                addSeedToCorpus(corpus, testID2Seed,
                        versionDeltaFeedbackPacketUp, score,
                        Corpus.QueueType.FORMAT_COVERAGE_VERSION_DELTA);
            }
        }
        // update testid2Seed, no use anymore
        // for (int testID : versionDeltaFeedbackPacket.testIDs) {
        // if (!versionDeltaInducedTpIds.contains(testID)) {
        // testID2Seed.remove(testID);
        // }
        // }

        if (versionDeltaFeedbackPacket.hasERRORLog) {
            if (failureDir == null) {
                failureDir = createFailureDir(
                        versionDeltaFeedbackPacket.configFileName);
                saveFullSequence(failureDir,
                        versionDeltaFeedbackPacket.fullSequence);
            }
            saveErrorReport(failureDir,
                    versionDeltaFeedbackPacket.errorLogReport, startTestID,
                    endTestID);
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

        // Update the coveredBranches to the newest value
        Pair<Integer, Integer> curOriCoverageStatus = Utilities
                .getCoverageStatus(
                        curOriCoverage);
        originalCoveredBranches = curOriCoverageStatus.left;
        originalProbeNum = curOriCoverageStatus.right;

        Pair<Integer, Integer> curUpCoverageStatus = Utilities
                .getCoverageStatus(
                        curUpCoverage);
        upgradedCoveredBranches = curUpCoverageStatus.left;
        upgradedProbeNum = curUpCoverageStatus.right;

        Long timeElapsed = TimeUnit.SECONDS.convert(
                System.nanoTime(), TimeUnit.NANOSECONDS)
                - startTime;
        if (timeElapsed - lastTimePoint > Config.getConf().timeInterval
                || lastTimePoint == 0) {
            // Insert a record (time: coverage)
            originalCoverageAlongTime.add(
                    new Pair(timeElapsed, originalCoveredBranches));
            upgradedCoverageAlongTime.add(
                    new Pair(timeElapsed, upgradedCoveredBranches));
            lastTimePoint = timeElapsed;
        }

        testBatchCorpus
                .addConfigFile(versionDeltaFeedbackPacket.configFileName);
        printInfo();
        System.out.println();

        StackedTestPacket stackedTestPacketVersionDelta = new StackedTestPacket(
                versionDeltaFeedbackPacket.nodeNum,
                versionDeltaFeedbackPacket.configFileName);
        StackedTestPacket stackedTestPacketBranchCoverage = new StackedTestPacket(
                versionDeltaFeedbackPacket.nodeNum,
                versionDeltaFeedbackPacket.configFileName);
        StackedTestPacket stackedTestPacketFormatCoverage = new StackedTestPacket(
                versionDeltaFeedbackPacket.nodeNum,
                versionDeltaFeedbackPacket.configFileName);

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
                    versionDeltaFeedbackPacket.fullSequence);
        }
        // addOneBatchToSharedQueue(versionDeltaFeedbackPacket);
    }

    public synchronized void analyzeFeedbackFromVersionDeltaGroup2(
            VersionDeltaFeedbackPacket versionDeltaFeedbackPacket) {

        if (versionDeltaFeedbackPacket.skippedUpgrade) {
            // upgrade process is skipped
            logger.info("upgrade process is skipped");
        }

        if (versionDeltaFeedbackPacket.skippedDowngrade) {
            // upgrade process is skipped
            logger.info("downgrade process is skipped");
        }

        Path failureDir = null;

        int startTestID = 0;
        int endTestID = 0;
        if (versionDeltaFeedbackPacket.getFpList("up").size() > 0) {
            startTestID = versionDeltaFeedbackPacket.getFpList("up")
                    .get(0).testPacketID;
            endTestID = versionDeltaFeedbackPacket.getFpList("up")
                    .get(versionDeltaFeedbackPacket.getFpList("up").size()
                            - 1).testPacketID;
        }

        if (versionDeltaFeedbackPacket.isUpgradeProcessFailed) {
            failureDir = createFailureDir(
                    versionDeltaFeedbackPacket.configFileName);
            saveFullSequence(failureDir,
                    versionDeltaFeedbackPacket.fullSequence);
            saveFullStopCrashReport(failureDir,
                    versionDeltaFeedbackPacket.upgradeFailureReport,
                    startTestID,
                    endTestID);
            if (versionDeltaFeedbackPacket.isDowngradeProcessFailed) {
                saveFullStopCrashReport(failureDir,
                        versionDeltaFeedbackPacket.downgradeFailureReport,
                        startTestID,
                        endTestID);
            }
            finishedTestID++;
            finishedTestIdAgentGroup2++;
        } else if (versionDeltaFeedbackPacket.isDowngradeProcessFailed) {
            failureDir = createFailureDir(
                    versionDeltaFeedbackPacket.configFileName);
            saveFullSequence(failureDir,
                    versionDeltaFeedbackPacket.fullSequence);
            saveFullStopCrashReport(failureDir,
                    versionDeltaFeedbackPacket.downgradeFailureReport,
                    startTestID,
                    endTestID);
            finishedTestID++;
            finishedTestIdAgentGroup2++;
        }
        FuzzingServerHandler.printClientNum();

        List<FeedbackPacket> versionDeltaFeedbackPacketsUp = versionDeltaFeedbackPacket
                .getFpList("up");
        List<FeedbackPacket> versionDeltaFeedbackPacketsDown = versionDeltaFeedbackPacket
                .getFpList("down");

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
                            versionDeltaFeedbackPacket.configFileName);
                    saveFullSequence(failureDir,
                            versionDeltaFeedbackPacket.fullSequence);
                }
                saveInconsistencyReport(failureDir,
                        versionDeltaFeedbackPacketUp.testPacketID,
                        versionDeltaFeedbackPacketUp.inconsistencyReport);
            }

            if (addToCorpusAfterUpgrade) {
                addSeedToCorpus(corpus, testID2Seed,
                        versionDeltaFeedbackPacketUp, score,
                        Corpus.QueueType.NEW_BRANCH_COVERAGE_NEW_VERSION_AFTER_UPGRADE);
            }
            if (addToCorpusAfterDowngrade) {
                addSeedToCorpus(corpus, testID2Seed,
                        versionDeltaFeedbackPacketDown, score,
                        Corpus.QueueType.NEW_BRANCH_COVERAGE_OLD_VERSION_AFTER_DOWNGRADE);
            }
        }
        // update testid2Seed, no use anymore
        for (int testID : versionDeltaFeedbackPacket.testIDs) {
            testID2Seed.remove(testID);
        }
        if (versionDeltaFeedbackPacket.hasERRORLog) {
            if (failureDir == null) {
                failureDir = createFailureDir(
                        versionDeltaFeedbackPacket.configFileName);
                saveFullSequence(failureDir,
                        versionDeltaFeedbackPacket.fullSequence);
            }
            saveErrorReport(failureDir,
                    versionDeltaFeedbackPacket.errorLogReport, startTestID,
                    endTestID);
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

    private void saveErrorReport(Path failureDir, String report, int testID) {
        Path errorSubDir = createErrorSubDir(failureDir);
        Path reportPath = Paths.get(
                errorSubDir.toString(),
                String.format("error_%d.report", testID));
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
                "BC queue size : " + corpus.getSize(
                        Corpus.QueueType.BRANCH_COVERAGE_BEFORE_VERSION_CHANGE),
                "BC index : "
                        + corpus.getIndex(
                                Corpus.QueueType.BRANCH_COVERAGE_BEFORE_VERSION_CHANGE),
                "cur testID : " + testID,
                "total exec : " + finishedTestID);
        if (Config.getConf().testSingleVersion) {
            System.out.format("|%30s|%30s|\n",
                    "run time : " + timeElapsed + "s",
                    "BC : " + originalCoveredBranches + "/"
                            + originalProbeNum);
        } else {
            System.out.format("|%30s|%30s|%30s|%30s|\n",
                    "run time : " + timeElapsed + "s",
                    "round : " + round,
                    "ori BC : " + originalCoveredBranches + "/"
                            + originalProbeNum,
                    "up BC : " + upgradedCoveredBranches + "/"
                            + upgradedProbeNum);
        }
        // Format Coverage Info
        if (Config.getConf().useFormatCoverage
                && (Config.getConf().formatCoverageChoiceProb > 0)) {
            System.out.format("|%30s|%30s|%30s|%30s|\n",
                    "FC queue size : "
                            + corpus.getSize(Corpus.QueueType.FORMAT_COVERAGE),
                    "FC index : "
                            + corpus.getIndex(Corpus.QueueType.FORMAT_COVERAGE),
                    "ori new format num : " + oriNewFormatNum,
                    "up new format num : " + upNewFormatNum);
        }
        // Version Delta Info
        if (Config.getConf().useVersionDelta
                && (Config.getConf().branchVersionDeltaChoiceProb > 0)) {
            System.out.format("|%30s|%30s|%30s|%30s|\n",
                    "exec group 1 : " + finishedTestIdAgentGroup1,
                    "exec group 2 : " + finishedTestIdAgentGroup2,
                    "BC delta queue size : " + corpus.getSize(
                            Corpus.QueueType.BRANCH_COVERAGE_VERSION_DELTA),
                    "FC delta queue size : " + corpus.getSize(
                            Corpus.QueueType.FORMAT_COVERAGE_VERSION_DELTA));
            System.out.format("|%30s|%30s|%30s|%30s|\n",
                    "post upg queue size : " + corpus.getSize(
                            Corpus.QueueType.NEW_BRANCH_COVERAGE_NEW_VERSION_AFTER_UPGRADE),
                    "post downg queue size : " + corpus.getSize(
                            Corpus.QueueType.NEW_BRANCH_COVERAGE_OLD_VERSION_AFTER_DOWNGRADE),
                    "only new BC delta : "
                            + executionCountWithOnlyNewBranchDelta,
                    "only new FC delta : "
                            + executionCountWithOnlyNewFormatDelta);
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

        if (Config.getConf().debug)
            System.out.format("|%30s|%30s|\n",
                    "testID2Seed size : " + testID2Seed.size(),
                    "stackedTestPackets size : " + stackedTestPackets.size());
        if (Config.getConf().debug) {
            logger.info("[HKLOG] insignificant inconsistencies in: "
                    + insignificantInconsistenciesIn.toString());
            logger.info("[HKLOG: Seed tracker] Mutated Seed IDs: "
                    + mutatedSeedIds.toString());
            logger.info("[HKLOG] buffer details: ");
            testBatchCorpus.printCache();
        }

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

    public int getSeedOrTestType(double[] cumulativeProbabilities) {
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

    public int getNextBestType(Map<Integer, Double> probabilities, int type) {
        // Sort probabilities in descending order
        List<Map.Entry<Integer, Double>> sortedProbabilities = new ArrayList<>(
                probabilities.entrySet());
        sortedProbabilities.sort((entry1, entry2) -> Double
                .compare(entry2.getValue(), entry1.getValue()));

        // Iterate through sorted probabilities
        for (Map.Entry<Integer, Double> entry : sortedProbabilities) {
            int elementIndex = entry.getKey();
            if (type == 0) {
                if (!corpus.isEmpty(Corpus.QueueType.values()[elementIndex])) {
                    return elementIndex; // Return the index of the non-empty
                                         // list
                }
            } else {
                if (!testBatchCorpus.isEmpty(InterestingTestsCorpus.TestType
                        .values()[elementIndex])) {
                    return elementIndex; // Return the index of the non-empty
                                         // list
                }
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

    public void saveSeed(CommandSequence commandSequence,
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
