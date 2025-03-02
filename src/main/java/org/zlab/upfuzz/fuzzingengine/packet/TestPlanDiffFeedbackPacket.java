package org.zlab.upfuzz.fuzzingengine.packet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.Serializable;

public class TestPlanDiffFeedbackPacket extends Packet implements Serializable {
    static Logger logger = LogManager.getLogger(TestPlanFeedbackPacket.class);

    public String systemID;
    public int testPacketID;

    public TestPlanFeedbackPacket[] testPlanFeedbackPackets;

    public TestPlanDiffFeedbackPacket(String systemID,
            int testPacketID,
            TestPlanFeedbackPacket[] testPlanFeedbackPackets) {
        this.type = PacketType.TestPlanDiffFeedbackPacket;

        this.systemID = systemID;
        this.testPacketID = testPacketID;
        this.testPlanFeedbackPackets = testPlanFeedbackPackets;
    }

    public static TestPlanDiffFeedbackPacket read(DataInputStream in) {
        return (TestPlanDiffFeedbackPacket) read(in,
                TestPlanDiffFeedbackPacket.class);
    }
}
