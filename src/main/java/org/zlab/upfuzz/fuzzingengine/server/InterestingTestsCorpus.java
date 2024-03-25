package org.zlab.upfuzz.fuzzingengine.server;

import org.zlab.upfuzz.CommandSequence;
import org.zlab.upfuzz.fuzzingengine.packet.StackedTestPacket;
import org.zlab.upfuzz.utils.Pair;
import org.zlab.upfuzz.utils.Utilities;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Iterator;

public class InterestingTestsCorpus {
    public BlockingQueue<StackedTestPacket>[] queues = new LinkedBlockingQueue[3];
    // LinkedList<StackedTestPacket>[] queues = new LinkedList[3];
    {
        for (int i = 0; i < queues.length; i++) {
            queues[i] = new LinkedBlockingQueue<>();
        }
    }

    public StackedTestPacket getBatch(int type) {
        if (queues[type].isEmpty())
            return null;
        return queues[type].poll();
    }

    public StackedTestPacket peekBatch(int type) {
        if (queues[type].isEmpty())
            return null;
        return queues[type].peek();
    }

    public void addBatch(StackedTestPacket batch, int type) {
        queues[type].add(batch);
    }

    public boolean areAllQueuesEmpty() {
        for (BlockingQueue<StackedTestPacket> queue : queues) {
            if (!queue.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public void removeBatch(int targetBatchId, int type) {
        // Iterate through the queue using an iterator to avoid
        // ConcurrentModificationException
        Iterator<StackedTestPacket> iterator = queues[type].iterator();
        while (iterator.hasNext()) {
            StackedTestPacket nextBatch = iterator.next();
            if (nextBatch.batchId == targetBatchId) {
                iterator.remove();
                break; // Since there's only one seed with the target ID, we can
                       // break after removal
            }
        }
    }

    public boolean isEmpty(int type) {
        return queues[type].isEmpty();
    }
}
