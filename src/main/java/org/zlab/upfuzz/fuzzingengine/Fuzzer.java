package org.zlab.upfuzz.fuzzingengine;

import org.apache.commons.lang3.SerializationUtils;
import org.jacoco.core.data.ExecutionDataStore;
import org.zlab.upfuzz.CommandSequence;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
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
    public static int coveredBranches = 0;
    public static int probeNum = 0;

    public static List<Pair<Integer, Integer>> coverageAlongTime = new ArrayList<>(); // time: Coverage
    public static long lastTimePoint = 0;
    public static long timeInterval = 600; // seconds, now set it as every 10 mins

    public static int seedID = 0;
    public static int round = 0;

    public static boolean fuzzOne(Random rand,
                                  CommandSequence commandSequence,
                                  CommandSequence validationCommandSequence,
                                  ExecutionDataStore curCoverage,
                                  Queue<Pair<CommandSequence, CommandSequence>> queue,
                                  FuzzingClient fuzzingClient,
                                  boolean fromCorpus) {
        // Fuzz this command sequence for lots of times
        if (fromCorpus) {
            // Only run the mutated seeds
            for (int i = 0; i < TEST_NUM; i++) {
                printInfo(queue.size(), fuzzingClient.crashID, testID);

                CommandSequence mutatedCommandSequence = SerializationUtils.clone(commandSequence);
                try {
                    int j = 0;

                    for (; j < MUTATE_RETRY_TIME; j++) {
                        // Learned from syzkaller
                        // 1/3 probability that the mutation could be stacked
                        // But if exceeds MUTATE_RETRY_TIME(10) stacked mutation, this sequence will be dropped
                        if (mutatedCommandSequence.mutate() == true && Utilities.oneOf(rand, 3)) break;
                    }
                    if (j == MUTATE_RETRY_TIME) {
                        continue; // Discard current seq since the mutation keeps failing
                    }
                    System.out.println("Mutated Command Sequence:");
                    for (String cmdStr : mutatedCommandSequence.getCommandStringList()) {
                        System.out.println(cmdStr);
                    }
                    System.out.println();
                    // Update the validationCommandSequence...
                    validationCommandSequence = mutatedCommandSequence.generateRelatedReadSequence();
                    System.out.println("Read Command Sequence:");
                    for (String readCmdStr : validationCommandSequence.getCommandStringList()) {
                        System.out.println(readCmdStr);
                    }
                    System.out.println();
                    if (validationCommandSequence.commands.isEmpty() == true) {
                        validationCommandSequence = CassandraExecutor.prepareValidationCommandSequence(mutatedCommandSequence.state);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    i--;
                    continue;
                }
                ExecutionDataStore testSequenceCoverage = null;
                try {
                    testSequenceCoverage = fuzzingClient.start(mutatedCommandSequence, validationCommandSequence, testID);
                } catch (Exception e) {
                    e.printStackTrace();
                    Utilities.clearCassandraDataDir();
                    i--;
                    continue;
                }
                // TODO: Add compare function in Jacoco
                updateStatus(mutatedCommandSequence, validationCommandSequence, curCoverage, queue, testSequenceCoverage);
            }

            round++;

        } else {
            printInfo(queue.size(), fuzzingClient.crashID, testID);
            // Only run the current seed, no mutation
            ExecutionDataStore testSequenceCoverage = null;
            try {
                testSequenceCoverage = fuzzingClient.start(commandSequence, validationCommandSequence, testID);
            } catch (Exception e) {
                Utilities.clearCassandraDataDir();
            }
            updateStatus(commandSequence, validationCommandSequence, curCoverage, queue, testSequenceCoverage);
        }
        return true;
    }

    private static void updateStatus(CommandSequence commandSequence, CommandSequence validationCommandSequence, ExecutionDataStore curCoverage, Queue<Pair<CommandSequence, CommandSequence>> queue, ExecutionDataStore testSequenceCoverage) {
        // Check new bits, update covered branches, add record (time, coverage) pair
        if (Utilities.hasNewBits(curCoverage, testSequenceCoverage)) {
            saveSeed(commandSequence, validationCommandSequence);
            queue.add(new Pair<>(commandSequence, validationCommandSequence));
            curCoverage.merge(testSequenceCoverage);

            // Update the coveredBranches to the newest value
            Pair<Integer, Integer> coverageStatus = Utilities.getCoverageStatus(curCoverage);
            coveredBranches = coverageStatus.left;
            probeNum = coverageStatus.right;
        }

        Long timeElapsed = TimeUnit.SECONDS.convert(System.nanoTime() - Main.startTime, TimeUnit.NANOSECONDS);
        if (timeElapsed - lastTimePoint > timeInterval || lastTimePoint == 0) {
            // Insert a record (time: coverage)
            coverageAlongTime.add(new Pair(timeElapsed, coveredBranches));
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
        for (String commandStr : validationCommandSequence.getCommandStringList()) {
            sb.append(commandStr);
            sb.append("\n");
        }
        Path crashReportPath = Paths.get(Config.getConf().corpusDir,
                "seed_" + seedID + ".txt");


        Utilities.write2TXT(crashReportPath.toFile(), sb.toString());
        seedID++;
    }

    public static void printInfo(int queueSize, int crashID, int testID) {
        Long timeElapsed = TimeUnit.SECONDS.convert(System.nanoTime() - Main.startTime, TimeUnit.NANOSECONDS);

        System.out.println("\n\n------------------- Executing one fuzzing test -------------------");
        System.out.println("[Fuzz Status]\n" +
                           "=================================================================" +
                           "====================================================\n" +
                           "|" + "Queue Size = " + queueSize +
                           "|" + "Round = " + round +
                           "|" + "Crash Found = " + crashID +
                           "|" + "Current Test ID = " + testID +
                           "|" + "Covered Branches Num = " + coveredBranches +
                           "|" + "Total Branch Num = " + probeNum +
                           "|" + "Time Elapsed = " + timeElapsed + "s" +
                           "|" + "\n" +
                           "-----------------------------------------------------------------" +
                           "----------------------------------------------------");

        // Print the coverage status
        for (Pair<Integer, Integer> timeCoveragePair : coverageAlongTime) {
            System.out.println("TIME: " + timeCoveragePair.left + "s" +
                    "\t\t Coverage: " + timeCoveragePair.right + "/" + probeNum +
                    "\t\t percentage: " + (float) timeCoveragePair.right/probeNum + "%");
        }

        System.out.println();
    }
}
