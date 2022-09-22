package org.zlab.upfuzz.fuzzingengine;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.fuzzingengine.Packet.*;
import org.zlab.upfuzz.fuzzingengine.testplan.TestPlan;

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
                switch (type) {
                // Now there's only StackedFeedbackPacket, there'll be
                // rolling upgrade instructions when testing rolling
                // upgrade
                case StackedTestPacket: {
                    // Run executor
                    StackedTestPacket stackedTestPacket = StackedTestPacket
                            .read(in);
                    StackedFeedbackPacket stackedFeedbackPacket = fuzzingClient
                            .executeStackedTestPacket(
                                    stackedTestPacket);
                    logger.debug(
                            "[Client] Writing stacked feedback packet back to server");

                    logger.debug("[Client] fp size = "
                            + stackedFeedbackPacket.size());
                    stackedFeedbackPacket.write(out);
                    break;
                }
                case TestPlanPacket: {
                    TestPlanPacket testPlanPacket = TestPlanPacket.read(in);
                    TestPlanFeedbackPacket testPlanFeedbackPacket = fuzzingClient
                            .executeTestPlanPacket(testPlanPacket);
                    testPlanFeedbackPacket.write(out);
                    logger.info(
                            "[Client] Writing testplan feedback packet to server");
                }
                }
                readHeader();
            } catch (Exception e) {
                System.out.println("intType = " + intType);
                e.printStackTrace();
                throw new RuntimeException(e);
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
}
