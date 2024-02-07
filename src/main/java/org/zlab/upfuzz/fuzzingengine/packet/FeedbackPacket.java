package org.zlab.upfuzz.fuzzingengine.packet;

import com.google.gson.Gson;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.ocov.tracker.ObjectGraphCoverage;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.fuzzingengine.FeedBack;

public class FeedbackPacket extends Packet {
    static Logger logger = LogManager.getLogger(FeedbackPacket.class);

    public String systemID;
    public int nodeNum;
    public int testPacketID;

    // public FeedBack feedBack; // It should contain each node
    public FeedBack[] feedBacks;

    public boolean isInconsistent = false; // true if inconsistent
    public String inconsistencyReport;

    public List<String> validationReadResults;

    // inv status
    public boolean breakNewInv = false;
    public int[] brokenInvs;

    // format coverage
    public ObjectGraphCoverage formatCoverage;

    // has this test packet induced new version delta?
    public boolean inducedNewVersionDeltaBeforeVersionChange = false;

    public FeedbackPacket(String systemID, int nodeNum, int testPacketID,
            FeedBack[] feedBacks, List<String> validationReadResults) {
        this.type = PacketType.FeedbackPacket;

        this.systemID = systemID;
        this.nodeNum = Config.getConf().nodeNum;
        this.testPacketID = testPacketID;
        this.feedBacks = feedBacks;

        this.validationReadResults = validationReadResults;
    }

    public static FeedbackPacket read(DataInputStream in) {
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
                    FeedbackPacket.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
