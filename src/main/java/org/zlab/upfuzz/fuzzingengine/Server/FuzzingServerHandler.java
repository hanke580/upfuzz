package org.zlab.upfuzz.fuzzingengine.Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.fuzzingengine.Packet.Packet.PacketType;
import org.zlab.upfuzz.fuzzingengine.Packet.RegisterPacket;
import org.zlab.upfuzz.fuzzingengine.Packet.StackedFeedbackPacket;
import org.zlab.upfuzz.fuzzingengine.Packet.StackedTestPacket;

public class FuzzingServerHandler implements Runnable {
    static Logger logger = LogManager.getLogger(FuzzingServerHandler.class);

    private FuzzingServer fuzzingServer;
    private Socket socket;
    DataInputStream in;
    DataOutputStream out;

    FuzzingServerHandler(FuzzingServer fuzzingServer, Socket socket) {
        this.fuzzingServer = fuzzingServer;
        this.socket = socket;
        try {
            socket.setSendBufferSize(4194304);
            socket.setReceiveBufferSize(4194304);
        } catch (SocketException e) {
            logger.error(e);
        }
        try {
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
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
        int intType = in.readInt();
        assert intType == PacketType.StackedFeedbackPacket.value;
        StackedFeedbackPacket stackedFeedbackPacket = StackedFeedbackPacket
                .read(in);
        fuzzingServer.updateStatus(stackedFeedbackPacket);
    }

    private void readRegisterPacket() throws IOException {
        int intType = in.readInt();
        assert intType == PacketType.RegisterPacket.value;
        RegisterPacket registerPacket = RegisterPacket.read(in);
    }

}
