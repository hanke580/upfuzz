package org.zlab.upfuzz.fuzzingengine.server;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
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
import org.zlab.upfuzz.fuzzingengine.configgen.ConfigGen;
import org.zlab.upfuzz.fuzzingengine.packet.*;
import org.zlab.upfuzz.fuzzingengine.executor.Executor;
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

    public static int failureId = 0;

    public static int fullStopCrashNum = 0;
    public static int eventCrashNum = 0;
    public static int inconsistencyNum = 0;
    public static int errorLogNum = 0;

    boolean isFullStopUpgrade = true;

    ConfigGen configGen;
    public Path configDirPath;

    // ----------test plan----------
    public int testPlanMutationRetry = 50;

    public FuzzingServer() {
        testID2Seed = new HashMap<>();
        testID2TestPlan = new HashMap<>();
        stackedTestPackets = new LinkedList<>();
        fullStopPackets = new LinkedList<>();
        testPlanPackets = new LinkedList<>();
        curOriCoverage = new ExecutionDataStore();
        curUpCoverage = new ExecutionDataStore();

        configDirPath = Paths.get(System.getProperty("user.dir"),
                Config.getConf().configDir, Config.getConf().originalVersion
                        + "_" + Config.getConf().upgradedVersion);

        startTime = TimeUnit.SECONDS.convert(System.nanoTime(),
                TimeUnit.NANOSECONDS);
    }

    private void init() {
        if (Config.getConf().initSeedDir != null) {
            corpus.initCorpus(Paths.get(Config.getConf().initSeedDir));
        }

        // maintain the num of configuration files
        // read all configurations file name in a list
        configGen = new ConfigGen();

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
            // always execute one test case (to verify whether a bug really
            // exists)
            return generateExampleFullStopPacket();

        } else if (Config.getConf().testingMode == 2) {
            return generateMixedTestPacket();
        } else if (Config.getConf().testingMode == 3) {
            logger.info("execute example test plan");
            return generateExampleTestplanPacket();
        } else if (Config.getConf().testingMode == 4) {
            // test full-stop and rolling upgrade iteratively
            Packet packet;
            if (isFullStopUpgrade) {
                if (stackedTestPackets.isEmpty())
                    fuzzOne();
                assert !stackedTestPackets.isEmpty();
                packet = stackedTestPackets.poll();
            } else {
                // packet = generateMixedTestPacket();
                if (testPlanPackets.isEmpty())
                    fuzzTestPlan();
                packet = testPlanPackets.poll();
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
        Seed seed = corpus.getSeed();
        round++;
        StackedTestPacket stackedTestPacket;

        int configIdx = configGen.generateConfig();
        String configFileName = "test" + configIdx;
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

                    // Each upgrade should execute with different config
                    configIdx = configGen.generateConfig();
                    configFileName = "test" + configIdx;

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
                    logger.debug("Mutation failed");
                    i--;
                }
            }
            if (stackedTestPacket.size() != 0) {
                stackedTestPackets.add(stackedTestPacket);
            }
        }
    }

    private void fuzzTestPlan() {
        // We should first try to mutate the test plan, but there
        // should still be possibility for generating a new test plan
        // Mutate a testplan
        TestPlan testPlan = testPlanCorpus.getTestPlan();

        if (testPlan != null) {
            for (int i = 0; i < Config.getConf().testPlanMutationEpoch; i++) {
                TestPlan mutateTestPlan = null;
                int j = 0;
                for (; j < testPlanMutationRetry; j++) {
                    mutateTestPlan = SerializationUtils.clone(testPlan);
                    mutateTestPlan.mutate();
                    if (testPlanVerifier(mutateTestPlan.getEvents(),
                            testPlan.nodeNum)) {
                        break;
                    }
                }
                if (j == testPlanMutationRetry)
                    return;
                testID2TestPlan.put(testID, mutateTestPlan);

                int configIdx = configGen.generateConfig();
                String configFileName = "test" + configIdx;

                testPlanPackets.add(new TestPlanPacket(
                        Config.getConf().system,
                        testID++, configFileName, mutateTestPlan));
            }
        } else {

            // disable system state comparison
            FullStopSeed fullStopSeed = fullStopCorpus.getSeed();
            if (fullStopSeed == null) {
                // genenrate a full-stop upgrade, do not compare read results
                Seed seed;
                if (corpus.isEmpty()) {
                    // random generate a fullStopSeed
                    seed = Executor.generateSeed(commandPool, stateClass);
                } else {
                    seed = corpus.peekSeed();
                }
                fullStopSeed = new FullStopSeed(seed, Config.getConf().nodeNum,
                        new HashMap<>(), new LinkedList<>());
            }

            // Generate several test plan...
            for (int i = 0; i < Config.getConf().testPlanGenerationNum; i++) {

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
    }

    public boolean fuzzFullStopUpgrade() {
        FullStopSeed fullStopSeed = fullStopCorpus.getSeed();
        round++;
        if (fullStopSeed == null) {
            // corpus is empty, generate some
            Seed seed = Executor.generateSeed(commandPool, stateClass);

            if (seed != null) {
                int configIdx = configGen.generateConfig();
                String configFileName = "test" + configIdx;

                FullStopUpgrade fullStopUpgrade = new FullStopUpgrade(
                        Config.getConf().nodeNum,
                        seed.originalCommandSequence.getCommandStringList(),
                        seed.validationCommandSequnece.getCommandStringList(),
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
        List<String> commands = readcommands(
                commandPath.resolve("commands.txt"));
        List<String> validcommands = readcommands(
                commandPath.resolve("validcommands.txt"));
        Set<String> targetSystemStates = new HashSet<>();

        logger.info("commands size = " + commands.size());
        logger.info("validcommands size = " + validcommands.size());

        FullStopUpgrade fullStopUpgrade = new FullStopUpgrade(
                Config.getConf().nodeNum,
                commands,
                validcommands,
                targetSystemStates);
        // TODO: Change this to the configIdx you want to test
        int configIdx = configGen.generateConfig();
        // int configIdx = 470;

        String configFileName = "test" + configIdx;
        FullStopPacket fullStopPacket = new FullStopPacket(
                Config.getConf().system,
                testID++, configFileName, fullStopUpgrade);
        logger.info("config path = " + configFileName);
        return fullStopPacket;
    }

    public TestPlan generateExampleTestPlan() {
        List<Event> exampleEvents = new LinkedList<>();
        int nodeNum = 3;
        if (Config.getConf().system.equals("hdfs"))
            nodeNum = 4;
        exampleEvents.add(new ShellCommand(
                "CREATE KEYSPACE IF NOT EXISTS uuid54944042eef940e88c689d5c5f9517ae WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };"));

        exampleEvents.add(new ShellCommand(
                "CREATE TABLE  uuid54944042eef940e88c689d5c5f9517ae.wYd (wYd TEXT,kwkJcHpnkNUZtvdD TEXT,BhAkXweAcvAJnpcGfuyr TEXT, PRIMARY KEY (BhAkXweAcvAJnpcGfuyr, kwkJcHpnkNUZtvdD )) WITH speculative_retry = 'always';"));

        exampleEvents.add(new PrepareUpgrade());

        if (Config.getConf().system.equals("hdfs")) {
            exampleEvents.add(new HDFSStopSNN());
        }

        exampleEvents.add(new UpgradeOp(0));

        exampleEvents.add(new ShellCommand(
                "CREATE TABLE  uuid54944042eef940e88c689d5c5f9517ae.tb2 (wYd TEXT,kwkJcHpnkNUZtvdD TEXT,BhAkXweAcvAJnpcGfuyr TEXT, PRIMARY KEY (BhAkXweAcvAJnpcGfuyr, kwkJcHpnkNUZtvdD )) WITH speculative_retry = 'always';"));

        exampleEvents.add(new ShellCommand(
                "INSERT INTO uuid54944042eef940e88c689d5c5f9517ae.wYd (BhAkXweAcvAJnpcGfuyr, kwkJcHpnkNUZtvdD) VALUES ('val1','val2');"));

        exampleEvents.add(new ShellCommand(
                "SELECT * FROM uuid54944042eef940e88c689d5c5f9517ae.wYd;"));

        exampleEvents.add(new ShellCommand(
                "TRUNCATE TABLE uuid54944042eef940e88c689d5c5f9517ae.wYd;"));

        exampleEvents.add(new UpgradeOp(1));
        exampleEvents.add(new UpgradeOp(2));

        Set<String> targetSystemStates = new HashSet<>();
        Map<Integer, Map<String, String>> oracle = new HashMap<>();
        List<String> validationCommands = new LinkedList<>();
        List<String> validationReadResultsOracle = new LinkedList<>();

        return new TestPlan(nodeNum, exampleEvents, targetSystemStates,
                oracle, validationCommands, validationReadResultsOracle);
    }

    public TestPlan generateTestPlan(FullStopSeed fullStopSeed) {
        // Some systems might have special requirements for
        // upgrade, like HDFS needs to upgrade NN.
        int nodeNum = fullStopSeed.nodeNum;
        List<Event> upgradeOps = new LinkedList<>();
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
        }
        upgradeOps.add(0, new PrepareUpgrade());

        // -----------fault----------
        int faultNum = rand.nextInt(Config.getConf().faultMaxNum + 1);
        List<Pair<Fault, FaultRecover>> faultPairs = Fault
                .randomGenerateFaults(nodeNum, faultNum);
        List<Event> upgradeOpAndFaults = interleaveFaultAndUpgradeOp(faultPairs,
                upgradeOps);

        if (!testPlanVerifier(upgradeOpAndFaults, nodeNum)) {
            return null;
        }

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
                    fullStopSeed.targetSystemStateResults, new LinkedList<>(),
                    new LinkedList<>());
        }

        // -----------verifier----------
        // Make sure the test plan is executable
        /**
         * There are several patterns
         * Cassandra
         * Pattern1
         * - If we isolate the seed node (node0), all the other nodes
         * cannot upgrade.
         * Pattern2
         * - If we isolate a node which is not a seed node, then we cannot upgrade it
         * - (If the fault is not recovered)
         *
         * Pattern3
         * - If we crash the seed node, then all the other nodes cannot upgrade
         *
         * HDFS
         * - Shared the two patterns, but has another pattern
         * - For a node, if there's a bi-link fault between the node
         * - and the NN (node0), then we cannot upgrade it.
         */

        // Randomly interleave the commands with the upgradeOp&faults
        List<Event> shellCommands = new LinkedList<>();
        if (fullStopSeed.seed != null)
            shellCommands = ShellCommand.seedWriteCmd2Events(fullStopSeed.seed);
        else
            logger.error("empty full stop seed");

        List<Event> events = interleaveWithOrder(upgradeOpAndFaults,
                shellCommands);

        events.add(events.size(), new FinalizeUpgrade());
        return new TestPlan(nodeNum, events, targetSystemStates,
                fullStopSeed.targetSystemStateResults,
                fullStopSeed.seed.validationCommandSequnece
                        .getCommandStringList(),
                fullStopSeed.validationReadResults);
    }

    public boolean testPlanVerifier(List<Event> events, int nodeNum) {
        // check connection status to the seed node
        boolean[][] connection = new boolean[nodeNum][nodeNum];
        for (int i = 0; i < nodeNum; i++) {
            for (int j = 0; j < nodeNum; j++) {
                connection[i][j] = true;
            }
        }
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
                    || event instanceof DowngradeOp) {
                int nodeIdx;
                if (event instanceof UpgradeOp)
                    nodeIdx = ((UpgradeOp) event).nodeIndex;
                else
                    nodeIdx = ((DowngradeOp) event).nodeIndex;
                if (nodeIdx == 0)
                    continue;
                if (Config.getConf().system.equals("hdfs")) {
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
        // check upgrade while seed node is down
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
            } else if (event instanceof UpgradeOp) {
                int nodeIdx = ((UpgradeOp) event).nodeIndex;
                if (nodeIdx != 0 && !isSeedAlive) {
                    return false;
                }
            }
        }

        return true;
    }

    public List<String> readcommands(Path path) {
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

        if (addToCorpus) {
            fullStopCorpus.addSeed(new FullStopSeed(
                    testID2Seed.get(fullStopFeedbackPacket.testPacketID),
                    Config.getConf().nodeNum,
                    fullStopFeedbackPacket.systemStates, null));

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

        Path failureDir;
        if (fullStopFeedbackPacket.isUpgradeProcessFailed
                || fullStopFeedbackPacket.isInconsistent
                || fullStopFeedbackPacket.hasERRORLog) {
            failureDir = createFailureDir(
                    fullStopFeedbackPacket.configFileName);
            saveFullSequence(failureDir, fullStopFeedbackPacket.fullSequence);
            if (fullStopFeedbackPacket.isUpgradeProcessFailed) {
                saveFullStopCrashReport(failureDir,
                        fullStopFeedbackPacket.upgradeFailureReport);
            }
            if (fullStopFeedbackPacket.isInconsistent) {
                saveInconsistencyReport(failureDir,
                        fullStopFeedbackPacket.testPacketID,
                        fullStopFeedbackPacket.inconsistencyReport);
            }
            if (fullStopFeedbackPacket.hasERRORLog) {
                saveErrorReport(failureDir,
                        fullStopFeedbackPacket.errorLogReport);
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

    public synchronized void updateStatus(
            TestPlanFeedbackPacket testPlanFeedbackPacket) {

        FeedBack fb = mergeCoverage(testPlanFeedbackPacket.feedBacks);
        boolean addToCorpus = false;
        if (Config.getConf().useFeedBack) {
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
                        testPlanFeedbackPacket.errorLogReport);
            }
        }
        testID2TestPlan.remove(testPlanFeedbackPacket.testPacketID);

        finishedTestID++;
        printInfo();
        System.out.println();
    }

    public synchronized void updateStatus(
            StackedFeedbackPacket stackedFeedbackPacket) {
        Path failureDir = null;

        if (stackedFeedbackPacket.isUpgradeProcessFailed) {
            failureDir = createFailureDir(stackedFeedbackPacket.configFileName);
            saveFullSequence(failureDir, stackedFeedbackPacket.fullSequence);
            saveFullStopCrashReport(failureDir,
                    stackedFeedbackPacket.upgradeFailureReport);
            finishedTestID++;
        }
        FuzzingServerHandler.printClientNum();
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
                    Seed seed = testID2Seed
                            .get(feedbackPacket.testPacketID);
                    Fuzzer.saveSeed(seed.originalCommandSequence,
                            seed.validationCommandSequnece);
                    fullStopCorpus.addSeed(
                            new FullStopSeed(seed, feedbackPacket.nodeNum,
                                    null,
                                    feedbackPacket.validationReadResults));
                    corpus.addSeed(seed);
                }
            }
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
            // Remove the seed from the waiting list
            testID2Seed.remove(feedbackPacket.testPacketID);
        }

        if (stackedFeedbackPacket.hasERRORLog) {
            if (failureDir == null) {
                failureDir = createFailureDir(
                        stackedFeedbackPacket.configFileName);
                saveFullSequence(failureDir,
                        stackedFeedbackPacket.fullSequence);
            }
            saveErrorReport(failureDir,
                    stackedFeedbackPacket.errorLogReport);
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

    private void saveFullStopCrashReport(Path failureDir,
            String report) {
        Path subDir = createFullStopCrashSubDir(failureDir);
        Path crashReportPath = Paths.get(
                subDir.toString(),
                "fullstop_crash.report");
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

    private void saveErrorReport(Path failureDir, String report) {
        Path errorSubDir = createErrorSubDir(failureDir);
        Path reportPath = Paths.get(
                errorSubDir.toString(),
                "error.report");
        Utilities.write2TXT(reportPath.toFile(), report, false);
        errorLogNum++;
    }

    public void printInfo() {
        long timeElapsed = TimeUnit.SECONDS.convert(
                System.nanoTime(), TimeUnit.NANOSECONDS) - startTime;

        System.out.println("--------------------------------------------------"
                +
                " TestStatus ---------------------------------------------------------------");
        System.out.println("System: " + Config.getConf().system + "\n"
                + "Upgrade: " + Config.getConf().originalVersion + "=>"
                + Config.getConf().upgradedVersion);
        System.out.println(
                "============================================================"
                        + "=================================================================");
        System.out.format("|%30s|%30s|%30s|%30s|\n",
                "queue size : " + corpus.queue.size(),
                "round : " + round,
                "cur testID : " + testID,
                "total exec : " + finishedTestID);
        System.out.format("|%30s|%30s|%30s|%30s|\n",
                "fullstop crash : " + fullStopCrashNum,
                "event crash : " + eventCrashNum,
                "inconsistency : " + inconsistencyNum,
                "error log : " + errorLogNum);
        System.out.format("|%30s|%30s|%30s|\n",
                "run time : " + timeElapsed + "s",
                "ori cov : " + originalCoveredBranches + "/"
                        + originalProbeNum,
                "up cov : " + upgradedCoveredBranches + "/"
                        + upgradedProbeNum);
        System.out.println(
                "------------------------------------------------------------"
                        + "-----------------------------------------------------------------");

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
}
