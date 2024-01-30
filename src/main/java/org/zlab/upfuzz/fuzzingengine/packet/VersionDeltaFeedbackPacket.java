package org.zlab.upfuzz.fuzzingengine.packet;

import com.google.gson.Gson;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Client: Execute and reply the execute information
 */

public class VersionDeltaFeedbackPacket extends Packet implements Serializable {
    static Logger logger = LogManager
            .getLogger(VersionDeltaFeedbackPacket.class);

    public final List<FeedbackPacket> fpListUpgrade;
    public final List<FeedbackPacket> fpListDowngrade;

    // include all testIDs (either executed or not)
    // we should remove them from testID2Seed for oom problem
    public final List<Integer> testIDs;
    // TODO: Handle brokenInv for configurations (start up)
    // public Set<Integer> brokenInv; // configuration broken invariants!

    public String fullSequence = ""; // for reproducing
    public String configFileName;

    public boolean skippedUpgrade = false; // skipped since no invariant
    public boolean skippedDowngrade = false; // skipped since no invariant

    public boolean isUpgradeProcessFailed = false;
    public String upgradeFailureReport;

    public boolean isDowngradeProcessFailed = false;
    public String downgradeFailureReport = "";

    public boolean hasERRORLog = false;
    public String errorLogReport = "";

    public boolean breakNewInvUp = false;
    public boolean breakNewInvDown = false;

    // public int nodeNum;
    // (1) Failed Upgrade Process: Report all command sequences
    // (2) Result Inconsistency: Report the target seed's inconsistency

    public VersionDeltaFeedbackPacket(String configFileName,
            List<Integer> testIDs) {
        this.configFileName = configFileName;
        this.testIDs = testIDs;
        this.type = PacketType.VersionDeltaFeedbackPacket;
        fpListUpgrade = new LinkedList<>();
        fpListDowngrade = new LinkedList<>();
    }

    public void addToFpList(FeedbackPacket fp, String choice) {
        if (choice.equals("up"))
            fpListUpgrade.add(fp);
        else
            fpListDowngrade.add(fp);
    }

    public List<FeedbackPacket> getFpList(String choice) {
        if (choice.equals("up"))
            return fpListUpgrade;
        else
            return fpListDowngrade;
    }

    public int size(String choice) {
        if (choice == "up")
            return fpListUpgrade.size();
        else
            return fpListDowngrade.size();
    }

    public static VersionDeltaFeedbackPacket read(DataInputStream in) {
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
                    VersionDeltaFeedbackPacket.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
