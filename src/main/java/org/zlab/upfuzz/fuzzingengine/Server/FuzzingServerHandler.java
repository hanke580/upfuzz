package org.zlab.upfuzz.fuzzingengine.Server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import com.google.gson.Gson;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.fuzzingengine.Packet.Packet.PacketType;
import org.zlab.upfuzz.fuzzingengine.Packet.FeedbackPacket;
import org.zlab.upfuzz.fuzzingengine.Packet.RegisterPacket;
import org.zlab.upfuzz.fuzzingengine.Packet.StackedFeedbackPacket;
import org.zlab.upfuzz.fuzzingengine.Packet.StackedTestPacket;

public class FuzzingServerHandler implements Runnable {
    static Logger logger = LogManager.getLogger(FuzzingServerHandler.class);

    private FuzzingServer fuzzingServer;
    private Socket socket;
    InputStream in;
    OutputStream out;

    FuzzingServerHandler(FuzzingServer fuzzingServer, Socket socket) {
        this.fuzzingServer = fuzzingServer;
        this.socket = socket;
        try {
            in = socket.getInputStream();
            out = socket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            readRegisterPacket();
            while (true) {
                StackedTestPacket stackedTestPacket = fuzzingServer
                        .getOneTest();
                stackedTestPacket.write(out);
                readFeedbackPacket();
            }
            // TestPacket tp = fuzzingServer.getOneTest();
            // Gson gs = new Gson();
            // socket.getOutputStream().write(gs.toJson(tp).getBytes());
        } catch (IOException e) {
            logger.error(e);
        }
    }

    private void readFeedbackPacket() throws IOException {
        int intType = in.read();
        assert intType == PacketType.StackedFeedbackPacket.value;
        StackedFeedbackPacket stackedFeedbackPacket = StackedFeedbackPacket
                .read(in);
        fuzzingServer.updateStatus(stackedFeedbackPacket);
    }

    private void readRegisterPacket() throws IOException {
        int intType = in.read();
        assert intType == PacketType.RegisterPacket.value;
        RegisterPacket registerPacket = RegisterPacket.read(in);
        // byte[] bytes = new byte[65536];
        // int len = in.read(bytes);
        // RegisterPacket registerPacket = new Gson()
        // .fromJson(new String(bytes, 0, len), RegisterPacket.class);
        // logger.info("register fuzzingclient: " + registerPacket.clientId + "
        // "
        // + registerPacket.systemId);
    }

}
