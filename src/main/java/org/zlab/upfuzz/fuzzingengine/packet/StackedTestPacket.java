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
import org.zlab.ocov.tracker.ObjectGraphCoverage;

// This class is for execute multiple tests in one
// system instance. Like separating 60 tests with
// keyspace for cassandra to avoid the conflict
// between them for acceleration
public class StackedTestPacket extends Packet implements Serializable {
    static Logger logger = LogManager.getLogger(StackedTestPacket.class);

    public int nodeNum;
    public String configFileName;
    private List<TestPacket> tpList;
    public transient ExecutionDataStore curOriCoverage;
    public transient ExecutionDataStore curUpCoverage;
    public transient ObjectGraphCoverage curOriObjCoverage;
    public transient ObjectGraphCoverage curUpObjCoverage;
    public int clientGroupForVersionDelta;
    public int batchId;
    public int testDirection;
    public boolean isDowngradeSupported;

    public StackedTestPacket(int nodeNum, String configFileName) {
        this.nodeNum = nodeNum;
        this.configFileName = configFileName;
        this.type = PacketType.StackedTestPacket;
        tpList = new LinkedList<>();
    }

    public void setCurOriCoverage(ExecutionDataStore curOriCoverage) {
        this.curOriCoverage = curOriCoverage;
    }

    public void setCurUpCoverage(ExecutionDataStore curUpCoverage) {
        this.curUpCoverage = curUpCoverage;
    }

    public void setCurOriObjCoverage(ObjectGraphCoverage curOriObjCoverage) {
        this.curOriObjCoverage = curOriObjCoverage;
    }

    public void setCurUpObjCoverage(ObjectGraphCoverage curUpObjCoverage) {
        this.curUpObjCoverage = curUpObjCoverage;
    }

    public ExecutionDataStore getCurOriCoverage() {
        return curOriCoverage;
    }

    public ExecutionDataStore getCurUpCoverage() {
        return curUpCoverage;
    }

    public ObjectGraphCoverage getCurOriObjCoverage() {
        return curOriObjCoverage;
    }

    public ObjectGraphCoverage getCurUpObjCoverage() {
        return curOriObjCoverage;
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
