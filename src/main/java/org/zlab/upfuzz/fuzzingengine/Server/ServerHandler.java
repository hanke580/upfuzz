package org.zlab.upfuzz.fuzzingengine.Server;

import java.io.IOException;
import java.net.Socket;

import com.google.gson.Gson;

import org.zlab.upfuzz.fuzzingengine.TestPacket;

public class ServerHandler implements Runnable {

    private FuzzingServer fuzzingServer;
    private Socket socket;

    ServerHandler(FuzzingServer fuzzingServer, Socket socket) {
        this.fuzzingServer = fuzzingServer;
        this.socket = socket;
    }

    @Override
    public void run() {
        TestPacket tp = fuzzingServer.getOneTest();
        Gson gs = new Gson();
        try {
            socket.getOutputStream().write(gs.toJson(tp).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
