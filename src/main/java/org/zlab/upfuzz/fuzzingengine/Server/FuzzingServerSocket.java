package org.zlab.upfuzz.fuzzingengine.Server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.fuzzingengine.Config;

class FuzzingServerSocket implements Runnable {
    static Logger logger = LogManager.getLogger(FuzzingServerSocket.class);

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
            logger.info("fuzzing server start at "
                    + server.getLocalSocketAddress());
            while (true) {
                try {
                    FuzzingServerHandler handler = new FuzzingServerHandler(
                            fuzzingServer, server.accept());
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
