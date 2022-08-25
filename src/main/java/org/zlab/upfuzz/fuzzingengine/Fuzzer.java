package org.zlab.upfuzz.fuzzingengine;

import info.debatty.java.stringsimilarity.QGram;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jacoco.core.data.ExecutionDataStore;
import org.zlab.upfuzz.CommandPool;
import org.zlab.upfuzz.CommandSequence;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.cassandra.CassandraCommandPool;
import org.zlab.upfuzz.cassandra.CassandraCommands;
import org.zlab.upfuzz.cassandra.CassandraExecutor;
import org.zlab.upfuzz.cassandra.CassandraState;
import org.zlab.upfuzz.fuzzingengine.Server.Seed;
import org.zlab.upfuzz.fuzzingengine.executor.Executor;
import org.zlab.upfuzz.hdfs.HdfsCommandPool;
import org.zlab.upfuzz.hdfs.HdfsExecutor;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.Pair;
import org.zlab.upfuzz.utils.Utilities;

public class Fuzzer {
    static Logger logger = LogManager.getLogger(Fuzzer.class);

    /**
     * start from one seed, fuzz it for a certain times. Also check the coverage
     * here?
     *
     * @param commandSequence
     * @param fromCorpus
     *            Whether the given seq is from the corpus. If yes, only run the
     *            mutated seed. If no, this seed also need run.
     * @return
     */
    public static final int TEST_NUM = 2000;

    public static QGram qGram = new QGram();

    /**
     * If a seed cannot be correctly mutated for more than five times, Discard
     * this test case.
     */
    public static final int MUTATE_RETRY_TIME = 10;
    public static int testID = 0;

    // When merge new branches, increase this number
    public static int originalCoveredBranches = 0;
    public static int originalProbeNum = 0;
    public static int upgradedCoveredBranches = 0;
    public static int upgradedProbeNum = 0;

    public static List<Pair<Integer, Integer>> originalCoverageAlongTime = new ArrayList<>(); // time:
                                                                                              // Coverage
    public static List<Pair<Integer, Integer>> upgradedCoverageAlongTime = new ArrayList<>(); // time:
                                                                                              // Coverage
    public static long lastTimePoint = 0;

    // seconds, now set it as every 10 mins
    public static long timeInterval = 600;

    public long startTime;

    public static int seedID = 0;
    public static int round = 0;
    public CommandPool commandPool;
    public Executor executor;
    public Class<? extends State> stateClass;

    /**
     * We could also only save path. Queue<Path>, then when need a command
     * sequence, deserialize it then. But now try with the most simple one.
     */
    Queue<Pair<CommandSequence, CommandSequence>> queue = new LinkedList<>();
    Random rand = new Random();
    ExecutionDataStore curCoverage = new ExecutionDataStore();
    ExecutionDataStore upCoverage = new ExecutionDataStore();
    FuzzingClient fuzzingClient = null;

