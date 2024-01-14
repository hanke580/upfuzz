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
import java.util.concurrent.atomic.AtomicLong;
import java.util.Iterator;

public class PriorityCorpus {
    AtomicLong timestampGenerator = new AtomicLong(0); // To track enqueue order

    PriorityQueue<Seed> queue = new PriorityQueue<>(Collections.reverseOrder());
    // Queue<Seed> queue = new LinkedList<>();

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
                            commandSequencePair.right, -1, -1);
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
        seed.setTimestamp(timestampGenerator.getAndIncrement());
        queue.add(seed);
    }

    public void removeSeed(int targetTestId) {
        // Iterate through the queue using an iterator to avoid
        // ConcurrentModificationException
        Iterator<Seed> iterator = queue.iterator();
        while (iterator.hasNext()) {
            Seed seed = iterator.next();
            if (seed.testID == targetTestId) {
                iterator.remove();
                System.out.println(
                        "Seed with test ID " + targetTestId + " removed.");
                break; // Since there's only one seed with the target ID, we can
                       // break after removal
            }
        }
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

}
