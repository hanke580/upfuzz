package org.zlab.upfuzz.fuzzingengine.Server;

import java.nio.file.Paths;

import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.fuzzingengine.Packet.TestPacket;

public class FuzzingServer {

    // Seed Corpus (tuple(Seed, Info))

    public Corpus corpus;

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

    public TestPacket getOneTest() {
        return corpus.getOneTest();
    }

    enum FuzzingServerActions {
        start;
    }
}
