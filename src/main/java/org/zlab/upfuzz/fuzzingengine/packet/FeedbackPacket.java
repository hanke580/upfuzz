package org.zlab.upfuzz.fuzzingengine.packet;

import java.io.DataInputStream;
import java.util.List;

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

    public FeedBack[] feedBacks;

    public boolean isInconsistent = false; // true if inconsistent
    public boolean isInconsistencyInsignificant = false;
    public String inconsistencyReport;

    public List<String> validationReadResults;

    // format coverage
    public ObjectGraphCoverage formatCoverage;

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
        return (FeedbackPacket) read(in, FeedbackPacket.class);
    }
}
