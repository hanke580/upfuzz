package org.zlab.upfuzz.fuzzingengine.Server;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
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
import org.zlab.upfuzz.fuzzingengine.Packet.FeedbackPacket;
import org.zlab.upfuzz.fuzzingengine.Packet.StackedFeedbackPacket;
import org.zlab.upfuzz.fuzzingengine.Packet.StackedTestPacket;
import org.zlab.upfuzz.fuzzingengine.executor.Executor;
import org.zlab.upfuzz.hdfs.HdfsCommandPool;
import org.zlab.upfuzz.hdfs.HdfsExecutor;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.Pair;
import org.zlab.upfuzz.utils.Utilities;

public class FuzzingServer {

    static Logger logger = LogManager.getLogger(FuzzingServer.class);

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

    public synchronized StackedTestPacket getOneTest() {
        if (!stackedTestPackets.isEmpty()) {
            return stackedTestPackets.poll();
        }
        logger.debug("test packet queue is empty, try generate some");
        fuzzOne();
        assert !stackedTestPackets.isEmpty();
        return stackedTestPackets.poll();
    }

    private void fuzzOne() {
        // All packets have been dispatched, now fuzz next seeds
        Seed seed = corpus.getSeed();
        round++;
        StackedTestPacket stackedTestPacket = null;
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
            // get a seed from corpus, now fuzz it for a epoch
            stackedTestPacket = new StackedTestPacket();
            for (int i = 0; i < Config.getConf().mutationEpoch; i++) {
                if (i != 0 && i % Config.getConf().STACKED_TESTS_NUM == 0) {
                    stackedTestPackets.add(stackedTestPacket);
                    stackedTestPacket = new StackedTestPacket();
                } else {
                    // Mutation
                    Seed mutateSeed = SerializationUtils.clone(seed);
                    if (mutateSeed.mutate()) {
                        testID2Seed.put(testID, mutateSeed);
                        stackedTestPacket.addTestPacket(mutateSeed, testID++);
                    }
                }
            }
            if (stackedTestPacket != null && stackedTestPacket.size() != 0) {
                stackedTestPackets.add(stackedTestPacket);
            }
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
            if (Utilities.hasNewBits(
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
                Utilities.write2TXT(crashReport.toFile(), sb, false);
                crashID++;
            }
            // Remove the seed from the waiting list
            testID2Seed.remove(feedbackPacket.testPacketID);
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
