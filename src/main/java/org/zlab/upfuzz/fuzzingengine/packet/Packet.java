package org.zlab.upfuzz.fuzzingengine.packet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jgrapht.graph.DirectedMultigraph;
import org.zlab.ocov.tracker.graph.GraphDeserializer;
import org.zlab.ocov.tracker.graph.GraphSerializer;
import org.zlab.ocov.tracker.graph.label.LabelConstraint;
import org.zlab.ocov.tracker.graph.label.ValueConstraint;
import org.zlab.ocov.tracker.graph.structure.AccumulatedSizeConstraint;
import org.zlab.ocov.tracker.graph.structure.InDegreeConstraint;
import org.zlab.ocov.tracker.graph.structure.OutDegreeConstraint;
import org.zlab.ocov.tracker.graph.structure.StructureConstraint;
import org.zlab.ocov.tracker.inv.Invariant;
import org.zlab.ocov.tracker.inv.unary.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public abstract class Packet {
    static Logger logger = LogManager.getLogger(Packet.class);

    PacketType type;

    static Gson gson;
    static {
        RuntimeTypeAdapterFactory<LabelConstraint> typeFactory1 = RuntimeTypeAdapterFactory
                .of(LabelConstraint.class, "LabelConstraint")
                .registerSubtype(ValueConstraint.class, "ValueConstraint");

        RuntimeTypeAdapterFactory<Invariant> typeFactory2 = RuntimeTypeAdapterFactory
                .of(Invariant.class, "Invariant")
                .registerSubtype(UnaryInvariant.class, "UnaryInvariant");
        RuntimeTypeAdapterFactory<UnaryInvariant> typeFactory3 = RuntimeTypeAdapterFactory
                .of(UnaryInvariant.class, "UnaryInvariant")
                .registerSubtype(IntegerLowerBound.class, "IntegerLowerBound")
                .registerSubtype(IntegerUpperBound.class, "IntegerUpperBound")
                .registerSubtype(EmptyStringOnce.class, "EmptyStringOnce")
                .registerSubtype(TrueOnce.class, "TrueOnce")
                .registerSubtype(RestOnce.class, "RestOnce")
                .registerSubtype(FalseOnce.class, "FalseOnce")
                .registerSubtype(NegativeOneOnce.class, "NegativeOneOnce")
                .registerSubtype(OneCharStringOnce.class, "OneCharStringOnce")
                .registerSubtype(NullOnce.class, "NullOnce")
                .registerSubtype(OneOnce.class, "OneOnce")
                .registerSubtype(ZeroOnce.class, "ZeroOnce")
                .registerSubtype(PreservedStringOnce.class,
                        "PreservedStringOnce")
                .registerSubtype(RestStringSizeOnce.class, "RestStringSizeOnce")
                .registerSubtype(EnumConstant.class, "EnumConstant")
                .registerSubtype(LongLowerBound.class, "LongLowerBound")
                .registerSubtype(LongUpperBound.class, "LongUpperBound");
        RuntimeTypeAdapterFactory<StructureConstraint> typeFactory4 = RuntimeTypeAdapterFactory
                .of(StructureConstraint.class, "StructureConstraint")
                .registerSubtype(InDegreeConstraint.class, "InDegreeConstraint")
                .registerSubtype(OutDegreeConstraint.class,
                        "OutDegreeConstraint")
                .registerSubtype(AccumulatedSizeConstraint.class,
                        "AccumulatedSizeConstraint");

        gson = new GsonBuilder()
                .registerTypeAdapterFactory(typeFactory1)
                .registerTypeAdapterFactory(typeFactory2)
                .registerTypeAdapterFactory(typeFactory3)
                .registerTypeAdapterFactory(typeFactory4)
                .registerTypeAdapter(DirectedMultigraph.class,
                        new GraphSerializer())
                .registerTypeAdapter(DirectedMultigraph.class,
                        new GraphDeserializer())
                .create();
    }

    public void write(DataOutputStream out) throws IOException {
        out.writeInt(type.value);
        String packetStr = gson.toJson(this);
        byte[] packetByte = packetStr.getBytes();
        logger.debug("[Packet] Send packet size: " + packetByte.length);
        out.writeInt(packetByte.length);
        out.write(packetByte);
    }

    public String getGsonStr() throws IOException {
        return gson.toJson(this);
    }

    public enum PacketType {
        // -1 is reserved as a null packet
        RegisterPacket(0), StackedTestPacket(1), StackedFeedbackPacket(
                2), FeedbackPacket(3), TestPlanPacket(
                        4), TestPlanFeedbackPacket(
                                5), MixedTestPacket(6), MixedFeedbackPacket(
                                        7), VersionDeltaFeedbackPacket(
                                                10), VersionDeltaFeedbackPacketApproach1(
                                                        11), VersionDeltaFeedbackPacketApproach2(
                                                                12), TestPlanDiffFeedbackPacket(
                                                                        13);

        public final int value;

        PacketType(int value) {
            this.value = value;
        }
    }

    public static Object read(DataInputStream in, Class<?> clazz) {
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
            logger.debug("received packet length : " + len);
            return gson.fromJson(new String(bytes, 0, len),
                    clazz);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
