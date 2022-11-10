package org.zlab.upfuzz.fuzzingengine.packet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * It combines the stacked test packet with the test plan
 * - We'll execute 60 tests, and then execute one test plan
 * - Upgrade will be finished in the execution of test plan
 */
public class MixedTestPacket extends Packet {
    static Logger logger = LogManager.getLogger(MixedTestPacket.class);

    public StackedTestPacket stackedTestPacket;
    public TestPlanPacket testPlanPacket;

    public MixedTestPacket(StackedTestPacket stackedTestPacket,
            TestPlanPacket testPlanPacket) {
        this.type = PacketType.MixedTestPacket;

        this.stackedTestPacket = stackedTestPacket;
        this.testPlanPacket = testPlanPacket;
    }

    public static MixedTestPacket read(DataInputStream in) {
        try {
            int type1 = in.readInt();
            assert type1 == PacketType.StackedTestPacket.value;
            StackedTestPacket stackedTestPacket = StackedTestPacket.read(in);

            int type2 = in.readInt();
            assert type2 == PacketType.TestPlanFeedbackPacket.value;
            TestPlanPacket testPlanPacket = TestPlanPacket.read(in);

            return new MixedTestPacket(stackedTestPacket, testPlanPacket);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return null;
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeInt(type.value);

        stackedTestPacket.write(out);

        if (testPlanPacket == null) {
            logger.error("null testPlanPacket");
        }

        testPlanPacket.write(out);

    }

}
