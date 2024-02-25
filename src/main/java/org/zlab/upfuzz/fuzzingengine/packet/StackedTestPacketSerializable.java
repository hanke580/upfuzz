package org.zlab.upfuzz.fuzzingengine.packet;

import com.google.gson.Gson;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.fuzzingengine.server.Seed;
import org.jacoco.core.data.ExecutionDataStore;
import org.zlab.ocov.tracker.ObjectCoverage;

// This class is for execute multiple tests in one
// system instance. Like separating 60 tests with
// keyspace for cassandra to avoid the conflict
// between them for acceleration
public class StackedTestPacketSerializable extends Packet
        implements Serializable {
    static Logger logger = LogManager.getLogger(StackedTestPacket.class);

    public int nodeNum;
    public String configFileName;
    private List<TestPacket> tpList;
    public int clientGroupForVersionDelta;

    public StackedTestPacketSerializable(int nodeNum, String configFileName,
            List<TestPacket> tpList, int clientGroupForVersionDelta) {
        this.nodeNum = nodeNum;
        this.configFileName = configFileName;
        this.type = PacketType.StackedTestPacket;
        this.tpList = tpList;
        this.clientGroupForVersionDelta = clientGroupForVersionDelta;
    }

    public List<TestPacket> getTestPacketList() {
        return tpList;
    }

    public static StackedTestPacket read(DataInputStream in) {
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
                    StackedTestPacket.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