    public Fuzzer() {
        if (Config.getConf().system.equals("cassandra")) {
            executor = new CassandraExecutor();
            commandPool = new CassandraCommandPool();
            stateClass = CassandraState.class;

            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    try {
                        Utilities.exec(
                                new String[] { "bin/nodetool", "stopdaemon" },
                                Config.getConf().oldSystemPath);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

        } else if (Config.getConf().system.equals("hdfs")) {
            executor = new HdfsExecutor();
            commandPool = new HdfsCommandPool();
            stateClass = HdfsState.class;

            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    try {
                        Utilities.exec(new String[] { "sbin/stop-dfs.sh" },
                                Config.getConf().oldSystemPath);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        fuzzingClient = new FuzzingClient();

        if (Config.getConf().initSeedDir != null) {
            // Start up, load all command sequence into a queue.
            System.out.println("seed path = " + Config.getConf().initSeedDir);
            Path initSeedDirPath = Paths.get(Config.getConf().initSeedDir);
            File initSeedDir = initSeedDirPath.toFile();
            assert initSeedDir.isDirectory() == true;
            for (File seedFile : initSeedDir.listFiles()) {
                if (!seedFile.isDirectory()) {
                    // Deserialize current file, and add it into the queue.
                    // TODO: Execute them before adding them to the queue.
                    // Make sure all the seed in the queue must have been
                    // executed.
                    Pair<CommandSequence, CommandSequence> commandSequencePair = Utilities
                            .deserializeCommandSequence(seedFile.toPath());
                    if (commandSequencePair != null) {
                        Fuzzer.saveSeed(commandSequencePair.left,
                                commandSequencePair.right);
                        queue.add(commandSequencePair);
                    }
                }
            }
        }
    }

    private void updateStatus(CommandSequence commandSequence,
            CommandSequence validationCommandSequence,
            ExecutionDataStore curCoverage, ExecutionDataStore upCoverage,
            Queue<Pair<CommandSequence, CommandSequence>> queue,
            boolean hasNewOrder, FeedBack testFeedBack) {

        System.out.println("fb original cc: " +
                (testFeedBack.originalCodeCoverage == null));
        System.out.println(
                "fb size: " +
                        testFeedBack.originalCodeCoverage.getContents().size());

        // Check new bits, update covered branches, add record (time, coverage)
        // pair
        if (hasNewOrder) {
            saveSeed(commandSequence, validationCommandSequence);
            queue.add(new Pair<>(commandSequence, validationCommandSequence));

            if (Utilities.hasNewBits(curCoverage,
                    testFeedBack.originalCodeCoverage)) {
                curCoverage.merge(testFeedBack.originalCodeCoverage);
            }
            // upCoverage.merge(testFeedBack.upgradedCodeCoverage);

            // Update the coveredBranches to the newest value
            Pair<Integer, Integer> coverageStatus = Utilities
                    .getCoverageStatus(curCoverage);
            originalCoveredBranches = coverageStatus.left;
            originalProbeNum = coverageStatus.right;
        } else if (Utilities.hasNewBits(curCoverage,
                testFeedBack.originalCodeCoverage)) {
            saveSeed(commandSequence, validationCommandSequence);
            queue.add(new Pair<>(commandSequence, validationCommandSequence));
            curCoverage.merge(testFeedBack.originalCodeCoverage);
            // upCoverage.merge(testFeedBack.upgradedCodeCoverage);

            // Update the coveredBranches to the newest value
            Pair<Integer, Integer> coverageStatus = Utilities
                    .getCoverageStatus(curCoverage);
            originalCoveredBranches = coverageStatus.left;
            originalProbeNum = coverageStatus.right;
            // Pair<Integer, Integer> upgradedCoverageStatus = Utilities
            // .getCoverageStatus(upCoverage);
            // upgradedCoveredBranches = upgradedCoverageStatus.left;
            // upgradedProbeNum = upgradedCoverageStatus.right;
        }

        // Disable the usage of new code coverage temporally

        // else if (Utilities.hasNewBits(upCoverage,
        // testFeedBack.upgradedCodeCoverage)) {
        // saveSeed(commandSequence, validationCommandSequence);
        // queue.add(new Pair<>(commandSequence, validationCommandSequence));
        // curCoverage.merge(testFeedBack.originalCodeCoverage);
        // upCoverage.merge(testFeedBack.upgradedCodeCoverage);
        //
        // // Update the coveredBranches to the newest value
        // Pair<Integer, Integer> coverageStatus = Utilities
        // .getCoverageStatus(curCoverage);
        // originalCoveredBranches = coverageStatus.left;
        // originalProbeNum = coverageStatus.right;
        // Pair<Integer, Integer> upgradedCoverageStatus = Utilities
        // .getCoverageStatus(upCoverage);
        // upgradedCoveredBranches = upgradedCoverageStatus.left;
        // upgradedProbeNum = upgradedCoverageStatus.right;
        // }

        Long timeElapsed = TimeUnit.SECONDS.convert(
                System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
        if (timeElapsed - lastTimePoint > timeInterval || lastTimePoint == 0) {
            // Insert a record (time: coverage)
            originalCoverageAlongTime.add(
                    new Pair(timeElapsed, originalCoveredBranches));
            upgradedCoverageAlongTime.add(
                    new Pair(timeElapsed, upgradedCoveredBranches));
            lastTimePoint = timeElapsed;
        }
        testID++;
        System.out.println();
    }

    private void updateStatus(CommandSequence commandSequence,
            CommandSequence validationCommandSequence,
            ExecutionDataStore curCoverage, ExecutionDataStore upCoverage,
            boolean hasNewOrder, ExecutionDataStore seedOriginalCoverage,
            ExecutionDataStore seedUpgradeCoverage,
            CommandSequence seedCommandSequence,
            List<Pair<CommandSequence, CommandSequence>> roundCorpus,
            List<ExecutionDataStore> roundCoverage,
            List<Integer> seedIdxList, String seedStr,
            FeedBack testFeedBack) {
        // Check new bits, update covered branches, add record (time, coverage)
        // pair
        if (hasNewOrder) {
            roundCorpus.add(
                    new Pair<>(commandSequence, validationCommandSequence));
            roundCoverage.add(testFeedBack.originalCodeCoverage);
            if (Utilities.hasNewBits(curCoverage,
                    testFeedBack.originalCodeCoverage)) {
                curCoverage.merge(testFeedBack.originalCodeCoverage);
            }
            seedIdxList.add(seedID);
            saveSeed(commandSequence, validationCommandSequence);
            // upCoverage.merge(testFeedBack.upgradedCodeCoverage);

            // Update the coveredBranches to the newest value
            Pair<Integer, Integer> coverageStatus = Utilities
                    .getCoverageStatus(curCoverage);
            originalCoveredBranches = coverageStatus.left;
            originalProbeNum = coverageStatus.right;
        } else if (Utilities.hasNewBits(curCoverage,
                testFeedBack.originalCodeCoverage)) {
            System.out.println("[HKLOG] Get new bits!");
            // Must merge first, since computeDelta could change its value
            curCoverage.merge(testFeedBack.originalCodeCoverage);
            roundCorpus.add(
                    new Pair<>(commandSequence, validationCommandSequence));
            Utilities.computeDelta(curCoverage,
                    testFeedBack.originalCodeCoverage);
            roundCoverage.add(testFeedBack.originalCodeCoverage);
            seedIdxList.add(seedID);
            saveSeed(commandSequence, validationCommandSequence);
            // upCoverage.merge(testFeedBack.upgradedCodeCoverage);

            // Update the coveredBranches to the newest value
            Pair<Integer, Integer> coverageStatus = Utilities
                    .getCoverageStatus(curCoverage);
            originalCoveredBranches = coverageStatus.left;
            originalProbeNum = coverageStatus.right;
            // Pair<Integer, Integer> upgradedCoverageStatus = Utilities
            // .getCoverageStatus(upCoverage);
            // upgradedCoveredBranches = upgradedCoverageStatus.left;
            // upgradedProbeNum = upgradedCoverageStatus.right;
        } else {
            // no new order is found
            // update current corpus
            System.out.println("[HKLOG] No new bits!");

            if (Utilities.hasNewBits(seedOriginalCoverage,
                    testFeedBack.originalCodeCoverage)) {
                // compare and check whether we can find an identical one
                Utilities.computeDelta(seedOriginalCoverage,
                        testFeedBack.originalCodeCoverage);
                System.out.println("[HKLOG] NEWBITS COMPARE TO OLD");

                assert roundCorpus.size() == roundCoverage.size();
                assert roundCorpus.size() == seedIdxList.size();

                System.out.println("round corpus size = " + roundCorpus.size());
                System.out.println("roundCoverage size = " +
                        roundCoverage.size());
                System.out.println("seedIdxList size = " + seedIdxList);

                int i = 0;
                for (; i < roundCorpus.size(); i++) {
                    System.out.println("Comparing with " + seedIdxList.get(i));
                    if (Utilities.isEqualCoverage(
                            roundCoverage.get(i),
                            testFeedBack.originalCodeCoverage)) {
                        System.out.println("Found Identical Seed! Seed idx = " +
                                seedIdxList.get(i));
                        break;
                    }
                }
                if (i == roundCorpus.size()) {
                    // Not found, keep this seed
                    // corpus [s0, s1], s0: {b1, b2}, s1: {b2, b3}, s3: {b1, b3}
                    System.out.println("NOT FOUND!");
                    roundCorpus.add(
                            new Pair<>(commandSequence,
                                    validationCommandSequence));
                    roundCoverage.add(testFeedBack.originalCodeCoverage);
                    seedIdxList.add(seedID);
                    saveSeed(commandSequence, validationCommandSequence);
                } else {
                    // Compute the distance, replace if needed
                    String str1 = "";
                    for (String cmdStr : roundCorpus.get(i).left
                            .getCommandStringList()) {
                        str1 += cmdStr;
                    }
                    String str2 = "";
                    for (String cmdStr : commandSequence
                            .getCommandStringList()) {
                        str2 += cmdStr;
                    }

                    double dist1 = qGram.distance(seedStr, str1);
                    double dist2 = qGram.distance(seedStr, str2);

                    System.out.println("dist1 = " + dist1);
                    System.out.println("dist2 = " + dist2);

                    if (qGram.distance(seedStr, str1) > qGram.distance(seedStr,
                            str2)) {
                        roundCorpus.remove(i);
                        roundCorpus.add(i,
                                new Pair<>(commandSequence,
                                        validationCommandSequence));
                        appendSeedFile(commandSequence,
                                validationCommandSequence,
                                seedIdxList.get(i));
                    }
                }
            } else {
                System.out.println(
                        "Comparing to the seed sequence, no new bits found!");
                System.out.println("Original Sequence: ");
                for (String cmdStr : seedCommandSequence
                        .getCommandStringList()) {
                    System.out.println(cmdStr);
                }
            }
        }

        // Disable the usage of new code coverage temporally

        // else if (Utilities.hasNewBits(upCoverage,
        // testFeedBack.upgradedCodeCoverage)) {
        // saveSeed(commandSequence, validationCommandSequence);
        // queue.add(new Pair<>(commandSequence, validationCommandSequence));
        // curCoverage.merge(testFeedBack.originalCodeCoverage);
        // upCoverage.merge(testFeedBack.upgradedCodeCoverage);
        //
        // // Update the coveredBranches to the newest value
        // Pair<Integer, Integer> coverageStatus = Utilities
        // .getCoverageStatus(curCoverage);
        // originalCoveredBranches = coverageStatus.left;
        // originalProbeNum = coverageStatus.right;
        // Pair<Integer, Integer> upgradedCoverageStatus = Utilities
        // .getCoverageStatus(upCoverage);
        // upgradedCoveredBranches = upgradedCoverageStatus.left;
        // upgradedProbeNum = upgradedCoverageStatus.right;
        // }

        Long timeElapsed = TimeUnit.SECONDS.convert(
                System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
        if (timeElapsed - lastTimePoint > timeInterval || lastTimePoint == 0) {
            // Insert a record (time: coverage)
            originalCoverageAlongTime.add(
                    new Pair(timeElapsed, originalCoveredBranches));
            upgradedCoverageAlongTime.add(
                    new Pair(timeElapsed, upgradedCoveredBranches));
            lastTimePoint = timeElapsed;
        }
        testID++;
        System.out.println();
    }

    public static void saveSeed(CommandSequence commandSequence,
            CommandSequence validationCommandSequence) {
        // Serialize the seed of the queue in to disk
        if (Config.getConf().corpusDir == null) {
            logger.info("corpusDir is not provided!");
        } else {
            File corpusDir = new File(Config.getConf().corpusDir);
            if (!corpusDir.exists()) {
                corpusDir.mkdirs();
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Seed Id = " + seedID + "\n");
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

    public static void appendSeedFile(CommandSequence commandSequence,
            CommandSequence validationCommandSequence,
            int seedIdx) {
        // Serialize the seed of the queue in to disk
        StringBuilder sb = new StringBuilder();
        sb.append("\n APPEND SEED \n");
        sb.append("Seed Id = " + seedIdx + "\n");
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

        Path seedFilePath = Paths.get(Config.getConf().corpusDir,
                "seed_" + seedIdx + ".txt");

        Utilities.write2TXT(seedFilePath.toFile(), sb.toString(), true);
    }

    // TODO: Need a function to re-write the seed so that we can modify the
    // existing seeds

    public void printInfo(int queueSize, int crashID, int testID) {
        Long timeElapsed = TimeUnit.MILLISECONDS.convert(
                System.nanoTime() - startTime, TimeUnit.NANOSECONDS);

        System.out.println(
                "\n\n------------------- Executing one fuzzing test -------------------");
        System.out.println(
                "[Fuzz Status]\n"
                        +
                        "================================================================="
                        + "====================================================\n"
                        + "|"
                        + "Queue Size = " + queueSize + "|"
                        + "Round = " + round + "|"
                        + "Crash Found = " + crashID + "|"
                        + "Current Test ID = " + testID + "|"
                        + "Covered Branches Num = " + originalCoveredBranches
                        + "|"
                        + "Total Branch Num = " + originalProbeNum + "|"
                        + "Time Elapsed = " + timeElapsed / 1000. + "s"
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

    public static boolean checkIfHasNewOrder_Two_Cmd(List<String> cmdStrList,
            String cmd1,
            String cmd2,
            Map<Set<Integer>, Set<Set<Integer>>> orders) {
        // Care about the order between two commands
        boolean hasNewOrder;
        Set<Integer> cmd1Pos = new HashSet<>();
        Set<Integer> cmd2Pos = new HashSet<>();
        for (int i = 0; i < cmdStrList.size(); i++) {
            String cmdStr = cmdStrList.get(i);
            if (cmdStr.contains(cmd1)) {
                cmd1Pos.add(i);
            } else if (cmdStr.contains(cmd2)) {
                cmd2Pos.add(i);
            }
        }
        if (orders.containsKey(cmd1Pos)) {
            if (orders.get(cmd1Pos).contains(cmd2Pos)) {
                hasNewOrder = false;
            } else {
                orders.get(cmd1Pos).add(cmd2Pos);
                hasNewOrder = true;
            }
        } else {
            orders.put(cmd1Pos, new HashSet<>());
            orders.get(cmd1Pos).add(cmd2Pos);
            hasNewOrder = true;
        }
        return hasNewOrder;
    }

    public static boolean checkIfHasNewOrder_OneCmd(List<String> cmdStrList,
            String cmd1,
            Set<Set<Integer>> orders) {
        // Care about only the position of target command
        boolean hasNewOrder;
        Set<Integer> cmd1Pos = new HashSet<>();
        for (int i = 0; i < cmdStrList.size(); i++) {
            String cmdStr = cmdStrList.get(i);
            if (cmdStr.contains(cmd1)) {
                cmd1Pos.add(i);
            }
        }
        if (orders.contains(cmd1Pos)) {
            hasNewOrder = false;
        } else {
            orders.add(cmd1Pos);
            hasNewOrder = true;
        }
        return hasNewOrder;
    }

}
