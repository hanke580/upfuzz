package org.zlab.upfuzz.fuzzingengine;

import info.debatty.java.stringsimilarity.QGram;
import org.apache.commons.lang3.SerializationUtils;
import org.jacoco.core.data.ExecutionDataStore;
import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.CommandSequence;
import org.zlab.upfuzz.cassandra.CassandraCommands;
import org.zlab.upfuzz.cassandra.CassandraExecutor;
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

    public static QGram qGram = new QGram();

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

    public static boolean fuzzOne(Random rand, CommandSequence commandSequence,
            CommandSequence validationCommandSequence,
            ExecutionDataStore curCoverage, ExecutionDataStore upCoverage,
            Queue<Pair<CommandSequence, CommandSequence>> queue,
            FuzzingClient fuzzingClient, boolean fromCorpus) {
        // Fuzz this command sequence for lots of times
        if (fromCorpus) {

            // [1] Maintain a list storing all new seeds
            // [2] compare with latest coverage, if a new coverage is found,
            // directly add it.
            // [3] If not, compare it with the original seed, if a new coverage
            // is found, start finding
            // identical seeds process.
            // and if there is an identical seed, compute the
            // edit distance, do the replacement if needed.
            // Scan [Can be optimized by binary search]
            // If none of them is identical, we can keep it or throw it.
            // (temporally throw it)
            // Cannot use has_new_bits, since it could be seed0 includes seed1,
            // we want the identical relationship
            // There need 2 funcs, (1) compare (2) compute the edit distance

            // Special Care about the order between commands
            // Choice 1: Only care about the locations of DROP
            // Choice 2: Care about the order between DROP and INSERT
            // The order of command can be assigned by USER

            // Care about all the order between commands

            // String cmd1 = "INSERT";
            // String cmd2 = "DROP";
            // Map<Set<Integer>, Set<Set<Integer>>> ordersOf2Cmd = new
            // HashMap<>();
            // Set<Set<Integer>> ordersOf1Cmd = new HashSet<>();
            // Set<String> orderOfAllCmds = new HashSet<>();
            boolean hasNewOrder = false;
            ExecutionDataStore seedOriginalCoverage = null;
            ExecutionDataStore seedUpgradeCoverage = null;

            List<Pair<CommandSequence, CommandSequence>> roundCorpus = new LinkedList<>();
            List<ExecutionDataStore> roundCoverage = new LinkedList<>();
            List<Integer> seedIdxList = new LinkedList<>();
            String seedStr = "";
            for (String cmdStr : commandSequence.getCommandStringList()) {
                seedStr += cmdStr;
            }

            // Only run the mutated seeds
            for (int i = 0; i < TEST_NUM; i++) {
                printInfo(queue.size(), fuzzingClient.crashID, testID);

                CommandSequence mutatedCommandSequence = SerializationUtils
                        .clone(commandSequence);
                try {
                    int j = 0;

                    if (i != 0) { // Run the original seed and keep its coverage
                        for (; j < MUTATE_RETRY_TIME; j++) {
                            // Learned from syzkaller
                            // 1/3 probability that the mutation could be
                            // stacked
                            // But if exceeds MUTATE_RETRY_TIME(10) stacked
                            // mutation, this sequence will be dropped
                            if (mutatedCommandSequence.mutate() == true
                                    && Utilities.oneOf(rand, 3))
                                break;
                        }
                        if (j == MUTATE_RETRY_TIME) {
                            // Discard current seq since the mutation keeps
                            // failing
                            // or too much mutation is stacked together
                            continue;
                        }
                    }
                    // hasNewOrder = checkIfHasNewOrder_AllCmds(
                    // mutatedCommandSequence.getCommandStringList(),
                    // orderOfAllCmds);
                    // hasNewOrder = checkIfHasNewOrder_Two_Cmd(
                    // mutatedCommandSequence.getCommandStringList(), cmd1,
                    // cmd2, ordersOf2Cmd);
                    // hasNewOrder =
                    // checkIfHasNewOrder_OneCmd(mutatedCommandSequence
                    // .getCommandStringList(), cmd1, ordersOf1Cmd);
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
                        validationCommandSequence = CassandraExecutor
                                .prepareValidationCommandSequence(
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
                if (i == 0) {
                    seedOriginalCoverage = fb.originalCodeCoverage;
                    seedUpgradeCoverage = fb.upgradedCodeCoverage;
                }
                updateStatus(mutatedCommandSequence, validationCommandSequence,
                        curCoverage, upCoverage, hasNewOrder,
                        seedOriginalCoverage, seedUpgradeCoverage, roundCorpus,
                        roundCoverage, seedIdxList, seedStr, fb);
            }

            // TODO: Add roundCorpus to corpus

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

    private static void updateStatus(CommandSequence commandSequence,
            CommandSequence validationCommandSequence,
            ExecutionDataStore curCoverage, ExecutionDataStore upCoverage,
            boolean hasNewOrder, ExecutionDataStore seedOriginalCoverage,
            ExecutionDataStore seedUpgradeCoverage,
            List<Pair<CommandSequence, CommandSequence>> roundCorpus,
            List<ExecutionDataStore> roundCoverage, List<Integer> seedIdxList,
            String seedStr, FeedBack testFeedBack) {
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
            roundCorpus.add(
                    new Pair<>(commandSequence, validationCommandSequence));
            roundCoverage.add(testFeedBack.originalCodeCoverage);
            curCoverage.merge(testFeedBack.originalCodeCoverage);
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
            // update it with the current corpus
            if (Utilities.hasNewBits(curCoverage, seedOriginalCoverage)) {
                // compare and check whether we can find an identical one
                assert roundCorpus.size() == roundCorpus.size();
                assert roundCorpus.size() == seedIdxList.size();

                int i;
                for (i = 0; i < roundCorpus.size(); i++) {
                    if (Utilities.isEqualCoverage(roundCoverage.get(i),
                            testFeedBack.originalCodeCoverage)) {
                        break;
                    }
                }
                if (i == roundCorpus.size()) {
                    // Not found, keep this seed!
                    // corpus [s0, s1], s0: {b1, b2}, s1: {b2, b3}, s3: {b1, b3}
                    roundCorpus.add(new Pair<>(commandSequence,
                            validationCommandSequence));
                    roundCoverage.add(testFeedBack.originalCodeCoverage);
                } else {
                    // Found the identical one!
                    // Compute the distance, replace if needed

                    System.out.println("Found Identical Seed! Seed idx = "
                            + seedIdxList.get(i));

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
                    if (qGram.distance(seedStr, str1) > qGram.distance(seedStr,
                            str2)) {
                        // The new command sequence has the smaller distance
                        // Replacement!
                        // Only need to update the round Corpus, since coverage
                        // is the same
                        roundCorpus.remove(i);
                        roundCorpus.add(i, new Pair<>(commandSequence,
                                validationCommandSequence));

                    }
                    // Rewrite the seed file, (Use append for checking!)
                    // File name: "seed" + seedIdxList.get(i)
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

        Utilities.write2TXT(crashReportPath.toFile(), sb.toString(), false);
        seedID++;
    }

    public static void appendSeedFile(CommandSequence commandSequence,
            CommandSequence validationCommandSequence, int seedIdx) {
        // Serialize the seed of the queue in to disk
        StringBuilder sb = new StringBuilder();
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

    public static boolean checkIfHasNewOrder_AllCmds(List<String> cmdStrList,
            Set<String> orderSet) {
        // Care about only the position of target command
        String order = getStringOrder(cmdStrList);
        if (orderSet.contains(order)) {
            return false;
        } else {
            orderSet.add(order);
            return true;
        }
    }

    // Get the order of all commands, format like 1-2-3-3-4 <===> CCIID
    public static String getStringOrder(List<String> cmdStrList) {
        StringBuilder sb = new StringBuilder();
        for (String cmdStr : cmdStrList) {
            for (int i = 0; i < CassandraCommands.commandNameList.size(); i++) {
                if (cmdStr.contains(CassandraCommands.commandNameList.get(i))) {
                    if (sb.toString().length() == 0) {
                        sb.append(i);
                    } else {
                        sb.append("-" + i);
                    }
                    break;
                }
                // If nothing found, temporally do not care about this command
            }
        }
        return sb.toString();
    }
}
