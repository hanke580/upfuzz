package org.zlab.upfuzz.fuzzingengine.Server;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
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
import org.zlab.upfuzz.fuzzingengine.Fuzzer;
import org.zlab.upfuzz.fuzzingengine.Packet.*;
import org.zlab.upfuzz.fuzzingengine.executor.Executor;
import org.zlab.upfuzz.fuzzingengine.testplan.TestPlan;
import org.zlab.upfuzz.fuzzingengine.testplan.event.Event;
import org.zlab.upfuzz.fuzzingengine.testplan.event.command.ShellCommand;
import org.zlab.upfuzz.fuzzingengine.testplan.event.fault.*;
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
    private int testID = 0;
    private int finishedTestID = 0;

    public CommandPool commandPool; // Command Definition
    public Executor executor;
    public Class<? extends State> stateClass;

    // seeds: sent to client and waiting for Feedback
    private final Map<Integer, Seed> testID2Seed;
    // stackedTestPackets to be sent to clients
    private final Queue<StackedTestPacket> stackedTestPackets;
    // test plan could contains (1) cmd (2) fault (3) upgrade op
    private final Queue<TestPlanPacket> testPlanPackets;

    // When merge new branches, increase this number
    public static int originalCoveredBranches = 0;
    public static int originalProbeNum = 0;
    public static int upgradedCoveredBranches = 0;
    public static int upgradedProbeNum = 0;

    public static List<Pair<Integer, Integer>> originalCoverageAlongTime = new ArrayList<>(); // time:
    public static List<Pair<Integer, Integer>> upgradedCoverageAlongTime = new ArrayList<>(); // time:

    public static long lastTimePoint = 0;
    public long startTime;

    ExecutionDataStore curCoverage;
    ExecutionDataStore upCoverage;

    public static int round = 0;
    public static int epoch = 0;
    public static int crashID = 0;

    public FuzzingServer() {
        testID2Seed = new HashMap<>();
        stackedTestPackets = new LinkedList<>();
        testPlanPackets = new LinkedList<>();
        curCoverage = new ExecutionDataStore();
        upCoverage = new ExecutionDataStore();

        startTime = TimeUnit.SECONDS.convert(System.nanoTime(),
                TimeUnit.NANOSECONDS);
    }

    private void init() {
        if (Config.getConf().initSeedDir != null) {
            corpus.initCorpus(Paths.get(Config.getConf().initSeedDir));
        }

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
    }

    public void start() {
        init();
        new Thread(new FuzzingServerSocket(this)).start();

        // new Thread(new FuzzingServerDispatcher(this)).start();
    }

    public synchronized Packet getOneTest() {
        // TODO: getOneTest could return a stacked or single test plan
        // We should always dispatch test plan to a specific set of clients
        // And the stacked tests to another set of clients.

        // Do we dispatch a test plan or a stacked test packet?
        // When we do testing, we start 20 clients for single-node full stop
        // upgrade
        // testing. And 10-20 nodes for rolling upgrade testing.
        // We make the full-stop upgrade to explore the test space, and utilize
        // the
        // seed from the corpus to generate the testPlan

        // Start from simple. Start up three nodes and only execute the test
        // plan!

        if (Config.getConf().fullStopTest) {
            if (!stackedTestPackets.isEmpty()) {
                return stackedTestPackets.poll();
            }
            fuzzOne();
            assert !stackedTestPackets.isEmpty();
            return stackedTestPackets.poll();
        } else {
            if (!testPlanPackets.isEmpty()) {
                return testPlanPackets.poll();
            }
            fuzzTestPlan();
            assert !testPlanPackets.isEmpty();
            return testPlanPackets.poll();
        }
    }

    private void fuzzOne() {
        // It should also generate test plan

        // Pick one test case from the corpus, fuzz it for mutationEpoch
        // Add the new tests into the stackedTestPackets
        // All packets have been dispatched, now fuzz next seeds
        Seed seed = corpus.getSeed();
        round++;
        StackedTestPacket stackedTestPacket;
        if (seed == null) {
            // corpus is empty, random generate one test packet and wait
            stackedTestPacket = new StackedTestPacket();
            for (int i = 0; i < Config.getConf().STACKED_TESTS_NUM; i++) {
                seed = Executor.generateSeed(commandPool, stateClass);
                if (seed != null) {
                    testID2Seed.put(testID, seed);
                    stackedTestPacket.addTestPacket(seed, testID++);
                }
            }
            if (stackedTestPacket != null && stackedTestPacket.size() != 0) {
                stackedTestPackets.add(stackedTestPacket);
            } else {
                logger.error("failed to generate any test packet");
            }
        } else {
            // get a seed from corpus, now fuzz it for an epoch
            stackedTestPacket = new StackedTestPacket();
            for (int i = 0; i < Config.getConf().mutationEpoch; i++) {
                // logger.info("Generating " + i + " packet");
                if (i != 0 && i % Config.getConf().STACKED_TESTS_NUM == 0) {
                    stackedTestPackets.add(stackedTestPacket);
                    stackedTestPacket = new StackedTestPacket();
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
            if (stackedTestPacket != null && stackedTestPacket.size() != 0) {
                stackedTestPackets.add(stackedTestPacket);
            }
        }
    }

    private void fuzzTestPlan() {
        // We use the seed from the corpus and generate a set of test plans
        // Do we consume a seed? We shouldn't. Since we don't mutate seed.
        // Do we add seed back? Do we want this code coverage? I think so,
        // There should be some coverage that can only be reached during the
        // rolling upgrade.
        // Then we should also keep a corpus of test plan. And the mutation to
        // the test plan would be changing the order between upgradeOp, faults
        // and commands.

        // Let's go with the simplest one. Randomly generate a seed, then a test
        // plan. And finally, execute it.
        Seed seed = corpus.getSeed();
        round++;
        if (seed == null) {
            seed = Executor.generateSeed(commandPool, stateClass);
            if (seed != null) {
                for (int i = 0; i < Config.getConf().testPlanEpoch; i++) {
                    TestPlan testPlan = generateTestPlan(seed);
                    testPlanPackets.add(new TestPlanPacket(
                            Config.getConf().system, testID++, testPlan));
                }
            }
        }
    }

    // We never upgrade a node with fault, all fault will be
    // recovered before upgrading the node
    public TestPlan generateTestPlan(Seed seed) {
        List<Event> events = new LinkedList<>();

        // Some systems might have special requirements for
        // upgrade, like HDFS needs to upgrade NN first
        int faultNum = rand.nextInt(Config.getConf().faultMaxNum + 1);
        List<Pair<Fault, FaultRecover>> faultPairs = new LinkedList<>();
        for (int i = 0; i < faultNum; i++) {
            Pair<Fault, FaultRecover> faultPair = Fault
                    .randomGenerateFault(Config.getConf().nodeNum);
            if (faultPair != null) {
                faultPairs.add(faultPair);
            } else {
                logger.info("Fault Generation Failed");
            }
        }

        // TODO
        // Another way: We shuffle upgradeOp, faults
        // Then we find each fault, and randomly inject the recover in its
        // behind.
        List<Event> upgradeOpAndFaults = new LinkedList<>();
        if (Config.getConf().upgradeWithFault) {
            // ugprade operation can happen when there is a failure
            for (int i = 0; i < Config.getConf().nodeNum; i++) {
                upgradeOpAndFaults.add(new UpgradeOp(i));
            }
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
                            // Remove the upgrade operation since there's no
                            // recover
                            upgradeOpAndFaults.remove(nodeUpgradePos);
                            continue;
                        }
                        // [pos1 + 1, nodeUpgradePos]: Num: nodeUpgradePos -
                        // pos1
                        // rand.nextInt(nodeUpgradePos - pos1) + pos1 + 1
                        pos2 = Utilities.randWithRange(rand, pos1 + 1,
                                nodeUpgradePos + 1);
                    }
                }
                if (faultPair.right != null)
                    upgradeOpAndFaults.add(pos2, faultPair.right);
            }
        } else {
            // Upgrade will not be executed under a fault
            Map<Integer, Pair<Fault, FaultRecover>> faultMap = new HashMap<>();
            int idx = 0;
            for (Pair<Fault, FaultRecover> faultPair : faultPairs) {
                faultMap.put(idx++, faultPair);
            }
            Map<Integer, UpgradeOp> upgradeOpMap = new HashMap<>();
            for (int i = 0; i < Config.getConf().nodeNum; i++) {
                upgradeOpMap.put(idx++, new UpgradeOp(i));
            }
            List<Integer> eventIndexes = new LinkedList<>();
            for (int i = 0; i < idx; i++) {
                eventIndexes.add(i);
            }
            Collections.shuffle(eventIndexes);

            int nodeIdx = 0;
            for (int i = 0; i < idx; i++) { // scan event indexes
                if (faultMap.containsKey(eventIndexes.get(i))) {
                    Pair<Fault, FaultRecover> faultPair = faultMap
                            .get(eventIndexes.get(i));
                    upgradeOpAndFaults.add(faultPair.left);
                    if (faultPair.right != null) {
                        upgradeOpAndFaults
                                .add(faultPair.right);
                    }
                } else {
                    if (Config.getConf().shuffleUpgradeOrder) {
                        upgradeOpAndFaults
                                .add(upgradeOpMap.get(eventIndexes.get(i)));
                    } else {
                        upgradeOpAndFaults.add(new UpgradeOp(nodeIdx++));
                    }
                }
            }

            // An addition scan to remove the upgrade op after a node failure
            // without crash
            Set<Integer> removeIdx = new HashSet<>();
            for (int i = 0; i < upgradeOpAndFaults.size(); i++) {
                if (upgradeOpAndFaults.get(i) instanceof NodeFailure) {
                    int nodeIndex = ((NodeFailure) upgradeOpAndFaults
                            .get(i)).nodeIndex;
                    // find whether there is any upgrade operation of this node
                    for (int j = i + 1; j < upgradeOpAndFaults.size(); j++) {
                        if (upgradeOpAndFaults
                                .get(j) instanceof NodeFailureRecover) {
                            if (((NodeFailureRecover) upgradeOpAndFaults
                                    .get(j)).nodeIndex == nodeIndex) {
                                break;
                            }
                        }
                        if (upgradeOpAndFaults.get(j) instanceof UpgradeOp) {
                            if (((UpgradeOp) upgradeOpAndFaults
                                    .get(j)).nodeIndex == nodeIndex) {
                                // remove this upgrade op
                                removeIdx.add(j);
                                break;
                            }
                        }
                    }
                }
            }
            List<Event> updatedUpgradeOpAndFaults = new LinkedList<>();
            for (int i = 0; i < upgradeOpAndFaults.size(); i++) {
                if (!removeIdx.contains(i)) {
                    updatedUpgradeOpAndFaults.add(upgradeOpAndFaults.get(i));
                }
            }
            upgradeOpAndFaults = updatedUpgradeOpAndFaults;
        }

        // TODO: If the node is current down, we should switch to
        // another node for execution.
        // Randomly interleave the commands with the upgradeOp&faults

        List<Event> shellCommands = ShellCommand.seed2Events(seed);

        int upgradeOpAndFaultsSize = upgradeOpAndFaults.size();
        int commandsSize = shellCommands.size();
        int totalEventSize = upgradeOpAndFaultsSize + commandsSize;
        int upgradeOpAndFaultsIdx = 0;
        int commandIdx = 0;
        for (int i = 0; i < totalEventSize; i++) {
            // Magic Number: Prefer to execute commands first
            // Also make the commands more separate
            if (Utilities.oneOf(rand, 5)) {
                if (upgradeOpAndFaultsIdx < upgradeOpAndFaults.size())
                    events.add(upgradeOpAndFaults.get(upgradeOpAndFaultsIdx++));
                else
                    break;
            } else {
                if (commandIdx < commandsSize)
                    events.add(shellCommands
                            .get(commandIdx++));
                else
                    break;
            }
        }
        if (upgradeOpAndFaultsIdx < upgradeOpAndFaultsSize) {
            for (int i = upgradeOpAndFaultsIdx; i < upgradeOpAndFaultsSize; i++) {
                events.add(upgradeOpAndFaults.get(i));
            }
        } else if (commandIdx < commandsSize) {
            for (int i = commandIdx; i < commandsSize; i++) {
                events.add(shellCommands.get(i));
            }
        }
        return new TestPlan(events);
    }

    public synchronized void updateStatus(
            TestPlanFeedbackPacket testPlanFeedbackPacket) {
        logger.info("test plan feedback: update status");
        // TODO: update status for test plan feed back
        // Do we utilize the feedback?
        // Do we mutate the test plan?

        if (Config.getConf().useFeedBack && Utilities.hasNewBits(curCoverage,
                testPlanFeedbackPacket.feedBack.upgradedCodeCoverage)) {
            // Add to test plan corpus???
            Pair<Integer, Integer> upCoverageStatus = Utilities
                    .getCoverageStatus(
                            testPlanFeedbackPacket.feedBack.upgradedCodeCoverage);
            logger.info(String.format(
                    "[TestPlan Coverage] covered branch num = %d, total branch = %d",
                    upCoverageStatus.left, upCoverageStatus.right));
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
            // comparing the read/state failed
        }

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
            if (Config.getConf().useFeedBack && Utilities.hasNewBits(
                    curCoverage,
                    feedbackPacket.feedBack.originalCodeCoverage)) {
                // Write Seed to Disk + Add to Corpus
                Seed seed = testID2Seed.get(feedbackPacket.testPacketID);
                Fuzzer.saveSeed(seed.originalCommandSequence,
                        seed.validationCommandSequnece);
                corpus.addSeed(seed);
                curCoverage.merge(feedbackPacket.feedBack.originalCodeCoverage);
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
        Pair<Integer, Integer> coverageStatus = Utilities
                .getCoverageStatus(curCoverage);
        originalCoveredBranches = coverageStatus.left;
        originalProbeNum = coverageStatus.right;

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
        Long timeElapsed = TimeUnit.SECONDS.convert(
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
                        + "Finished Test Num = " + finishedTestID + "|" +
                        "Covered Branches Num = " + originalCoveredBranches
                        + "|"
                        + "Total Branch Num = " + originalProbeNum + "|"
                        + "Time Elapsed = " + timeElapsed + "s"
                        + "|"
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

    enum FuzzingServerActions {
        start
    }
}
