package org.zlab.upfuzz.fuzzingengine.Packet;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.fuzzingengine.FeedBack;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class TestPlanFeedbackPacket extends Packet {
    static Logger logger = LogManager.getLogger(TestPlanFeedbackPacket.class);

    public String systemID;
    public int testPacketID;

    // If the upgradeOp failed, this will be marked as true
    // We expect the system upgrade op should always succeed
    // no matter whether the normal command is correct or not
    public boolean isUpgradeOperationFailed;

    // For test plan, we only collect the new version coverage
    public FeedBack feedBack;

    // We might want to compare the state between
    // (1) Rolling upgrade and (2) Full-stop upgrade
    public boolean isInconsistent; // true if inconsistent
    public String inconsistencyReport; // The inconsistency information should
    // be placed here

    public TestPlanFeedbackPacket(String systemID, int testPacketID,
            FeedBack feedBack) {
        this.type = PacketType.TestPlanFeedbackPacket;

        this.systemID = systemID;
        this.testPacketID = testPacketID;
        this.feedBack = feedBack;
    }

    public static TestPlanFeedbackPacket read(DataInputStream in) {
        try {
            int packetLength = in.readInt();
            byte[] bytes = new byte[packetLength + 1];
            int len = 0;
            len = in.read(bytes, len, packetLength - len);
            logger.debug("packet length: " + packetLength);
            while (len < packetLength) {
                int size = in.read(bytes, len, packetLength - len);
                // logger.debug("packet read extra: " + size);
                len += size;
            }
            logger.debug("receive stacked test packet length : " + len);
            return new Gson().fromJson(new String(bytes, 0, len),
                    TestPlanFeedbackPacket.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
