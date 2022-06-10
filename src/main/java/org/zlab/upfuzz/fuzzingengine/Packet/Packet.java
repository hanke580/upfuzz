package org.zlab.upfuzz.fuzzingengine.Packet;

public class Packet {
    PacketType type;

    public enum PacketType {
        RegisterPacket(0), StackedTestPacket(1), FeedbackPacket(2);

        public int value;

        private PacketType(int Value) {
            this.value = Value;
        }
    }
}
