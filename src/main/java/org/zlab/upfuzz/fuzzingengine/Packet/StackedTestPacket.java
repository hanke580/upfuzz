package org.zlab.upfuzz.fuzzingengine.Packet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

import com.google.gson.Gson;

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

    private List<TestPacket> tpList;

    public StackedTestPacket() {
        this.type = PacketType.StackedTestPacket;
        tpList = new LinkedList<>();
    }

    public void addTestPacket(Seed seed, int testID) {
        if (seed.upgradedCommandSequence == null) {
            tpList.add(new TestPacket(Config.getConf().system, testID,
                    seed.originalCommandSequence.getCommandStringList(), null,
                    seed.validationCommandSequnece.getCommandStringList()));
        } else {
            tpList.add(new TestPacket(Config.getConf().system, testID,
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

    public static StackedTestPacket read(InputStream in) {
        byte[] bytes = new byte[1048576];
        int len = 0;
        try {
            len = in.read(bytes, len, 1048576 - len);
            int available = in.available();
            logger.debug("input stream available : " + available);
            while (available > 0) {
                int size = in.read(bytes, len, 1048576 - len);
                logger.debug("read length : " + size);
                logger.debug("input stream available : " + available);
                available = in.available();
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

    public void write(OutputStream out) throws IOException {
        out.write(type.value);
        String packetStr = new Gson().toJson(this);
        logger.debug("send packet size: " + packetStr.getBytes().length);
        out.write(packetStr.getBytes());
    }
}
