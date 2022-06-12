package org.zlab.upfuzz.fuzzingengine.Server;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import org.apache.commons.lang3.SerializationUtils;
import org.zlab.upfuzz.CommandPool;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.cassandra.CassandraCommandPool;
import org.zlab.upfuzz.cassandra.CassandraExecutor;
import org.zlab.upfuzz.cassandra.CassandraState;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.fuzzingengine.Packet.StackedTestPacket;
import org.zlab.upfuzz.fuzzingengine.executor.Executor;
import org.zlab.upfuzz.hdfs.HdfsCommandPool;
import org.zlab.upfuzz.hdfs.HdfsExecutor;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.Utilities;

public class FuzzingServer {

    // Seed Corpus (tuple(Seed, Info))
    public Corpus corpus;
    private int testID;

    public CommandPool commandPool;
    public Executor executor;
    public Class<? extends State> stateClass;

    // Sent to client and waiting for Feedback
    private final Map<Integer, Seed> testID2Seed;
    // stackedTestPackets seeds to be tested in a client
    private final Queue<StackedTestPacket> stackedTestPackets;

    public FuzzingServer() {
        testID2Seed = new HashMap<>();
        stackedTestPackets = new LinkedList<>();
    }

    private void init() {
        if (Config.getConf().initSeedDir != null) {
            corpus.initCorpus(Paths.get(Config.getConf().initSeedDir));
        }

        if (Config.getConf().system.equals("cassandra")) {
            executor = new CassandraExecutor(null, null);
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
            executor = new HdfsExecutor(null, null);
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

    // TODO: Optimization for do mutations early
    public synchronized StackedTestPacket getOneTest() {
        if (!stackedTestPackets.isEmpty()) {
            return stackedTestPackets.poll();
        }
        fuzzOne();
        if (stackedTestPackets.isEmpty())
            System.exit(1);
        return stackedTestPackets.poll();
    }

    private void fuzzOne() {
        // All packets have been dispatched, now fuzz next seeds
        Seed seed = corpus.getSeed();
        StackedTestPacket stackedTestPacket = null;
        if (seed == null) {
            // corpus is empty, random generate one test packet and wait
            stackedTestPacket = new StackedTestPacket();
            for (int i = 0; i < Config.getConf().STACKED_TESTS_NUM; i++) {
                seed = Executor.generateSeed(commandPool, stateClass);
                if (seed != null) {
                    testID2Seed.put(testID, seed);
                    stackedTestPacket.addTest(seed, testID++);
                }
            }
        } else {
            // get a seed from corpus, now lets fuzz it for a epoch
            for (int i = 0; i < Config.getConf().mutationEpoch; i++) {
                if (i % Config.getConf().STACKED_TESTS_NUM == 0) {
                    if (i != 0) {
                        stackedTestPackets.add(stackedTestPacket);
                    }
                    stackedTestPacket = new StackedTestPacket();
                } else {
                    // Mutation
                    Seed mutateSeed = SerializationUtils.clone(seed);
                    if (mutateSeed.mutate()) {
                        testID2Seed.put(testID, mutateSeed);
                        stackedTestPacket.addTest(mutateSeed, testID++);
                    }
                }
            }
            if (stackedTestPacket != null && stackedTestPacket.size() != 0) {
                stackedTestPackets.add(stackedTestPacket);
            }
        }
    }

    enum FuzzingServerActions {
        start
    }
}
