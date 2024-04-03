package org.zlab.upfuzz.fuzzingengine.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.fuzzingengine.packet.*;
import org.zlab.upfuzz.fuzzingengine.packet.Packet.PacketType;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.fuzzingengine.server.InterestingTestsCorpus;

public class FuzzingServerHandler implements Runnable {
    static Logger logger = LogManager.getLogger(FuzzingServerHandler.class);

    private static int clientNum = 0;
    private static int group1ClientCount = 0;
    private static int group2ClientCount = 0;

    private FuzzingServer fuzzingServer;
    private Socket socket;
    private int clientGroup;
    DataInputStream in;
    DataOutputStream out;
    DataOutputStream outGroup2;

    public void addBatchesToInterestingTestCorpus(
            VersionDeltaFeedbackPacket versionDeltaFeedbackPacket) {
        fuzzingServer.analyzeFeedbackFromVersionDeltaGroup1(
                versionDeltaFeedbackPacket);
        if (Config.getConf().debug) {
            logger.info("Added element to shared queue. ");
        }
        synchronized (fuzzingServer.testBatchCorpus) {
            fuzzingServer.testBatchCorpus.notifyAll();
        }
    }

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
            this.clientGroup = readRegisterPacket();
            synchronized (FuzzingServerHandler.class) {
                if (clientGroup == 1) {
                    group1ClientCount++;
                    logger.info(
                            "live client number group1: "
                                    + group1ClientCount);
                } else if (clientGroup == 2) {
                    group2ClientCount++;
                    logger.info(
                            "live client number group2: "
                                    + group2ClientCount);
                }
            }
            while (true) {
                Packet testPacket;
                if (!(Config.getConf().useVersionDelta
                        && Config.getConf().versionDeltaApproach == 2)) {
                    testPacket = fuzzingServer
                            .getOneTest();
                    assert testPacket != null;
                    testPacket.write(out);
                } else {
                    if (this.clientGroup == 1) {
                        testPacket = fuzzingServer.getOneTest();
                        logger.info(
                                "[HKLOG: server handler] client group for version delta: "
                                        + ((StackedTestPacket) testPacket).clientGroupForVersionDelta);
                        assert testPacket != null;
                        testPacket.write(out);
                        readFeedbackPacket();

                        synchronized (fuzzingServer.testBatchCorpus) {
                            logger.info(
                                    "Tests inducing Branch coverage in both versions: "
                                            +
                                            fuzzingServer.testBatchCorpus.queues[InterestingTestsCorpus.TestType.BRANCH_COVERAGE_BEFORE_VERSION_CHANGE
                                                    .ordinal()]
                                                            .size());
                            logger.info(
                                    "Tests inducing Format coverage in both versions: "
                                            +
                                            fuzzingServer.testBatchCorpus.queues[InterestingTestsCorpus.TestType.FORMAT_COVERAGE
                                                    .ordinal()]
                                                            .size());
                            logger.info(
                                    "Tests inducing version delta in branch coverage: "
                                            +
                                            fuzzingServer.testBatchCorpus.queues[InterestingTestsCorpus.TestType.BRANCH_COVERAGE_VERSION_DELTA
                                                    .ordinal()]
                                                            .size());
                            logger.info(
                                    "Tests inducing version delta in format coverage: "
                                            +
                                            fuzzingServer.testBatchCorpus.queues[InterestingTestsCorpus.TestType.FORMAT_COVERAGE_VERSION_DELTA
                                                    .ordinal()]
                                                            .size());
                        }
                    } else {
                        StackedTestPacket stackedTestPacketForGroup2 = null;
                        synchronized (fuzzingServer.testBatchCorpus) {
                            while (fuzzingServer.testBatchCorpus
                                    .areAllQueuesEmpty()
                                    || fuzzingServer.testBatchCorpus.configFiles
                                            .size() == 0) {
                                try {
                                    fuzzingServer.testBatchCorpus
                                            .wait();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            if (Config.getConf().debug) {
                                logger.info(
                                        "Now executing version delta induced test packets in group 2");
                            }
                            try {
                                stackedTestPacketForGroup2 = fuzzingServer
                                        .getOneBatch();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        try {
                            Packet testPacketForGroup2 = (Packet) stackedTestPacketForGroup2;
                            testPacketForGroup2.write(out);
                            readFeedbackPacket();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                if (this.clientGroup != 1 && this.clientGroup != 2) {
                    readFeedbackPacket();
                }
            }
        } catch (Exception e) {
            logger.error("FuzzingServerHandler runs into exceptions ", e);
            e.printStackTrace();
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
        System.out.println(intType);
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
            if (Config.getConf().debug) {
                logger.info("Sent from group: "
                        + versionDeltaFeedbackPacket.clientGroup);
            }
            if (Config.getConf().versionDeltaApproach == 2) {
                logger.info("Got version delta feedback packet from group: "
                        + versionDeltaFeedbackPacket.clientGroup);
                if (this.clientGroup == 2
                        && versionDeltaFeedbackPacket.clientGroup == 1) {
                    try {
                        if (Config.getConf().debug) {
                            logger.info(
                                    "HERE!!!! MATCHED THIS CONDITION: clientGroup 2, got feedback packet from group 1!");
                        }
                        StackedTestPacket stackedTestPacketForGroup2 = fuzzingServer.stackedTestPacketsQueueVersionDelta
                                .take();
                        Packet testPacketForGroup2 = (Packet) stackedTestPacketForGroup2;
                        testPacketForGroup2.write(out);
                    } catch (Exception e) {
                        // Handle interruption gracefully
                        e.printStackTrace();
                    }
                } else if (this.clientGroup == 1
                        && versionDeltaFeedbackPacket.clientGroup == 1) {
                    try {
                        if (Config.getConf().debug) {
                            logger.info(
                                    "MATCHED THIS CONDITION: clientGroup 1, got feedback packet from group 1!");
                        }

                        logger.info("Going to call update corpus for group 1");
                        addBatchesToInterestingTestCorpus(
                                versionDeltaFeedbackPacket);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (this.clientGroup == 2
                        && versionDeltaFeedbackPacket.clientGroup == 2) {
                    if (Config.getConf().debug) {
                        logger.info(
                                "MATCHED THIS CONDITION: clientGroup 2, got feedback packet from group 2, now update status! Induced new version delta coverage? "
                                        + versionDeltaFeedbackPacket.inducedNewVersionDeltaCoverage);
                    }
                    logger.info("Calling update status");
                    versionDeltaFeedbackPacket.inducedNewVersionDeltaCoverage = true;
                    fuzzingServer.analyzeFeedbackFromVersionDeltaGroup2(
                            versionDeltaFeedbackPacket);
                }
            } else {
                fuzzingServer.analyzeFeedbackFromVersionDeltaGroup2(
                        versionDeltaFeedbackPacket);
            }
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

    private int readRegisterPacket() throws IOException {
        int intType = in.readInt();
        assert intType == PacketType.RegisterPacket.value;
        RegisterPacket registerPacket = RegisterPacket.read(in);
        int clientGroup = registerPacket.group;
        return clientGroup;
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
