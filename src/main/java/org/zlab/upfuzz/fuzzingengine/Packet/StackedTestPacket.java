package org.zlab.upfuzz.fuzzingengine.Packet;

import com.google.gson.Gson;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.fuzzingengine.Server.Seed;

// This class is for execute multiple tests in one
// system instance. Like separating 60 tests with
// keyspace for cassandra to avoid the conflict
// between them for acceleration
public class StackedTestPacket extends Packet {
    // This wraps the test packet so that we can
    // control the number of tests we want to
    // execute in one instance.
    static Logger logger = LogManager.getLogger(StackedTestPacket.class);

    public int nodeNum;
    private List<TestPacket> tpList;

    public StackedTestPacket() {
        this.nodeNum = Config.getConf().nodeNum;
        this.type = PacketType.StackedTestPacket;
        tpList = new LinkedList<>();
    }

    public StackedTestPacket(int nodeNum) {
        this.nodeNum = nodeNum;
        this.type = PacketType.StackedTestPacket;
        tpList = new LinkedList<>();
    }

    public void addTestPacket(Seed seed, int testID) {
        if (seed.upgradedCommandSequence == null) {
            tpList.add(new TestPacket(
                    Config.getConf().system, testID,
                    seed.originalCommandSequence.getCommandStringList(), null,
                    seed.validationCommandSequnece.getCommandStringList()));
        } else {
            tpList.add(new TestPacket(
                    Config.getConf().system, testID,
                    seed.originalCommandSequence.getCommandStringList(),
                    seed.upgradedCommandSequence.getCommandStringList(),
                    seed.validationCommandSequnece.getCommandStringList()));
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
