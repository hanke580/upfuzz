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

// This class is for execute multiple tests in one
// system instance. Like separating 60 tests with
// keyspace for cassandra to avoid the conflict
// between them for acceleration
public class StackedTestPacket extends Packet implements Serializable {
    static Logger logger = LogManager.getLogger(StackedTestPacket.class);

    public int nodeNum;
    public String configFileName;
    private List<TestPacket> tpList;

    public Set<Integer> ignoredInvs; // inv which are broken all the time

    public StackedTestPacket(int nodeNum, String configFileName) {
        this.nodeNum = nodeNum;
        this.configFileName = configFileName;
        this.type = PacketType.StackedTestPacket;
        tpList = new LinkedList<>();
        ignoredInvs = new HashSet<>();
    }

    public void addTestPacket(Seed seed, int testID) {
        if (seed.upgradedCommandSequence == null) {
            tpList.add(new TestPacket(
                    Config.getConf().system, testID,
                    seed.originalCommandSequence.getCommandStringList(), null,
                    seed.validationCommandSequence.getCommandStringList()));
        } else {
            tpList.add(new TestPacket(
                    Config.getConf().system, testID,
                    seed.originalCommandSequence.getCommandStringList(),
                    seed.upgradedCommandSequence.getCommandStringList(),
                    seed.validationCommandSequence.getCommandStringList()));
        }
    }

    public void addTestPacket(TestPacket tp) {
        tpList.add(tp);
    }

    public List<TestPacket> getTestPacketList() {
        return tpList;
    }

    public int size() {
        return tpList.size();
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
