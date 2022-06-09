package org.zlab.upfuzz.fuzzingengine.Server;

import java.io.File;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;

import org.zlab.upfuzz.CommandSequence;
import org.zlab.upfuzz.fuzzingengine.TestPacket;
import org.zlab.upfuzz.fuzzingengine.TestPacket.TestPacketComparator;
import org.zlab.upfuzz.utils.Pair;
import org.zlab.upfuzz.utils.Utilities;

public class Corpus {

    // PriorityQueue queue = new PriorityQueue<TestPacket>(
    // new TestPacketComparator());

    Queue<Entry> queue = new LinkedList();

    public boolean initCorpus(Path initSeedDirPath) {
        File initSeedDir = initSeedDirPath.toFile();
        assert initSeedDir.isDirectory();
        for (File seedFile : initSeedDir.listFiles()) {
            if (!seedFile.isDirectory()) {
                Pair<CommandSequence, CommandSequence> commandSequencePair = Utilities
                        .deserializeCommandSequence(seedFile.toPath());
                if (commandSequencePair != null) {
                    // Fuzzer.saveSeed(commandSequencePair.left,
                    // commandSequencePair.right);
                    queue.add(new Entry(commandSequencePair.left,
                            commandSequencePair.right));
                }
            }
        }
        return true;
    }

    public TestPacket getOneTest() {
        TestPacket tp = null;
        return tp;
    }
}
