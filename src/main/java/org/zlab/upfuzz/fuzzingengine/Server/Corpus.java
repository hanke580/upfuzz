package org.zlab.upfuzz.fuzzingengine.Server;

import java.util.PriorityQueue;

import org.zlab.upfuzz.fuzzingengine.TestPacket;
import org.zlab.upfuzz.fuzzingengine.TestPacket.TestPacketComparator;

public class Corpus {

    PriorityQueue queue = new PriorityQueue<TestPacket>(
            new TestPacketComparator());

    public TestPacket getOneTest() {
        TestPacket tp = null;
        return tp;
    }
}
