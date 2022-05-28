package org.zlab.upfuzz.fuzzingengine;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.Queue;

import org.apache.commons.lang3.SerializationUtils;
import org.jacoco.core.data.ExecutionDataStore;
import org.zlab.upfuzz.CommandPool;
import org.zlab.upfuzz.CommandSequence;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.cassandra.CassandraCommandPool;
import org.zlab.upfuzz.cassandra.CassandraExecutor;
import org.zlab.upfuzz.cassandra.CassandraState;
import org.zlab.upfuzz.fuzzingengine.executor.Executor;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.hdfs.HdfsCommandPool;
import org.zlab.upfuzz.hdfs.HdfsExecutor;
import org.zlab.upfuzz.utils.Pair;
import org.zlab.upfuzz.utils.Utilities;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Fuzzer {
    /**
     * start from one seed, fuzz it for a certain times.
     * Also check the coverage here?
     * @param commandSequence
     * @param fromCorpus Whether the given seq is from the corpus. If yes, only run the
     *                   mutated seed. If no, this seed also need run.
     * @return
     */
    public static final int TEST_NUM = 2000;

    /**
     * If a seed cannot be correctly mutated for more than five times,
     * Discard this test case.
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
    public static long timeInterval = 600; // seconds, now set it as every 10
                                           // mins

    public static int seedID = 0;
    public static int round = 0;
    public CommandPool commandPool;
    public Executor executor;
    public Class<? extends State> stateClass;

    public Fuzzer() {
        if (Config.getConf().system.equals("cassandra")) {
            executor = new CassandraExecutor(null, null);
            commandPool = new CassandraCommandPool();
            stateClass = CassandraState.class;
        } else if (Config.getConf().system.equals("hdfs")) {
            executor = new HdfsExecutor(null, null);
            commandPool = new HdfsCommandPool();
            stateClass = HdfsState.class;
        }
    }

    public boolean fuzzOne(Random rand, CommandSequence commandSequence,
            CommandSequence validationCommandSequence,
            ExecutionDataStore curCoverage, ExecutionDataStore upCoverage,
            Queue<Pair<CommandSequence, CommandSequence>> queue,
            FuzzingClient fuzzingClient, boolean fromCorpus) {

        // Fuzz this command sequence for lots of times
        if (fromCorpus) {

            // Special Care about the order between commands
            // Choice 1: Only care about the locations of DROP
            // Choice 2: Care about the order between DROP and INSERT
            // The order of command can be assigned by USER

            String cmd1 = "INSERT";
            String cmd2 = "DROP";
            Map<Set<Integer>, Set<Set<Integer>>> ordersOf2Cmd = new HashMap<>();
            // Set<Set<Integer>> ordersOf1Cmd = new HashSet<>();
            boolean hasNewOrder;
            // Only run the mutated seeds
            for (int i = 0; i < TEST_NUM; i++) {
                printInfo(queue.size(), fuzzingClient.crashID, testID);

                CommandSequence mutatedCommandSequence = SerializationUtils
                        .clone(commandSequence);
                try {
                    int j = 0;
                    for (; j < MUTATE_RETRY_TIME; j++) {
                        // Learned from syzkaller
                        // 1/3 probability that the mutation could be stacked
                        // But if exceeds MUTATE_RETRY_TIME(10) stacked
                        // mutation, this sequence will be dropped
                        if (mutatedCommandSequence.mutate() == true
                                && Utilities.oneOf(rand, 3))
                            break;
                    }
                    if (j == MUTATE_RETRY_TIME) {
                        // Discard current seq since the mutation keeps failing
                        // or too much mutation is stacked together
                        continue;
                    }
                    hasNewOrder = checkIfHasNewOrder_Two_Cmd(
                            mutatedCommandSequence.getCommandStringList(), cmd1,
                            cmd2, ordersOf2Cmd);
                    System.out.println("Mutated Command Sequence:");
                    for (String cmdStr : mutatedCommandSequence
                            .getCommandStringList()) {
                        System.out.println(cmdStr);
                    }
                    System.out.println();
                    // Update the validationCommandSequence...
                    validationCommandSequence = mutatedCommandSequence
                            .generateRelatedReadSequence();
                    System.out.println("Read Command Sequence:");
                    for (String readCmdStr : validationCommandSequence
                            .getCommandStringList()) {
                        System.out.println(readCmdStr);
                    }
                    System.out.println();
                    if (validationCommandSequence.commands.isEmpty() == true) {
                        validationCommandSequence = Executor
                                .prepareValidationCommandSequence(commandPool,
                                        mutatedCommandSequence.state);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    i--;
                    continue;
                }
                FeedBack fb = null;
                try {
                    fb = fuzzingClient.start(mutatedCommandSequence,
                            validationCommandSequence, testID);
                } catch (Exception e) {
                    e.printStackTrace();
                    Utilities.clearCassandraDataDir();
                    i--;
                    continue;
                }
                // TODO: Add compare function in Jacoco
                updateStatus(mutatedCommandSequence, validationCommandSequence,
                        curCoverage, upCoverage, queue, hasNewOrder, fb);
            }

            round++;

        } else {
            printInfo(queue.size(), fuzzingClient.crashID, testID);
            // Only run the current seed, no mutation
            FeedBack fb = null;
            try {
                fb = fuzzingClient.start(commandSequence,
                        validationCommandSequence, testID);
            } catch (Exception e) {
                Utilities.clearCassandraDataDir();
            }
            updateStatus(commandSequence, validationCommandSequence,
                    curCoverage, upCoverage, queue, false, fb);
        }
        return true;
    }

    private static void updateStatus(CommandSequence commandSequence,
            CommandSequence validationCommandSequence,
            ExecutionDataStore curCoverage, ExecutionDataStore upCoverage,
            Queue<Pair<CommandSequence, CommandSequence>> queue,
            boolean hasNewOrder, FeedBack testFeedBack) {
        // Check new bits, update covered branches, add record (time, coverage)
        // pair
        if (hasNewOrder || Utilities.hasNewBits(curCoverage,
                testFeedBack.originalCodeCoverage)) {
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
            // Pair<Integer, Integer> upgradedCoverageStatus = Utilities
            // .getCoverageStatus(upCoverage);
            // upgradedCoveredBranches = upgradedCoverageStatus.left;
            // upgradedProbeNum = upgradedCoverageStatus.right;

        } else if (Utilities.hasNewBits(upCoverage,
                testFeedBack.upgradedCodeCoverage)) {
            saveSeed(commandSequence, validationCommandSequence);
            queue.add(new Pair<>(commandSequence, validationCommandSequence));
            curCoverage.merge(testFeedBack.originalCodeCoverage);
            upCoverage.merge(testFeedBack.upgradedCodeCoverage);

            // Update the coveredBranches to the newest value
            Pair<Integer, Integer> coverageStatus = Utilities
                    .getCoverageStatus(curCoverage);
            originalCoveredBranches = coverageStatus.left;
            originalProbeNum = coverageStatus.right;
            Pair<Integer, Integer> upgradedCoverageStatus = Utilities
                    .getCoverageStatus(upCoverage);
            upgradedCoveredBranches = upgradedCoverageStatus.left;
            upgradedProbeNum = upgradedCoverageStatus.right;
        }

        Long timeElapsed = TimeUnit.SECONDS.convert(
                System.nanoTime() - Main.startTime, TimeUnit.NANOSECONDS);
        if (timeElapsed - lastTimePoint > timeInterval || lastTimePoint == 0) {
            // Insert a record (time: coverage)
            originalCoverageAlongTime
                    .add(new Pair(timeElapsed, originalCoveredBranches));
            upgradedCoverageAlongTime
                    .add(new Pair(timeElapsed, upgradedCoveredBranches));
            lastTimePoint = timeElapsed;
        }
        testID++;
        System.out.println();
    }

    public static void saveSeed(CommandSequence commandSequence,
            CommandSequence validationCommandSequence) {
        // Serialize the seed of the queue in to disk
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

        Utilities.write2TXT(crashReportPath.toFile(), sb.toString());
        seedID++;
    }

    public static void printInfo(int queueSize, int crashID, int testID) {
        Long timeElapsed = TimeUnit.SECONDS.convert(
                System.nanoTime() - Main.startTime, TimeUnit.NANOSECONDS);

        System.out.println(
                "\n\n------------------- Executing one fuzzing test -------------------");
        System.out.println("[Fuzz Status]\n"
                + "================================================================="
                + "====================================================\n" + "|"
                + "Queue Size = " + queueSize + "|" + "Round = " + round + "|"
                + "Crash Found = " + crashID + "|" + "Current Test ID = "
                + testID + "|" + "Covered Branches Num = "
                + originalCoveredBranches + "|" + "Total Branch Num = "
                + originalProbeNum + "|" + "Time Elapsed = " + timeElapsed + "s"
                + "|" + "\n"
                + "-----------------------------------------------------------------"
                + "----------------------------------------------------");

        // Print the coverage status
        for (Pair<Integer, Integer> timeCoveragePair : originalCoverageAlongTime) {
            System.out.println("TIME: " + timeCoveragePair.left + "s"
                    + "\t\t Orginal Coverage: " + timeCoveragePair.right + "/"
                    + originalProbeNum + "\t\t percentage: "
                    + (float) timeCoveragePair.right / originalProbeNum + "%");
        }

        for (Pair<Integer, Integer> timeCoveragePair : originalCoverageAlongTime) {
            System.out.println("TIME: " + timeCoveragePair.left + "s"
                    + "\t\t Upgraded Coverage: " + timeCoveragePair.right + "/"
                    + upgradedProbeNum + "\t\t percentage: "
                    + (float) timeCoveragePair.right / upgradedProbeNum + "%");
        }
        System.out.println();
    }

    public static boolean checkIfHasNewOrder_Two_Cmd(List<String> cmdStrList,
            String cmd1, String cmd2,
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
            String cmd1, Set<Set<Integer>> orders) {
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
