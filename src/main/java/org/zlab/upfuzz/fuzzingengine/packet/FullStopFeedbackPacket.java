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
    public int testPacketID;

    public FeedBack[] feedBacks;
    public Map<Integer, Map<String, String>> systemStates;

    public String fullSequence = ""; // for reproducing
    public String configFileName;

    public boolean isUpgradeProcessFailed = false;
    public String upgradeFailureReport = "";

    public boolean isDowngradeProcessFailed = false;
    public String downgradeFailureReport = "";

    public boolean isInconsistent = false;
    public String inconsistencyReport = "";

    public boolean hasERRORLog = false;
    public String errorLogReport = "";

    public FullStopFeedbackPacket(String systemID, String configFileName,
            int testPacketID,
            FeedBack[] feedBacks,
            Map<Integer, Map<String, String>> systemStates) {
        this.type = PacketType.FullStopFeedbackPacket;

        this.systemID = systemID;
        this.configFileName = configFileName;
        this.testPacketID = testPacketID;
        this.feedBacks = feedBacks;
        this.systemStates = systemStates;
    }

    public static FullStopFeedbackPacket read(DataInputStream in) {
        return (FullStopFeedbackPacket) read(in, FullStopFeedbackPacket.class);
    }
}
