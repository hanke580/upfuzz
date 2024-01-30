package org.zlab.upfuzz.fuzzingengine.packet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import org.zlab.ocov.tracker.type.*;

import java.io.DataOutputStream;
import java.io.IOException;

public abstract class Packet {
    PacketType type;

    static Gson gson;
    static {
        RuntimeTypeAdapterFactory<TypeInfo> typeFactory = RuntimeTypeAdapterFactory
                .of(TypeInfo.class, "type") // "type" is a field in JSON that
                                            // tells us what the
                // actual type is
                .registerSubtype(ArrayType.class, "array")
                .registerSubtype(BooleanType.class, "boolean")
                .registerSubtype(CollectionType.class, "collection")
                .registerSubtype(MapType.class, "map")
                .registerSubtype(DoubleType.class, "double")
                .registerSubtype(FloatType.class, "float")
                .registerSubtype(IntegerType.class, "integer")
                .registerSubtype(LongType.class, "long")
                .registerSubtype(ObjectType.class, "object")
                .registerSubtype(ShortType.class, "short")
                .registerSubtype(StringType.class, "string");

        gson = new GsonBuilder().registerTypeAdapterFactory(typeFactory)
                .create();
    }

    public void write(DataOutputStream out) throws IOException {
        out.writeInt(type.value);
        String packetStr = gson.toJson(this);
        byte[] packetByte = packetStr.getBytes();
        // logger.debug("send stacked test packet size: " + packetByte.length);
        out.writeInt(packetByte.length);
        out.write(packetByte);
    }

    public String getGsonStr() throws IOException {
        String packetStr = gson.toJson(this);
        return packetStr;
    }

    public enum PacketType {
        // -1 is reserved as a null packet
        RegisterPacket(0), StackedTestPacket(1), StackedFeedbackPacket(
                2), FeedbackPacket(3), TestPlanPacket(
                        4), TestPlanFeedbackPacket(5), MixedTestPacket(
                                6), MixedFeedbackPacket(7), FullStopPacket(
                                        8), FullStopFeedbackPacket(
                                                9), VersionDeltaFeedbackPacket(
                                                        10);

        public int value;

        private PacketType(int Value) {
            this.value = Value;
        }
    }
}
