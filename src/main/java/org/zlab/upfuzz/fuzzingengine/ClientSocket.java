package org.zlab.upfuzz.fuzzingengine;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;

import org.jacoco.core.data.ExecutionDataWriter;

public class ClientSocket extends Thread {
    final FuzzingClient client;
    final ServerSocket server;
    final Config conf;
    final ExecutionDataWriter fileWriter;

    ClientSocket(FuzzingClient client) throws UnknownHostException, IOException {
        this.client = client;
        this.conf = client.conf;
        this.server = new ServerSocket(conf.clientPort, 0, InetAddress.getByName(conf.clientHost));
        this.fileWriter = new ExecutionDataWriter(new FileOutputStream("./zlab-jacoco.exec"));
    }

    @Override
    public void run() {
        ClientHandler handler;
        while (true) {
            try {
                handler = new ClientHandler(client, server.accept(), fileWriter);
                new Thread(handler).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
