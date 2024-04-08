package org.zlab.upfuzz.fuzzingengine.packet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

public class VersionDeltaFeedbackPacketApproach2
        extends VersionDeltaFeedbackPacketApproach1 {

    public final List<TestPacket> tpList;

    public VersionDeltaFeedbackPacketApproach2(
            StackedFeedbackPacket stackedFeedbackPacketUpgrade,
            StackedFeedbackPacket stackedFeedbackPacketDowngrade,
            List<TestPacket> tpList) {
        super(stackedFeedbackPacketUpgrade, stackedFeedbackPacketDowngrade);
        this.type = PacketType.VersionDeltaFeedbackPacketApproach2;
        this.tpList = tpList;
    }

    public static VersionDeltaFeedbackPacketApproach2 read(DataInputStream in) {
        try {
            int packetLength = in.readInt();
            byte[] bytes = new byte[packetLength + 1];
            int len = 0;
            len = in.read(bytes, len, packetLength - len);
            // logger.debug("packet length: " + packetLength);
            while (len < packetLength) {
                int size = in.read(bytes, len, packetLength - len);
                len += size;
            }
            // logger.debug("get packet length " + len);
            return gson.fromJson(new String(bytes, 0, len),
                    VersionDeltaFeedbackPacketApproach2.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
