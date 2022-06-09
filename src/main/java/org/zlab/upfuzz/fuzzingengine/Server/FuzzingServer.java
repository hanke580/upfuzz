package org.zlab.upfuzz.fuzzingengine.Server;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.zlab.upfuzz.CommandSequence;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.fuzzingengine.Fuzzer;
import org.zlab.upfuzz.fuzzingengine.TestPacket;
import org.zlab.upfuzz.utils.Pair;
import org.zlab.upfuzz.utils.Utilities;

public class FuzzingServer {

    // Seed Corpus (tuple(Seed, Info))

    private Config conf;

    public Corpus corpus;

    FuzzingServer(Config conf) {
        this.conf = conf;
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

    class FuzzingServerSocket implements Runnable {
        FuzzingServer fuzzingServer;

        FuzzingServerSocket(FuzzingServer fuzzingServer) {
            this.fuzzingServer = fuzzingServer;
        }

        @Override
        public void run() {
            try {
                final ServerSocket server = new ServerSocket(
                        Config.getConf().serverPort, 0,
                        InetAddress.getByName(Config.getConf().serverHost));
                System.out.println(
                        "server start at " + server.getLocalSocketAddress());
                while (true) {
                    try {
                        ServerHandler handler = new ServerHandler(fuzzingServer,
                                server.accept());
                        new Thread(handler).start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public TestPacket getOneTest() {
        return corpus.getOneTest();
    }

    enum FuzzingServerActions {
        start;
    }
}
