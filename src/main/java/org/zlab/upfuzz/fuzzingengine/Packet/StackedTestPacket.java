package org.zlab.upfuzz.fuzzingengine.Packet;

import java.util.List;

// This class is for execute multiple tests in one
// system instance. Like separating 60 tests with
// keyspace for cassandra to avoid the conflict
// between them for acceleration
public class StackedTestPacket {
    // This wraps the test packet so that we can
    // control the number of tests we want to
    // execute in one instance.
    public List<TestPacket> tpList;
}
