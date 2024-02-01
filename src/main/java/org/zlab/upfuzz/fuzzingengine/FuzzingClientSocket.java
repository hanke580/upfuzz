package org.zlab.upfuzz.fuzzingengine;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.fuzzingengine.packet.*;

class FuzzingClientSocket implements Runnable {
    static Logger logger = LogManager.getLogger(FuzzingClientSocket.class);

    final FuzzingClient fuzzingClient;

    DataInputStream in;
    DataOutputStream out;
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
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            logger.error("failed to connect fuzzing server " +
                    Config.getConf().serverHost + ":" +
                    Config.getConf().serverPort);
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        writeRegisterPacket();
        while (true) {
            int intType = -1;
            try {
                intType = in.readInt();
                System.out.println("intType = " + intType);
                Packet.PacketType type = Packet.PacketType.values()[intType];
                Packet feedBackPacket = null;

                switch (type) {
                // Now there's only StackedFeedbackPacket, there'll be
                // rolling upgrade instructions when testing rolling
                // upgrade
                case StackedTestPacket: {
                    // Run executor
                    StackedTestPacket stackedTestPacket = StackedTestPacket
                            .read(in);
                    if (!Config.getConf().useVersionDelta) {
                        System.out.println("Regular stacked testing");
                        feedBackPacket = fuzzingClient
                                .executeStackedTestPacket(stackedTestPacket);
                    } else {
                        System.out.println("Version Delta testing");
                        feedBackPacket = fuzzingClient
                                .executeStackedTestPacketRegularVersionDelta(
                                        stackedTestPacket);
                    }
                    break;
                }
                case FullStopPacket: {
                    FullStopPacket fullStopPacket = FullStopPacket.read(in);
                    feedBackPacket = fuzzingClient
                            .executeFullStopPacket(fullStopPacket);
                    break;
                }
                case TestPlanPacket: {
                    TestPlanPacket testPlanPacket = TestPlanPacket.read(in);
                    feedBackPacket = fuzzingClient
                            .executeTestPlanPacket(testPlanPacket);
                    break;
                }
                case MixedTestPacket: {
                    MixedTestPacket mixedTestPacket = MixedTestPacket.read(in);
                    feedBackPacket = fuzzingClient
                            .executeMixedTestPacket(mixedTestPacket);
                    break;
                }
                }

                if (feedBackPacket == null) {
                    logger.debug(
                            "[HKLOG] Old version cluster startup problem");
                    out.writeInt(-1);
                } else {
                    feedBackPacket.write(out);
                    logger.debug(
                            "[HKLOG] Writing feedback packet back to server");
                }
                readHeader();
            } catch (Exception e) {
                logger.debug("intType = " + intType);
                logger.error("client break because of exception: ", e);
                closeResources();
                break;
            }
        }
    }

    private void writeRegisterPacket() {
        RegisterPacket registerPacket = new RegisterPacket(socket);
        try {
            registerPacket.write(out);
        } catch (IOException e) {
            logger.error("write register packet exception, " + e);
        }
    }

    private void readHeader() {
    }

    private void closeResources() {
        try {
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            logger.error("Error while closing resources: " + e.getMessage());
        }
    }
}
