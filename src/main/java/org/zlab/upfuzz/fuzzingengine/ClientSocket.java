package org.zlab.upfuzz.fuzzingengine;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;

import org.jacoco.core.data.ExecutionDataWriter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ClientSocket extends Thread {
    static Logger logger = LogManager.getLogger(ClientSocket.class);

    final FuzzingClient client;
    final ServerSocket server;
    final ExecutionDataWriter fileWriter;

    ClientSocket(FuzzingClient client)
            throws UnknownHostException, IOException {
        this.client = client;
        this.server = new ServerSocket(Config.getConf().clientPort, 0,
                InetAddress.getByName(Config.getConf().clientHost));
        logger.info("Client socket Server start at: "
                + this.server.getLocalSocketAddress());
        this.fileWriter = new ExecutionDataWriter(
                new FileOutputStream("./zlab-jacoco.exec"));
    }

    @Override
    public void run() {
        ClientHandler handler;
        while (true) {
            try {
                handler = new ClientHandler(client, server.accept(),
                        fileWriter);
                new Thread(handler).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
