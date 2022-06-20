package org.zlab.upfuzz.fuzzingengine.Packet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

import com.google.gson.Gson;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Client: Execute and reply the execute information
 */

public class StackedFeedbackPacket extends Packet {
    static Logger logger = LogManager.getLogger(StackedFeedbackPacket.class);

    private final List<FeedbackPacket> fpList;
    public boolean isUpgradeProcessFailed;
    public String stackedCommandSequenceStr;

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

    public static StackedFeedbackPacket read(InputStream in) {
        byte[] bytes = new byte[4194304];
        int len = 0;
        try {
            len = in.read(bytes, len, 4194304 - len);
            int available = in.available();
            logger.debug("try read length " + len + " avail: " + available);
            while (available > 0) {
                int size = in.read(bytes, len, 4194304 - len);
                len += size;
                available = in.available();
            }
            logger.debug("get packet length " + len + ":\n"
                    + new String(bytes, 0, len));
            return new Gson().fromJson(new String(bytes, 0, len),
                    StackedFeedbackPacket.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void write(OutputStream out) throws IOException {
        out.write(type.value);
        String packetStr = new Gson().toJson(this);
        logger.debug("send stacked feedback packet size: "
                + packetStr.getBytes().length + "\n" + packetStr);
        out.write(packetStr.getBytes());
    }
}
