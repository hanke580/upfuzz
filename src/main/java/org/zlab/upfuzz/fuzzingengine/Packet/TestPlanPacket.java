package org.zlab.upfuzz.fuzzingengine.Packet;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.fuzzingengine.testplan.TestPlan;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class TestPlanPacket extends Packet {
    static Logger logger = LogManager.getLogger(TestPlanPacket.class);

    public String systemID;
    public int testPacketID;

    TestPlan testPlan;

    public TestPlanPacket(String systemID, int testPacketID,
            TestPlan testPlan) {
        this.type = PacketType.TestPlanPacket;

        this.systemID = systemID;
        this.testPacketID = testPacketID;
        this.testPlan = testPlan;
    }

    public TestPlan getTestPlan() {
        return testPlan;
    }

    public static TestPlanPacket read(DataInputStream in) {
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
                    TestPlanPacket.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void write(DataOutputStream out) throws IOException {
        out.writeInt(type.value);
        String packetStr = new Gson().toJson(this);
        byte[] packetByte = packetStr.getBytes();
        out.writeInt(packetByte.length);
        out.write(packetByte);
    }
}
