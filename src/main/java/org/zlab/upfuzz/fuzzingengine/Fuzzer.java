package org.zlab.upfuzz.fuzzingengine;

import org.apache.commons.lang3.SerializationUtils;
import org.jacoco.core.data.ExecutionDataStore;
import org.zlab.upfuzz.CommandSequence;
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
    public static final int TEST_NUM = 1;

    public static boolean fuzzOne(Config conf,
                                  CommandSequence commandSequence,
                                  ExecutionDataStore curCoverage,
                                  Queue<CommandSequence> queue,
                                  FuzzingClient fuzzingClient,
                                  boolean fromCorpus) {
        // Fuzz this command sequence for lots of times
        if (fromCorpus) {
            // Only run the mutated seeds
            for (int i = 0; i < TEST_NUM; i++) {
                CommandSequence mutatedCommandSequence = SerializationUtils.clone(commandSequence);
                try{
                    mutatedCommandSequence.mutate();
                } catch (InvocationTargetException | NoSuchMethodException | InstantiationException | IllegalAccessException e) {
                    i--;
                    continue;
                }
                ExecutionDataStore testSequenceCoverage = fuzzingClient.start(mutatedCommandSequence);
                // TODO: Add compare function in Jacoco
                if (Utilities.hasNewBits(curCoverage, testSequenceCoverage)) {
                    queue.add(mutatedCommandSequence);
                    curCoverage.merge(testSequenceCoverage);
                }
                System.out.println("[HKLOG1] QUEUE SIZE = " + queue.size());
            }

        } else {
            // Only run the current seed, no mutation
            ExecutionDataStore testSequenceCoverage = fuzzingClient.start(commandSequence);
            if (Utilities.hasNewBits(curCoverage, testSequenceCoverage)) {
                queue.add(commandSequence);
            }
        }
        return true;
    }
}
