package org.zlab.upfuzz.fuzzingengine.packet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class MixedFeedbackPacket extends Packet {
    static Logger logger = LogManager.getLogger(TestPlanFeedbackPacket.class);

    public StackedFeedbackPacket stackedFeedbackPacket;
    public TestPlanFeedbackPacket testPlanFeedbackPacket;

    public MixedFeedbackPacket(StackedFeedbackPacket stackedFeedbackPacket,
            TestPlanFeedbackPacket testPlanFeedbackPacket) {
        this.type = PacketType.MixedFeedbackPacket;

        this.stackedFeedbackPacket = stackedFeedbackPacket;
        this.testPlanFeedbackPacket = testPlanFeedbackPacket;
    }

    public static MixedFeedbackPacket read(DataInputStream in) {

        try {
            int type1 = in.readInt();
            assert type1 == PacketType.StackedFeedbackPacket.value;
            StackedFeedbackPacket stackedFeedbackPacket = StackedFeedbackPacket
                    .read(in);

            int type2 = in.readInt();
            assert type2 == PacketType.TestPlanFeedbackPacket.value;
            TestPlanFeedbackPacket testPlanFeedbackPacket = TestPlanFeedbackPacket
                    .read(in);

            return new MixedFeedbackPacket(stackedFeedbackPacket,
                    testPlanFeedbackPacket);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return null;
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeInt(type.value);

        stackedFeedbackPacket.write(out);
        testPlanFeedbackPacket.write(out);

    }

}
