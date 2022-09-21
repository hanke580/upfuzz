package org.zlab.upfuzz.fuzzingengine.Packet;

public class Packet {
    PacketType type;

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
