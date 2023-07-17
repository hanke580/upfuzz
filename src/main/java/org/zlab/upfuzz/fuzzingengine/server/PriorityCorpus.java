package org.zlab.upfuzz.fuzzingengine.server;

import org.zlab.upfuzz.CommandSequence;
import org.zlab.upfuzz.utils.Pair;
import org.zlab.upfuzz.utils.Utilities;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;

public class PriorityCorpus {

    // PriorityQueue<Seed> queue = new
    // PriorityQueue<>(Collections.reverseOrder());
    Queue<Seed> queue = new LinkedList<>();

    public boolean initCorpus(Path initSeedDirPath) {
        if (!initSeedDirPath.toFile().exists())
            return true;
        File initSeedDir = initSeedDirPath.toFile();
        assert initSeedDir.isDirectory();
        for (File seedFile : initSeedDir.listFiles()) {
            if (!seedFile.isDirectory()) {
                Pair<CommandSequence, CommandSequence> commandSequencePair = Utilities
                        .deserializeCommandSequence(seedFile.toPath());
                if (commandSequencePair != null) {
                    Seed seed = new Seed(commandSequencePair.left,
                            commandSequencePair.right, -1);
                    seed.score = 10;
                    queue.add(seed);
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

    public Seed peekSeed() {
        if (queue.isEmpty())
            return null;
        return queue.peek();
    }

    public void addSeed(Seed seed) {
        queue.add(seed);
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

}
