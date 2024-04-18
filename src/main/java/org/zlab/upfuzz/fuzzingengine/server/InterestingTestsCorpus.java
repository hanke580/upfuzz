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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Iterator;

public class InterestingTestsCorpus {
    static Logger logger = LogManager.getLogger(InterestingTestsCorpus.class);

    // public BlockingQueue<Pair<String,TestPacket>>[] queues = new
    // LinkedBlockingQueue[5];
    public HashMap<String, Queue<TestPacket>>[] intermediateBuffer = new HashMap[6];
    public List<String> configFiles = new ArrayList<>();

    // LinkedList<StackedTestPacket>[] queues = new LinkedList[3];
    {
        for (int i = 0; i < intermediateBuffer.length; i++) {
            intermediateBuffer[i] = new HashMap<String, Queue<TestPacket>>();
        }
    }

    public enum TestType {
        FORMAT_COVERAGE_VERSION_DELTA, BRANCH_COVERAGE_VERSION_DELTA, FORMAT_COVERAGE, BRANCH_COVERAGE_BEFORE_VERSION_CHANGE, BOUNDARY_BROKEN, LOW_PRIORITY
    }

    public String getConfigFile() {
        if (configFiles.isEmpty())
            return null;
        return configFiles.get(configFiles.size() - 1);
    }

    public String getConfigFileByIndex(int i) {
        List<String> configFileList = new ArrayList<>(configFiles);
        return configFileList.get(i);
    }

    public void addConfigFile(String configFileName) {
        configFiles.add(configFileName);
    }

    public TestPacket getPacket(TestType type, String configFileName) {
        if (intermediateBuffer[type.ordinal()].isEmpty())
            return null;
        else {
            HashMap<String, Queue<TestPacket>> bufferForThisConfig = intermediateBuffer[type
                    .ordinal()];
            if (bufferForThisConfig.keySet().contains(configFileName)) {
                Queue<TestPacket> listOfTests = bufferForThisConfig
                        .get(configFileName);
                TestPacket tp = listOfTests.poll();
                if (listOfTests.size() == 0) {
                    intermediateBuffer[type.ordinal()].remove(configFileName);
                }
                return tp;
            }
            return null;
        }
    }

    public TestPacket peekPacket(TestType type, String configFileName) {
        if (intermediateBuffer[type.ordinal()].isEmpty())
            return null;
        else {
            HashMap<String, Queue<TestPacket>> bufferForThisConfig = intermediateBuffer[type
                    .ordinal()];
            if (bufferForThisConfig.keySet().contains(configFileName)) {
                Queue<TestPacket> listOfTests = bufferForThisConfig
                        .get(configFileName);
                TestPacket tp = listOfTests.peek();
                return tp;
            }
            return null;
        }
    }

    public void addPacket(TestPacket packet, TestType type,
            String configFileName) {
        HashMap<String, Queue<TestPacket>> bufferForThisConfig = intermediateBuffer[type
                .ordinal()];
        if (!bufferForThisConfig.keySet().contains(configFileName)) {
            intermediateBuffer[type.ordinal()].put(configFileName,
                    new LinkedList<TestPacket>());
        }
        intermediateBuffer[type.ordinal()].get(configFileName).add(packet);
    }

    public boolean areAllQueuesEmpty() {
        for (HashMap<String, Queue<TestPacket>> bufferEntry : intermediateBuffer) {
            if (!bufferEntry.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public boolean areAllQueuesEmptyForThisConfig(String configFileName) {
        for (int i = 0; i < intermediateBuffer.length; i++) {
            if (intermediateBuffer[i].containsKey(configFileName)) {
                return false;
            }
        }
        return true;
    }

    public boolean noInterestingTests() {
        for (int i = 0; i < intermediateBuffer.length - 1; i++) {
            if (!intermediateBuffer[i].isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public boolean noInterestingTestsForThisConfig(String configFileName) {
        for (int i = 0; i < intermediateBuffer.length - 1; i++) {
            if (intermediateBuffer[i].containsKey(configFileName)) {
                return false;
            }
        }
        return true;
    }

    public void removePacket(int targetPacketId, int type,
            String configFileName) {
        // Iterate through the queue using an iterator to avoid
        // ConcurrentModificationException
        Queue<TestPacket> packetsQueue = intermediateBuffer[type]
                .get(configFileName);
        Iterator<TestPacket> iterator = packetsQueue.iterator();
        while (iterator.hasNext()) {
            TestPacket nextPacket = iterator.next();
            if (nextPacket.testPacketID == targetPacketId) {
                intermediateBuffer[type].get(configFileName).remove(nextPacket);
                if (intermediateBuffer[type].get(configFileName).size() == 0) {
                    intermediateBuffer[type].remove(configFileName);
                }
                break; // Since there's only one packet with the target ID, we
                       // can
                       // break after removal
            }
        }
    }

    public boolean isEmpty(TestType type) {
        return intermediateBuffer[type.ordinal()].isEmpty();
    }

    public void printCache() {
        for (int i = 0; i < intermediateBuffer.length; i++) {
            for (String configFileName : intermediateBuffer[i].keySet()) {
                logger.info("[HKLOG] Queue " + i + ", config file: "
                        + configFileName + ", size: "
                        + intermediateBuffer[i].get(configFileName).size());
            }
        }
        logger.info("[HKLOG] Config file queue size: " + configFiles.size());
    }

    public void printInfo() {
        // Print all six queues
        for (int i = 0; i < intermediateBuffer.length; i++) {
            int totalSize = 0;
            for (String configFileName : intermediateBuffer[i].keySet()) {
                totalSize += intermediateBuffer[i].get(configFileName).size();
            }
            System.out.format("|%30s|%61s|%30s|\n",
                    "Buffer Corpus",
                    "QueueType : " + TestType.values()[i],
                    "queue size : " + totalSize);
        }
    }
}
