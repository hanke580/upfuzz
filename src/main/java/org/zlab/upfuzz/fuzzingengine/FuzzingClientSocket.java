package org.zlab.upfuzz.fuzzingengine;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.fuzzingengine.Packet.Packet;
import org.zlab.upfuzz.fuzzingengine.Packet.RegisterPacket;
import org.zlab.upfuzz.fuzzingengine.Packet.StackedFeedbackPacket;
import org.zlab.upfuzz.fuzzingengine.Packet.StackedTestPacket;

class FuzzingClientSocket implements Runnable {
    static Logger logger = LogManager.getLogger(FuzzingClientSocket.class);

    final FuzzingClient fuzzingClient;

    InputStream in;
    OutputStream out;
    Socket socket;

    FuzzingClientSocket(FuzzingClient fuzzingClient) {
        this.fuzzingClient = fuzzingClient;
        try {
            socket = new Socket(Config.getConf().serverHost,
                    Config.getConf().serverPort);
            try {
                socket.setSendBufferSize(4194304);
                socket.setReceiveBufferSize(4194304);
            } catch (SocketException e) {
                logger.error(e);
            }
            in = socket.getInputStream();
            out = socket.getOutputStream();
        } catch (IOException e) {
            logger.error("failed to connect fuzzing server "
                    + Config.getConf().serverHost + ":"
                    + Config.getConf().serverPort);
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        writeRegisterPacket();
        while (true) {
            int intType = -1;
            try {
                intType = in.read();
                System.out.println("intType = " + intType);
                Packet.PacketType type = Packet.PacketType.values()[intType];
                switch (type) {
                // Now there's only StackedFeedbackPacket, there'll be
                // rolling upgrade instructions when testing rolling
                // upgrade
                case StackedTestPacket: {
                    // Run executor
                    StackedTestPacket stackedTestPacket = StackedTestPacket
                            .read(in);
                    StackedFeedbackPacket stackedFeedbackPacket = fuzzingClient
                            .executeStackedTestPacket(stackedTestPacket);
                    stackedFeedbackPacket.write(out);
                    break;
                }
                }
                readHeader();
            } catch (IOException e) {
                System.out.println("intType = " + intType);
                e.printStackTrace();
            }
        }
    }

    private void writeRegisterPacket() {
        RegisterPacket registerPacket = new RegisterPacket(socket);
        registerPacket.write(out);
    }

    private void readHeader() {
    }
}
