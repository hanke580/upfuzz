package org.zlab.upfuzz.fuzzingengine.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
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
            logger.info("fuzzing server start at " +
                    server.getLocalSocketAddress());
            while (true) {
                try {
                    Socket clientSocket = server.accept();
                    System.out.println("Local address: "
                            + clientSocket.getLocalSocketAddress());
                    System.out.println("Remote address: "
                            + clientSocket.getRemoteSocketAddress());
                    System.out.println(
                            "Local port: " + clientSocket.getLocalPort());
                    System.out.println(
                            "Remote port: " + clientSocket.getPort());
                    System.out.println("Socket information: " + clientSocket);
                    System.out.println("Creating handler");
                    FuzzingServerHandler handler = new FuzzingServerHandler(
                            fuzzingServer, clientSocket);
                    System.out.println("Created handler");
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
