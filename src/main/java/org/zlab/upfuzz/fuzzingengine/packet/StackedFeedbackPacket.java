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

public class StackedFeedbackPacket extends Packet implements Serializable {
    static Logger logger = LogManager.getLogger(StackedFeedbackPacket.class);

    public final List<FeedbackPacket> fpList;

    // TODO: Handle brokenInv for configurations (start up)
    // public Set<Integer> brokenInv; // configuration broken invariants!

    public String fullSequence = ""; // for reproducing
    public String configFileName;

    public boolean skipped = false; // skipped since no invariant

    public boolean isUpgradeProcessFailed = false;
    public String upgradeFailureReport;

    public boolean isDowngradeProcessFailed = false;
    public String downgradeFailureReport = "";

    public boolean hasERRORLog = false;
    public String errorLogReport = "";

    public boolean breakNewInv = false;

    // public int nodeNum;
    // (1) Failed Upgrade Process: Report all command sequences
    // (2) Result Inconsistency: Report the target seed's inconsistency

    public StackedFeedbackPacket(String configFileName) {
        this.configFileName = configFileName;
        this.type = PacketType.StackedFeedbackPacket;
        fpList = new LinkedList<>();
    }

    public void addFeedbackPacket(FeedbackPacket fp) {
        fpList.add(fp);
    }

    public List<FeedbackPacket> getFpList() {
        return fpList;
    }

    public int size() {
        return fpList.size();
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
            return new Gson().fromJson(new String(bytes, 0, len),
                    StackedFeedbackPacket.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
