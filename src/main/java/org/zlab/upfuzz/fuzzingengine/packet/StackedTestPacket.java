package org.zlab.upfuzz.fuzzingengine.packet;

import java.io.DataInputStream;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

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
    private final List<TestPacket> tpList;
    public int clientGroupForVersionDelta;
    public int testDirection;
    public boolean isDowngradeSupported;

    // For skipping upgrade
    public ObjectGraphCoverage formatCoverage;
    public ExecutionDataStore branchCoverage;

    public StackedTestPacket(int nodeNum, String configFileName) {
        this.nodeNum = nodeNum;
        this.configFileName = configFileName;
        this.type = PacketType.StackedTestPacket;
        tpList = new LinkedList<>();
    }

    public void addTestPacket(Seed seed, int testID) {
        if (seed.upgradedCommandSequence == null) {
            tpList.add(new TestPacket(
                    Config.getConf().system, testID,
                    seed.originalCommandSequence.getCommandStringList(), null,
                    seed.validationCommandSequence.getCommandStringList(),
                    seed.mutationDepth));
        } else {
            tpList.add(new TestPacket(
                    Config.getConf().system, testID,
                    seed.originalCommandSequence.getCommandStringList(),
                    seed.upgradedCommandSequence.getCommandStringList(),
                    seed.validationCommandSequence.getCommandStringList(),
                    seed.mutationDepth));
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
        return (StackedTestPacket) read(in, StackedTestPacket.class);
    }
}
