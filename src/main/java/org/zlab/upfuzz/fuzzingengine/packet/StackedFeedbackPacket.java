package org.zlab.upfuzz.fuzzingengine.packet;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.fuzzingengine.LogInfo;

/**
 * Client: Execute and reply the execute information
 */

public class StackedFeedbackPacket extends Packet implements Serializable {
    static Logger logger = LogManager.getLogger(StackedFeedbackPacket.class);

    public final List<FeedbackPacket> fpList;
    public List<List<String>> oriResults;
    public List<LogInfo> logInfos;

    // Include all testIDs (Either executed or not)
    // Need to remove them from testID2Seed for oom problem
    public final List<Integer> testIDs;

    public String fullSequence = ""; // for reproducing
    public String configFileName;

    public boolean upgradeSkipped = false;

    public boolean isUpgradeProcessFailed = false;
    public String upgradeFailureReport;

    public boolean isDowngradeProcessFailed = false;
    public String downgradeFailureReport = "";

    public boolean hasERRORLog = false;
    public String errorLogReport = "";

    public boolean breakNewInv = false;

    private String version;

    public StackedFeedbackPacket(String configFileName, List<Integer> testIDs) {
        this.configFileName = configFileName;
        this.testIDs = testIDs;
        this.type = PacketType.StackedFeedbackPacket;
        fpList = new LinkedList<>();
        oriResults = new LinkedList<>();
        logInfos = new LinkedList<>();
    }

    public void addFeedbackPacket(FeedbackPacket fp) {
        fpList.add(fp);
    }

    public void updateFeedbackPacket(int i, FeedbackPacket fp) {
        fpList.set(i, fp);
    }

    public List<FeedbackPacket> getFpList() {
        return fpList;
    }

    public List<List<String>> getOriResults() {
        return oriResults;
    }

    public List<LogInfo> getLogInfos() {
        return logInfos;
    }

    public int size() {
        return fpList.size();
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public static StackedFeedbackPacket read(DataInputStream in) {
        try {
            int packetLength = in.readInt();
            byte[] bytes = new byte[packetLength + 1];
            int len = 0;
            len = in.read(bytes, len, packetLength - len);
            // logger.debug("packet length: " + packetLength);
            while (len < packetLength) {
                int size = in.read(bytes, len, packetLength - len);
                len += size;
            }
            // logger.debug("get packet length " + len);
            return gson.fromJson(new String(bytes, 0, len),
                    StackedFeedbackPacket.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
