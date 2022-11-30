package org.zlab.upfuzz.fuzzingengine.packet;

import com.google.gson.Gson;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Client: Execute and reply the execute information
 */

public class StackedFeedbackPacket extends Packet {
    static Logger logger = LogManager.getLogger(StackedFeedbackPacket.class);

    public final List<FeedbackPacket> fpList;
    public boolean isUpgradeProcessFailed = false;
    public String upgradeFailureReport;

    public boolean hasERRORLog = false;
    public String errorLogReport = "";

    public String stackedCommandSequenceStr;

    // public int nodeNum;
    // (1) Failed Upgrade Process: Report all command sequences
    // (2) Result Inconsistency: Report the target seed's inconsistency

    public StackedFeedbackPacket() {
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
