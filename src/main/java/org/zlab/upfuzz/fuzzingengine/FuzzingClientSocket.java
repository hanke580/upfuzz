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
    int group;

    FuzzingClientSocket(FuzzingClient fuzzingClient, int group) {
        this.fuzzingClient = fuzzingClient;
        this.group = group;
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
                Packet.PacketType type = Packet.PacketType.values()[intType];
                logger.info("Packet val = " + intType + ", type = " + type);
                Packet feedBackPacket = null;
                int requestedGroupForVersionDelta = 0;

                switch (type) {
                // Now there's only StackedFeedbackPacket, there'll be
                // rolling upgrade instructions when testing rolling
                // upgrade
                case StackedTestPacket: {
                    // Run executor
                    StackedTestPacket stackedTestPacket = StackedTestPacket
                            .read(in);
                    requestedGroupForVersionDelta = stackedTestPacket.clientGroupForVersionDelta;
                    if (!Config.getConf().useVersionDelta) {
                        logger.info("Regular stacked testing");
                        feedBackPacket = fuzzingClient
                                .executeStackedTestPacket(stackedTestPacket);
                    } else {
                        logger.info("Version Delta testing");
                        if (Config.getConf().versionDeltaApproach == 2) {
                            feedBackPacket = fuzzingClient
                                    .executeStackedTestPacketVersionDelta(
                                            stackedTestPacket);
                        } else if (Config.getConf().versionDeltaApproach == 1) {
                            feedBackPacket = fuzzingClient
                                    .executeStackedTestPacketVersionDeltaApproach1(
                                            stackedTestPacket);
                        } else {
                            // runtime exception
                            throw new RuntimeException(
                                    "Invalid version delta approach");
                        }
                    }
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
                    logger.info("Feedback packet is null for group: " + group);
                    if (Config.getConf().useVersionDelta == true
                            && Config.getConf().versionDeltaApproach == 2) {
                        if (requestedGroupForVersionDelta != group) {
                            // Thread.sleep(10);
                            continue;
                        } else {
                            logger.debug(
                                    "[HKLOG] Old version cluster startup problem");
                            out.writeInt(-1);
                        }
                    } else {
                        logger.debug(
                                "[HKLOG] Old version cluster startup problem");
                        out.writeInt(-1);
                    }
                } else {
                    feedBackPacket.write(out);
                    logger.debug(
                            "[HKLOG] feedback packet sent to server");
                }
                readHeader();
            } catch (Exception e) {
                logger.debug("intType = " + intType);
                logger.error("client break because of exception: ", e);
                closeResources();
                return;
            }
        }
    }

    private void writeRegisterPacket() {
        RegisterPacket registerPacket = new RegisterPacket(socket, group);
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
