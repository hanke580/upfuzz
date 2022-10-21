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
import org.zlab.upfuzz.fuzzingengine.FeedBack;
import org.zlab.upfuzz.fuzzingengine.Fuzzer;
import org.zlab.upfuzz.fuzzingengine.Packet.*;
import org.zlab.upfuzz.fuzzingengine.executor.Executor;
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
    public TestPlanCorpus testPlanCorpus = new TestPlanCorpus();
    private int testID = 0;
    private int finishedTestID = 0;

    public CommandPool commandPool; // Command Definition
    public Executor executor;
    public Class<? extends State> stateClass;

    // seeds: sent to client and waiting for Feedback
    private final Map<Integer, Seed> testID2Seed;
    private final Map<Integer, TestPlan> testID2TestPlan;

    // stackedTestPackets to be sent to clients
    public final Queue<StackedTestPacket> stackedTestPackets;
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

    ExecutionDataStore curOriCoverage;
    ExecutionDataStore curUpCoverage;

    public static int round = 0;
    public static int epoch = 0;
    public static int crashID = 0;

    public FuzzingServer() {
        testID2Seed = new HashMap<>();
        testID2TestPlan = new HashMap<>();
        stackedTestPackets = new LinkedList<>();
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
        if (Config.getConf().testingMode == 0) {
            if (stackedTestPackets.isEmpty())
                fuzzOne();
            assert !stackedTestPackets.isEmpty();
            return stackedTestPackets.poll();
        } else if (Config.getConf().testingMode == 1) {
            if (testPlanPackets.isEmpty())
                fuzzTestPlan();
            assert !testPlanPackets.isEmpty();
            return testPlanPackets.poll();
        } else if (Config.getConf().testingMode == 2) {
            return generateMixedTestPacket();
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

        return new MixedTestPacket(stackedTestPacket, testPlanPacket);

    }

    public void fuzzOne() {
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
            Seed seed = corpus.peekSeed();

            // Might stuck here, this is serious
            while (seed == null) {
                seed = Executor.generateSeed(commandPool, stateClass);
            }
            testPlan = generateTestPlan(seed);
            testID2TestPlan.put(testID, testPlan);
            testPlanPackets.add(new TestPlanPacket(
                    Config.getConf().system,
                    testID++, testPlan));
        }
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

    // We never upgrade a node with fault, all fault will be
    // recovered before upgrading the node
    public TestPlan generateTestPlan(Seed seed) {
        // Some systems might have special requirements for
        // upgrade, like HDFS needs to upgrade NN first

        int nodeNum = Config.getConf().nodeNum;

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
            // exampleEvents.add(new UpgradeOp(3));
            // exampleEvents.add(0, new LinkFailure(1, 2));
            return new TestPlan(nodeNum, exampleEvents);
        }

        // TODO: If the node is current down, we should switch to
        // another node for execution.
        // Randomly interleave the commands with the upgradeOp&faults
        List<Event> shellCommands = ShellCommand.seed2Events(seed);
        List<Event> events = interleaveWithOrder(upgradeOpAndFaults,
                shellCommands);

        events.add(events.size(), new FinalizeUpgrade());
        return new TestPlan(nodeNum, events);
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
            // comparing the read/state failed
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

    enum FuzzingServerActions {
        start
    }

    public static List<Event> interleaveFaultAndUpgradeOp(
            List<Pair<Fault, FaultRecover>> faultPairs,
            List<Event> upgradeOps) {
        // Upgrade op can happen with fault
        // E.g. isolate node1 -> upgrade node1 -> recover node1
        List<Event> upgradeOpAndFaults = new LinkedList<>();
        for (Event upgradeOp : upgradeOps) {
            upgradeOpAndFaults.add(upgradeOp);
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
}
