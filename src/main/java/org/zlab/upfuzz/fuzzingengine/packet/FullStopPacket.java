package org.zlab.upfuzz.fuzzingengine.packet;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.fuzzingengine.testplan.FullStopUpgrade;

import java.io.DataInputStream;
import java.io.IOException;

public class FullStopPacket extends Packet {
    static Logger logger = LogManager.getLogger(StackedTestPacket.class);

    public String systemID;
    public int testPacketID;

    public String configFileName;
    public FullStopUpgrade fullStopUpgrade;

    public FullStopPacket(String systemID, int testPacketID,
            String configFileName,
            FullStopUpgrade fullStopUpgrade) {
        this.type = PacketType.FullStopPacket;

        this.systemID = systemID;
        this.testPacketID = testPacketID;
        this.configFileName = configFileName;
        this.fullStopUpgrade = fullStopUpgrade;
    }

    public int getNodeNum() {
        return fullStopUpgrade.nodeNum;
    }

    public static FullStopPacket read(DataInputStream in) {
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
                    FullStopPacket.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
