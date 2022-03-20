package org.zlab.upfuzz.fuzzingengine;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

public class FuzzingServer {

    private Config conf;

    FuzzingServer(Config conf) {
        this.conf = conf;
    }

    public void start() {
        try {
            final ServerSocket server = new ServerSocket(conf.serverPort, 0, InetAddress.getByName(conf.serverHost));
            System.out.println("server start at " + server.getLocalSocketAddress());
            // TODO start handler for client
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    enum FuzzingServerActions {
        start;
    }
}
