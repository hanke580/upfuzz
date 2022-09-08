package org.zlab.upfuzz.fuzzingengine.Server;

import java.io.File;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.Queue;
import org.zlab.upfuzz.CommandSequence;
import org.zlab.upfuzz.utils.Pair;
import org.zlab.upfuzz.utils.Utilities;

public class Corpus {

    // PriorityQueue queue = new PriorityQueue<TestPacket>(
    // new TestPacketComparator());

    // Q0 corpus seed (level 0) always in the corpus

    Queue<Seed> queue = new LinkedList();

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
                    Utilities.printCommandSequence(commandSequencePair.left);
                    Utilities.printCommandSequence(commandSequencePair.right);

                    queue.add(new Seed(commandSequencePair.left,
                            commandSequencePair.right));
                }
            }
        }
        return true;
    }

    public Seed getSeed() {
        if (queue.isEmpty())
            return null;
        return queue.poll();
    }

    // Add one interesting seed to corpus
    public boolean addSeed(Seed seed) {
        queue.add(seed);
        return true;
    }
}
