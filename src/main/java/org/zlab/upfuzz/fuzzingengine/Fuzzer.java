package org.zlab.upfuzz.fuzzingengine;

import org.apache.commons.lang3.SerializationUtils;
import org.jacoco.core.data.ExecutionDataStore;
import org.zlab.upfuzz.CommandSequence;
import org.zlab.upfuzz.cassandra.CassandraExecutor;
import org.zlab.upfuzz.utils.Pair;
import org.zlab.upfuzz.utils.Utilities;

import java.lang.reflect.InvocationTargetException;
import java.util.Queue;

public class Fuzzer {
    /**
     * start from one seed, fuzz it for a certain times.
     * Also check the coverage here?
     * @param commandSequence
     * @param fromCorpus Whether the given seq is from the corpus. If yes, only run the
     *                   mutated seed. If no, this seed also need run.
     * @return
     */
    //    public static final int TEST_NUM = 20; // Change this according to the seed.
    public static final int TEST_NUM = 20;

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
                System.out.println("\n\n----------- Executing one fuzzing test -----------");
                System.out.println("[Fuzz Status]\n" +
                                   "Queue Size = " + queue.size() + "\n" +
                                   "Crash Found = " + fuzzingClient.crashID + "\n" +
                                   "Current Test ID = " + testID + "\n"
                        );

                CommandSequence mutatedCommandSequence = SerializationUtils.clone(commandSequence);
                try {
                    mutatedCommandSequence.mutate();
                    // Update the validationCommandSequence...
                    validationCommandSequence = CassandraExecutor.prepareValidationCommandSequence(mutatedCommandSequence.state);
                } catch (Exception e) {
                    i--;
                    continue;
                }
                ExecutionDataStore testSequenceCoverage = null;
                try {
                    testSequenceCoverage = fuzzingClient.start(mutatedCommandSequence, validationCommandSequence);
                } catch (Exception e) {
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
            System.out.println("\n\n----------- Executing one fuzzing test -----------");
            System.out.println("[Fuzz Status]\n" +
                "Queue Size = " + queue.size() + "\n" +
                "Crash Found = " + fuzzingClient.crashID + "\n" +
                "Current Test ID = " + testID + "\n"
            );
            // Only run the current seed, no mutation
            ExecutionDataStore testSequenceCoverage = null;
            try {
                testSequenceCoverage = fuzzingClient.start(commandSequence, validationCommandSequence);
            } catch (Exception e) {
                Utilities.clearCassandraDataDir();
            }
            if (Utilities.hasNewBits(curCoverage, testSequenceCoverage)) {
                queue.add(new Pair<>(commandSequence, validationCommandSequence));
            }
            testID++;
            System.out.println();
        }
        return true;
    }
}
