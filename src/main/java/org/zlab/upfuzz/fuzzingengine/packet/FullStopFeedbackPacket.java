package org.zlab.upfuzz.fuzzingengine.packet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.fuzzingengine.FeedBack;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class FullStopFeedbackPacket extends Packet {
    static Logger logger = LogManager.getLogger(FeedbackPacket.class);

    public String systemID;
    public int nodeNum;
    public int testPacketID;

    public FeedBack[] feedBacks;
    public Map<Integer, Map<String, String>> systemStates;

    public String fullSequence = ""; // for reproducing

    public boolean isUpgradeProcessFailed = false;
    public String upgradeFailureReport;

    public boolean isInconsistent;
    public String inconsistencyReport;

    public boolean hasERRORLog = false;
    public String errorLogReport = "";

    public FullStopFeedbackPacket(String systemID, int nodeNum,
            int testPacketID,
            FeedBack[] feedBacks,
            Map<Integer, Map<String, String>> systemStates) {
        this.type = PacketType.FullStopFeedbackPacket;

        this.systemID = systemID;
        this.nodeNum = nodeNum;
        this.testPacketID = testPacketID;
        this.feedBacks = feedBacks;
        this.systemStates = systemStates;
    }

    public static FullStopFeedbackPacket read(DataInputStream in) {
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
            logger.debug("receive full-stop feedback length : " + len);
            return new Gson().fromJson(new String(bytes, 0, len),
                    FullStopFeedbackPacket.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
