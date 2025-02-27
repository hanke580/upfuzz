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

    private final FuzzingServer fuzzingServer;
    private final Socket socket;
    private int clientGroup;
    DataInputStream in;
    DataOutputStream out;

    public void addBatchesToInterestingTestCorpus(
            VersionDeltaFeedbackPacketApproach2 versionDeltaFeedbackPacket) {
        fuzzingServer.analyzeFeedbackFromVersionDeltaGroup1(
                versionDeltaFeedbackPacket);
        if (Config.getConf().debug) {
            logger.info("Added element to shared queue. ");
        }
        synchronized (fuzzingServer.testBatchCorpus) {
            if (!fuzzingServer.testBatchCorpus.areAllQueuesEmpty()) {
                fuzzingServer.testBatchCorpus.notifyAll();
            }
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
                        assert testPacket != null;
                        logger.info(
                                "[HKLOG: server handler] client group for version delta: "
                                        + ((StackedTestPacket) testPacket).clientGroupForVersionDelta);
                        testPacket.write(out);
                        readFeedbackPacket();

                        if (Config.getConf().debug) {
                            synchronized (fuzzingServer.testBatchCorpus) {
                                int queueSizeQueue0 = 0;
                                int queueSizeQueue1 = 0;
                                int queueSizeQueue2 = 0;
                                int queueSizeQueue3 = 0;
                                int queueSizeQueue4 = 0;

                                for (String key : fuzzingServer.testBatchCorpus.intermediateBuffer[InterestingTestsCorpus.TestType.BRANCH_COVERAGE_BEFORE_VERSION_CHANGE
                                        .ordinal()].keySet()) {
                                    queueSizeQueue3 += fuzzingServer.testBatchCorpus.intermediateBuffer[InterestingTestsCorpus.TestType.BRANCH_COVERAGE_BEFORE_VERSION_CHANGE
                                            .ordinal()].get(key).size();
                                }

                                for (String key : fuzzingServer.testBatchCorpus.intermediateBuffer[InterestingTestsCorpus.TestType.FORMAT_COVERAGE
                                        .ordinal()].keySet()) {
                                    queueSizeQueue2 += fuzzingServer.testBatchCorpus.intermediateBuffer[InterestingTestsCorpus.TestType.FORMAT_COVERAGE
                                            .ordinal()].get(key).size();
                                }

                                for (String key : fuzzingServer.testBatchCorpus.intermediateBuffer[InterestingTestsCorpus.TestType.BRANCH_COVERAGE_VERSION_DELTA
                                        .ordinal()].keySet()) {
                                    queueSizeQueue1 += fuzzingServer.testBatchCorpus.intermediateBuffer[InterestingTestsCorpus.TestType.BRANCH_COVERAGE_VERSION_DELTA
                                            .ordinal()].get(key).size();
                                }

                                for (String key : fuzzingServer.testBatchCorpus.intermediateBuffer[InterestingTestsCorpus.TestType.FORMAT_COVERAGE_VERSION_DELTA
                                        .ordinal()].keySet()) {
                                    queueSizeQueue0 += fuzzingServer.testBatchCorpus.intermediateBuffer[InterestingTestsCorpus.TestType.FORMAT_COVERAGE_VERSION_DELTA
                                            .ordinal()].get(key).size();
                                }

                                for (String key : fuzzingServer.testBatchCorpus.intermediateBuffer[InterestingTestsCorpus.TestType.LOW_PRIORITY
                                        .ordinal()].keySet()) {
                                    queueSizeQueue4 += fuzzingServer.testBatchCorpus.intermediateBuffer[InterestingTestsCorpus.TestType.LOW_PRIORITY
                                            .ordinal()].get(key).size();
                                }

                                logger.info(
                                        "Tests inducing Branch coverage in both versions: "
                                                + queueSizeQueue3);
                                logger.info(
                                        "Tests inducing Format coverage in both versions: "
                                                + queueSizeQueue2);
                                logger.info(
                                        "Tests inducing version delta in branch coverage: "
                                                + queueSizeQueue1);
                                logger.info(
                                        "Tests inducing version delta in format coverage: "
                                                + queueSizeQueue0);
                                logger.info("Tests inducing no new coverage: "
                                        + queueSizeQueue4);
                            }
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
                            assert stackedTestPacketForGroup2 != null;
                            stackedTestPacketForGroup2.write(out);
                            readFeedbackPacket();
                        } catch (Exception e) {
                            logger.error("Error while writing test packet: ",
                                    e);
                            for (StackTraceElement ste : e.getStackTrace()) {
                                logger.error(ste);
                            }
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
                        "one client crash with exception, client group = "
                                + clientGroup +
                                ", current live clients: "
                                + clientNum);
            }
            // if this thread stops, the client should also stop
            closeResources();
        }
    }

    private void readFeedbackPacket() throws IOException {
        int intType = in.readInt();
        logger.info("feedback type = " + intType);
        if (intType == PacketType.StackedFeedbackPacket.value) {
            StackedFeedbackPacket stackedFeedbackPacket = StackedFeedbackPacket
                    .read(in);
            fuzzingServer.updateStatus(stackedFeedbackPacket);
        } else if (intType == PacketType.TestPlanFeedbackPacket.value) {
            TestPlanFeedbackPacket testPlanFeedbackPacket = TestPlanFeedbackPacket
                    .read(in);
            fuzzingServer.updateStatus(testPlanFeedbackPacket);
        } else if (intType == PacketType.TestPlanDiffFeedbackPacket.value) {
            TestPlanDiffFeedbackPacket testPlanDiffFeedbackPacket = TestPlanDiffFeedbackPacket
                    .read(in);
            fuzzingServer.updateStatus(testPlanDiffFeedbackPacket);
        } else if (intType == PacketType.MixedFeedbackPacket.value) {
            MixedFeedbackPacket mixedFeedbackPacket = MixedFeedbackPacket
                    .read(in);
            fuzzingServer
                    .updateStatus(mixedFeedbackPacket.stackedFeedbackPacket);
            fuzzingServer
                    .updateStatus(mixedFeedbackPacket.testPlanFeedbackPacket);
        } else if (intType == PacketType.VersionDeltaFeedbackPacketApproach2.value) {
            logger.info("read version delta fb packet");
            VersionDeltaFeedbackPacketApproach2 versionDeltaFeedbackPacketApproach2 = VersionDeltaFeedbackPacketApproach2
                    .read(in);
            if (Config.getConf().debug) {
                assert versionDeltaFeedbackPacketApproach2 != null;
                logger.info("Sent from group: "
                        + versionDeltaFeedbackPacketApproach2.clientGroup);
            }
            if (Config.getConf().versionDeltaApproach == 2) {
                logger.info("Got version delta feedback packet from group: "
                        + versionDeltaFeedbackPacketApproach2.clientGroup);
                if (this.clientGroup == 2
                        && versionDeltaFeedbackPacketApproach2.clientGroup == 1) {
                    try {
                        if (Config.getConf().debug) {
                            logger.info(
                                    "HERE!!!! MATCHED THIS CONDITION: clientGroup 2, got feedback packet from group 1!");
                        }
                        StackedTestPacket stackedTestPacketForGroup2 = fuzzingServer.stackedTestPacketsQueueVersionDelta
                                .take();
                        stackedTestPacketForGroup2.write(out);
                    } catch (Exception e) {
                        // Handle interruption gracefully
                        e.printStackTrace();
                    }
                } else if (this.clientGroup == 1
                        && versionDeltaFeedbackPacketApproach2.clientGroup == 1) {
                    try {
                        if (Config.getConf().debug) {
                            logger.info(
                                    "MATCHED THIS CONDITION: clientGroup 1, got feedback packet from group 1!");
                        }

                        logger.info("Going to call update corpus for group 1");
                        addBatchesToInterestingTestCorpus(
                                versionDeltaFeedbackPacketApproach2);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (this.clientGroup == 2
                        && versionDeltaFeedbackPacketApproach2.clientGroup == 2) {
                    if (Config.getConf().debug) {
                        logger.info(
                                "MATCHED THIS CONDITION: clientGroup 2, got feedback packet from group 2, now update status! Induced new version delta coverage? ");
                    }
                    fuzzingServer.analyzeFeedbackFromVersionDeltaGroup2(
                            versionDeltaFeedbackPacketApproach2);
                }
            } else {
                throw new RuntimeException(
                        "Invalid version delta approach");
            }
        } else if (intType == PacketType.VersionDeltaFeedbackPacketApproach1.value) {
            assert Config.getConf().versionDeltaApproach == 1;
            VersionDeltaFeedbackPacketApproach1 versionDeltaFeedbackPacket = VersionDeltaFeedbackPacketApproach1
                    .read(in);
            fuzzingServer.analyzeFeedbackFromVersionDelta(
                    versionDeltaFeedbackPacket);
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
        return registerPacket.group;
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
