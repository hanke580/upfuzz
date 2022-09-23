package org.zlab.upfuzz.fuzzingengine.Packet;

import com.google.gson.Gson;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.fuzzingengine.FeedBack;

public class FeedbackPacket extends Packet {
    static Logger logger = LogManager.getLogger(FeedbackPacket.class);

    public String systemID;
    public int testPacketID;

    public FeedBack feedBack;
    public boolean isInconsistent; // true if inconsistent
    public String inconsistencyReport; // The inconsistency information should
                                       // be placed here

    public FeedbackPacket(String systemID, int testPacketID,
            FeedBack feedBack) {
        this.type = PacketType.FeedbackPacket;

        this.systemID = systemID;
        this.testPacketID = testPacketID;
        this.feedBack = feedBack;
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
