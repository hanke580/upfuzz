package org.zlab.upfuzz.fuzzingengine.Packet;

import com.google.gson.Gson;

import java.io.DataOutputStream;
import java.io.IOException;

public abstract class Packet {
    PacketType type;

    public void write(DataOutputStream out) throws IOException {
        out.writeInt(type.value);
        String packetStr = new Gson().toJson(this);
        byte[] packetByte = packetStr.getBytes();
        // logger.debug("send stacked test packet size: " + packetByte.length);
        out.writeInt(packetByte.length);
        out.write(packetByte);
    }

    public String getGsonStr() throws IOException {
        String packetStr = new Gson().toJson(this);
        return packetStr;
    }

    public enum PacketType {
        RegisterPacket(0), StackedTestPacket(1), StackedFeedbackPacket(
                2), FeedbackPacket(
                        3), TestPlanPacket(4), TestPlanFeedbackPacket(5);

        public int value;

        private PacketType(int Value) {
            this.value = Value;
        }
    }
}
