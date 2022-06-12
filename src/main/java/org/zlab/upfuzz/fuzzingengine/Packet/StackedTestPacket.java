package org.zlab.upfuzz.fuzzingengine.Packet;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

import com.google.gson.Gson;
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

    private List<TestPacket> tpList;

    public StackedTestPacket() {
        tpList = new LinkedList<>();
    }

    public void addTest(Seed seed, int testID) {
        tpList.add(new TestPacket(Config.getConf().system, testID,
                seed.originalCommandSequence.getCommandStringList(),
                seed.upgradedCommandSequence.getCommandStringList(),
                seed.validationCommandSequnece.getCommandStringList()));
    }

    public int size() {
        return tpList.size();
    }

    public void write(OutputStream out) throws IOException {
        out.write(new Gson().toJson(this).getBytes());
    }
}
