package org.zlab.upfuzz.fuzzingengine.packet;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.fuzzingengine.FeedBack;

import java.io.DataInputStream;
import java.io.IOException;

public class TestPlanFeedbackPacket extends Packet {
    static Logger logger = LogManager.getLogger(TestPlanFeedbackPacket.class);

    public String systemID;
    public int testPacketID;

    // If the upgradeOp failed, this will be marked as true
    // We expect the system upgrade op should always succeed
    // no matter whether the normal command is correct or not
    public String fullSequence = ""; // for reproducing

    public boolean isEventFailed = false; // One event failed.
    public String eventFailedReport;

    public boolean hasERRORLog = false;
    public String errorLogReport = "";

    // For test plan, we only collect the new version coverage
    public FeedBack[] feedBacks;

    // TODO: We might want to compare the state between
    // (1) Rolling upgrade and (2) Full-stop upgrade
    public boolean isInconsistent = false; // true if inconsistent
    public String inconsistencyReport; // The inconsistency information should
    // be placed here

    public TestPlanFeedbackPacket(String systemID, int nodeNum,
            int testPacketID,
            FeedBack[] feedBacks) {
        this.type = PacketType.TestPlanFeedbackPacket;

        this.systemID = systemID;
        this.testPacketID = testPacketID;
        this.feedBacks = feedBacks;
    }

    public static TestPlanFeedbackPacket read(DataInputStream in) {
        try {
            int packetLength = in.readInt();
            byte[] bytes = new byte[packetLength + 1];
            int len = 0;
            len = in.read(bytes, len, packetLength - len);
            // logger.debug("packet length: " + packetLength);
            while (len < packetLength) {
                int size = in.read(bytes, len, packetLength - len);
                // logger.debug("packet read extra: " + size);
                len += size;
            }
            // logger.debug("receive stacked test packet length : " + len);
            return new Gson().fromJson(new String(bytes, 0, len),
                    TestPlanFeedbackPacket.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
