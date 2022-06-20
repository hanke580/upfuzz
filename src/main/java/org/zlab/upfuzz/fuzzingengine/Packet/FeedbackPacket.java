package org.zlab.upfuzz.fuzzingengine.Packet;

import java.io.IOException;
import java.io.InputStream;

import com.google.gson.Gson;

import org.zlab.upfuzz.fuzzingengine.FeedBack;

public class FeedbackPacket extends Packet {
    public int testPacketID;
    public String systemID;

    public FeedBack feedBack;
    public boolean isInconsistent; // true if inconsistent
    public String inconsistencyReport; // The inconsistency information should
                                       // be placed here

    public FeedbackPacket(int testPacketID, String systemID,
            FeedBack feedBack) {
        this.testPacketID = testPacketID;
        this.systemID = systemID;
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
