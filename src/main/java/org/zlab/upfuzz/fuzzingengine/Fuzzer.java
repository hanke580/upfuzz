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

    ExecutionDataStore curCoverage = new ExecutionDataStore();
    FuzzingClient fuzzingClient = new FuzzingClient();
    Queue<Pair<CommandSequence, CommandSequence>> queue = new LinkedList<>();
    Executor executor;
    Class<? extends State> stateClass;
    CommandPool commandPool;

    public void init() {

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    Thread.sleep(200);
                    System.out.println(
                            "Fuzzing process end, have a good day ...");
                    //some cleaning up code...

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    e.printStackTrace();
                }
            }
        });

        if (Config.getConf().initSeedDir != null) {
            System.out.println("seed path = " + Config.getConf().initSeedDir);
            Path initSeedDirPath = Paths.get(Config.getConf().initSeedDir);
            File initSeedDir = initSeedDirPath.toFile();
            assert initSeedDir.isDirectory() == true;
            for (File seedFile : initSeedDir.listFiles()) {
                if (!seedFile.isDirectory()) {
                    // Deserialize current file, and add it into the queue.
                    // TODO: Execute them before adding them to the queue.
                    // Make sure all the seed in the queue must have been executed.
                    Pair<CommandSequence, CommandSequence> commandSequencePair = Utilities
                            .deserializeCommandSequence(seedFile.toPath());
                    if (commandSequencePair != null)
                        queue.add(commandSequencePair);
                }
            }
        }

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

    public void start() {
        init();

        while (true) {
            fuzzOne();
        }
    }

    public boolean fuzzOne() {

        Boolean fromCorpus = null;
        CommandSequence commandSequence = null,
                validationCommandSequence = null;
        Pair<CommandSequence, CommandSequence> commandSequencePair = null;
        if (queue.isEmpty()) {
            commandSequencePair = Executor.prepareCommandSequence(commandPool,
                    stateClass);
            fromCorpus = true;
        } else {
            commandSequencePair = queue.poll();
            fromCorpus = false;
        }
        commandSequence = commandSequencePair.left;
        validationCommandSequence = commandSequencePair.right;

        // Fuzz this command sequence for lots of times
        if (fromCorpus) {
            // Only run the mutated seeds
            for (int i = 0; i < TEST_NUM; i++) {

                System.out.println(
                        "\n\n----------- Executing one from corpus fuzzing test -----------");
                System.out.println("[Fuzz Status]\n" + "Queue Size = "
                        + queue.size() + "\n" + "Crash Found = "
                        + fuzzingClient.crashID + "\n" + "Current Test ID = "
                        + testID + "\n");

                CommandSequence mutatedCommandSequence = SerializationUtils
                        .clone(commandSequence);
                try {
                    mutatedCommandSequence.mutate();

                    // Update the validationCommandSequence...
                    validationCommandSequence = Executor
                            .prepareValidationCommandSequence(commandPool,
                                    mutatedCommandSequence.state);

                } catch (InvocationTargetException | NoSuchMethodException
                        | InstantiationException | IllegalAccessException e) {
                    i--;
                    continue;
                }
                executor.reset(mutatedCommandSequence,
                        validationCommandSequence);
                ExecutionDataStore testSequenceCoverage = fuzzingClient
                        .start(executor);
                // TODO: Add compare function in Jacoco
                if (Utilities.hasNewBits(curCoverage, testSequenceCoverage)) {
                    queue.add(new Pair<>(mutatedCommandSequence,
                            validationCommandSequence));
                    curCoverage.merge(testSequenceCoverage);
                }
                testID++;
                System.out.println();
            }

        } else {
            System.out.println(
                    "\n\n----------- Executing one fuzzing test -----------");
            System.out
                    .println("[Fuzz Status]\n" + "Queue Size = " + queue.size()
                            + "\n" + "Crash Found = " + fuzzingClient.crashID
                            + "\n" + "Current Test ID = " + testID + "\n");
            // Only run the current seed, no mutation
            executor.reset(commandSequence, validationCommandSequence);
            ExecutionDataStore testSequenceCoverage = fuzzingClient
                    .start(executor);
            if (Utilities.hasNewBits(curCoverage, testSequenceCoverage)) {
                queue.add(
                        new Pair<>(commandSequence, validationCommandSequence));
            }
            testID++;
            System.out.println();
        }
        return true;
    }
}
