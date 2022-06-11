package org.zlab.upfuzz.fuzzingengine.Packet;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import com.google.gson.Gson;

// This class is for execute multiple tests in one
// system instance. Like separating 60 tests with
// keyspace for cassandra to avoid the conflict
// between them for acceleration
public class StackedTestPacket {
    // This wraps the test packet so that we can
    // control the number of tests we want to
    // execute in one instance.
    public List<TestPacket> tpList;

    public void write(OutputStream out) throws IOException {
        out.write(new Gson().toJson(this).getBytes());
    }
}
