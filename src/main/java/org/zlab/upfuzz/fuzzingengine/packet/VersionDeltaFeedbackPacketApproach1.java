package org.zlab.upfuzz.fuzzingengine.packet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

public class VersionDeltaFeedbackPacketApproach1 extends Packet
        implements Serializable {
    public StackedFeedbackPacket stackedFeedbackPacketUpgrade;
    public StackedFeedbackPacket stackedFeedbackPacketDowngrade;

    public int clientGroup = 0;

    public VersionDeltaFeedbackPacketApproach1(
            StackedFeedbackPacket stackedFeedbackPacketUpgrade,
            StackedFeedbackPacket stackedFeedbackPacketDowngrade) {
        this.type = PacketType.VersionDeltaFeedbackPacketApproach1;

        this.stackedFeedbackPacketUpgrade = stackedFeedbackPacketUpgrade;
        this.stackedFeedbackPacketDowngrade = stackedFeedbackPacketDowngrade;
    }

    public static VersionDeltaFeedbackPacketApproach1 read(DataInputStream in) {
        return (VersionDeltaFeedbackPacketApproach1) read(in,
                VersionDeltaFeedbackPacketApproach1.class);
    }
}
