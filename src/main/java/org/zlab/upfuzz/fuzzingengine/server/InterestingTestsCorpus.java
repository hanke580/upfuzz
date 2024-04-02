package org.zlab.upfuzz.fuzzingengine.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.CommandSequence;
import org.zlab.upfuzz.fuzzingengine.configgen.ConfigInfo;
import org.zlab.upfuzz.fuzzingengine.packet.StackedTestPacket;
import org.zlab.upfuzz.fuzzingengine.packet.TestPacket;
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
    static Logger logger = LogManager.getLogger(InterestingTestsCorpus.class);

    public BlockingQueue<TestPacket>[] queues = new LinkedBlockingQueue[5];
    public BlockingQueue<String> configFiles = new LinkedBlockingQueue<>();

    // LinkedList<StackedTestPacket>[] queues = new LinkedList[3];
    {
        for (int i = 0; i < queues.length; i++) {
            queues[i] = new LinkedBlockingQueue<>();
        }
    }

<<<<<<< HEAD
    public enum TestType {
        FORMAT_COVERAGE_VERSION_DELTA, BRANCH_COVERAGE_VERSION_DELTA, FORMAT_COVERAGE, BRANCH_COVERAGE_BEFORE_VERSION_CHANGE, LOW_PRIORITY
    }

=======
>>>>>>> 9e1e8ed1f89b20129532ecfec18ac6e9d232223c
    public String getConfigFile() {
        if (configFiles.isEmpty())
            return null;
        return configFiles.poll();
    }

    public void addConfigFile(String configFileName) {
        configFiles.add(configFileName);
    }

<<<<<<< HEAD
    public TestPacket getPacket(TestType type) {
        if (queues[type.ordinal()].isEmpty())
            return null;
        return queues[type.ordinal()].poll();
    }

    public TestPacket peekPacket(TestType type) {
        if (queues[type.ordinal()].isEmpty())
            return null;
        return queues[type.ordinal()].peek();
    }

    public void addPacket(TestPacket packet, TestType type) {
        queues[type.ordinal()].add(packet);
=======
    public TestPacket getPacket(int type) {
        if (queues[type].isEmpty())
            return null;
        return queues[type].poll();
    }

    public TestPacket peekPacket(int type) {
        if (queues[type].isEmpty())
            return null;
        return queues[type].peek();
    }

    public void addPacket(TestPacket packet, int type) {
        queues[type].add(packet);
>>>>>>> 9e1e8ed1f89b20129532ecfec18ac6e9d232223c
    }

    public boolean areAllQueuesEmpty() {
        for (BlockingQueue<TestPacket> queue : queues) {
            if (!queue.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public boolean noInterestingTests() {
        for (int i = 0; i < queues.length - 1; i++) {
            if (!queues[i].isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public void removePacket(int targetPacketId, int type) {
        // Iterate through the queue using an iterator to avoid
        // ConcurrentModificationException
        Iterator<TestPacket> iterator = queues[type].iterator();
        while (iterator.hasNext()) {
            TestPacket nextPacket = iterator.next();
            if (nextPacket.testPacketID == targetPacketId) {
                iterator.remove();
                break; // Since there's only one packet with the target ID, we
                       // can
                       // break after removal
            }
        }
    }

<<<<<<< HEAD
    public boolean isEmpty(TestType type) {
        return queues[type.ordinal()].isEmpty();
=======
    public boolean isEmpty(int type) {
        return queues[type].isEmpty();
>>>>>>> 9e1e8ed1f89b20129532ecfec18ac6e9d232223c
    }

    public void printCache() {
        for (int i = 0; i < queues.length; i++) {
            logger.info("[HKLOG] Queue " + i + " size: " + queues[i].size());
        }
        logger.info("[HKLOG] Config file queue size: " + configFiles.size());
    }
}
