package org.zlab.upfuzz.fuzzingengine.Server;

import java.nio.file.Paths;
import java.util.Queue;

import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.fuzzingengine.Packet.TestPacket;

public class FuzzingServer {

    // Seed Corpus (tuple(Seed, Info))

    // Q1 seeds to be tested in a client
    // Q2 seeds sent to a client and waiting for a feedback

    Queue<CorpusEntry> q1;

    public Corpus corpus;

    private int mutationEpoch = 2000;

    public FuzzingServer() {
    }

    private void init() {
        if (Config.getConf().initSeedDir != null) {
            corpus.initCorpus(Paths.get(Config.getConf().initSeedDir));
        }
    }

    public void start() {
        init();
        new Thread(new FuzzingServerSocket(this)).start();
        // new Thread(new FuzzingServerDispatcher(this)).start();
    }

    public synchronized CorpusEntry getOneTest() {
        if (q1.isEmpty()) {
            CorpusEntry seed = corpus.getSeed();
            for (int i = 0; i < mutationEpoch; ++i) {
                addMutation(seed);
            }
        }
        return q1.peek();
    }

    private void addMutation(CorpusEntry seed) {
        q1.add(seed.mutate());
    }

    enum FuzzingServerActions {
        start;
    }
}
