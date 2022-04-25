package org.zlab.upfuzz.fuzzingengine;

import org.apache.commons.lang3.SerializationUtils;
import org.jacoco.core.data.ExecutionDataStore;
import org.zlab.upfuzz.CommandSequence;
import org.zlab.upfuzz.cassandra.CassandraExecutor;
import org.zlab.upfuzz.utils.Pair;
import org.zlab.upfuzz.utils.Utilities;

import java.util.Queue;
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
    public static final int TEST_NUM = 500;

    /**
     * If a seed cannot be correctly mutated for more than five times,
     * Discard this test case.
     */
    public static final int MUTATE_RETRY_TIME = 5;
    public static int testID = 0;

    public static boolean fuzzOne(CommandSequence commandSequence,
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
                        if (mutatedCommandSequence.mutate() == true) break;
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
                if (Utilities.hasNewBits(curCoverage, testSequenceCoverage)) {
                    queue.add(new Pair<>(mutatedCommandSequence, validationCommandSequence));
                    curCoverage.merge(testSequenceCoverage);
                }
                testID++;
                System.out.println();
            }

        } else {
            printInfo(queue.size(), fuzzingClient.crashID, testID);
            // Only run the current seed, no mutation
            ExecutionDataStore testSequenceCoverage = null;
            try {
                testSequenceCoverage = fuzzingClient.start(commandSequence, validationCommandSequence, testID);
            } catch (Exception e) {
                Utilities.clearCassandraDataDir();
            }
            if (Utilities.hasNewBits(curCoverage, testSequenceCoverage)) {
                queue.add(new Pair<>(commandSequence, validationCommandSequence));
                curCoverage.merge(testSequenceCoverage);
            }
            testID++;
            System.out.println();
        }
        return true;
    }

    public static void printInfo(int queueSize, int crashID, int testID) {
        Long timeElapsed = TimeUnit.SECONDS.convert(System.nanoTime() - Main.startTime, TimeUnit.NANOSECONDS);

        System.out.println("\n\n------------------- Executing one fuzzing test -------------------");
        System.out.println("[Fuzz Status]\n" +
                           "=========================================================================\n" +
                           "|" + "Queue Size = " + queueSize +
                           "|" + "Crash Found = " + crashID +
                           "|" + "Current Test ID = " + testID +
                           "|" + "Time Elapsed = " + timeElapsed + "s" +
                           "|" + "\n" +
                           "-------------------------------------------------------------------------");
        System.out.println();
    }
}
