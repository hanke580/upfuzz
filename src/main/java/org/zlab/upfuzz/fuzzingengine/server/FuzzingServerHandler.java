package org.zlab.upfuzz.fuzzingengine.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.fuzzingengine.packet.*;
import org.zlab.upfuzz.fuzzingengine.packet.Packet.PacketType;

public class FuzzingServerHandler implements Runnable {
    static Logger logger = LogManager.getLogger(FuzzingServerHandler.class);

    private static int clientNum = 0;

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
            synchronized (FuzzingServerHandler.class) {
                clientNum++;
                logger.info("live client number: " + clientNum);
            }
            readRegisterPacket();
            while (true) {
                Packet testPacket = fuzzingServer
                        .getOneTest();
                assert testPacket != null;
                testPacket.write(out);
                readFeedbackPacket();
            }
            // TestPacket tp = fuzzingServer.getOneTest();
            // Gson gs = new Gson();
            // socket.getOutputStream().write(gs.toJson(tp).getBytes());
        } catch (Exception e) {
            logger.error("FuzzingServerHandler runs into exceptions ", e);
        } finally {
            synchronized (FuzzingServerHandler.class) {
                clientNum--;
                logger.info(
                        "one client crash with exception, current live clients: "
                                + clientNum);
            }
            // if this thread stops, the client should also stop
            closeResources();
        }
    }

    private void readFeedbackPacket() throws IOException {
        int intType = in.readInt();

        if (intType == PacketType.StackedFeedbackPacket.value) {
            StackedFeedbackPacket stackedFeedbackPacket = StackedFeedbackPacket
                    .read(in);

            fuzzingServer.updateStatus(stackedFeedbackPacket);
        } else if (intType == PacketType.TestPlanFeedbackPacket.value) {
            TestPlanFeedbackPacket testPlanFeedbackPacket = TestPlanFeedbackPacket
                    .read(in);
            fuzzingServer.updateStatus(testPlanFeedbackPacket);
        } else if (intType == PacketType.MixedFeedbackPacket.value) {
            MixedFeedbackPacket mixedFeedbackPacket = MixedFeedbackPacket
                    .read(in);
            fuzzingServer
                    .updateStatus(mixedFeedbackPacket.stackedFeedbackPacket);
            fuzzingServer
                    .updateStatus(mixedFeedbackPacket.testPlanFeedbackPacket);
        } else if (intType == PacketType.FullStopFeedbackPacket.value) {
            logger.info("read fullstop fb packet");
            FullStopFeedbackPacket fullStopFeedbackPacket = FullStopFeedbackPacket
                    .read(in);
            fuzzingServer.updateStatus(fullStopFeedbackPacket);
        } else if (intType == PacketType.VersionDeltaFeedbackPacket.value) {
            logger.info("read version delta fb packet");
            VersionDeltaFeedbackPacket versionDeltaFeedbackPacket = VersionDeltaFeedbackPacket
                    .read(in);
            fuzzingServer.updateStatus(versionDeltaFeedbackPacket);
        } else if (intType == -1) {
            // do nothing, null packet
            // TODO: We should avoid using that configuration!
            logger.error(
                    "cluster start up problem, empty packet. The generated test configurations might be wrong");
        } else {
            logger.error(
                    "Cannot recognize type " + intType);
        }
    }

    private void readRegisterPacket() throws IOException {
        int intType = in.readInt();
        assert intType == PacketType.RegisterPacket.value;
        RegisterPacket registerPacket = RegisterPacket.read(in);
    }

    public static void printClientNum() {
        synchronized (FuzzingServerHandler.class) {
            logger.info("Live clients: " + clientNum);
        }
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
