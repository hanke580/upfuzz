package org.zlab.upfuzz.fuzzingengine.packet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.fuzzingengine.testplan.FullStopUpgrade;

import java.io.DataInputStream;

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
        return (FullStopPacket) read(in, FullStopPacket.class);
    }
}
