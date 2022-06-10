package org.zlab.upfuzz.fuzzingengine;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.fuzzingengine.Packet.Packet;
import org.zlab.upfuzz.fuzzingengine.Packet.RegisterPacket;

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
            int intType;
            try {
                intType = in.read();
                Packet.PacketType type = Packet.PacketType.values()[intType];
                switch (type) {
                case RegisterPacket: {
                }
                }
                readHeader();
            } catch (IOException e) {
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
