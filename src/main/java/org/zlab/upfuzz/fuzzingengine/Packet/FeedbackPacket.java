package org.zlab.upfuzz.fuzzingengine.Packet;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import org.zlab.upfuzz.fuzzingengine.FeedBack;

public class FeedbackPacket extends Packet {
    public String systemID;
    public int testPacketID;

    public FeedBack feedBack;
    public boolean isInconsistent; // true if inconsistent
    public String inconsistencyReport; // The inconsistency information should
                                       // be placed here

    public FeedbackPacket(String systemID, int testPacketID,
            FeedBack feedBack) {
        this.systemID = systemID;
        this.testPacketID = testPacketID;
        this.feedBack = feedBack;
    }

    public static FeedbackPacket read(InputStream in) {
        byte[] bytes = new byte[4194304];
        int len;
        try {
            len = in.read(bytes);
            return new Gson().fromJson(new String(bytes, 0, len),
                    FeedbackPacket.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
