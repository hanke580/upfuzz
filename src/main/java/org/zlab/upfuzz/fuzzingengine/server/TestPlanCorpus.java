package org.zlab.upfuzz.fuzzingengine.server;

import org.zlab.upfuzz.fuzzingengine.testplan.TestPlan;

import java.util.LinkedList;
import java.util.Queue;

public class TestPlanCorpus {
    Queue<TestPlan> queue = new LinkedList<>();

    public TestPlan getTestPlan() {
        if (queue.isEmpty())
            return null;
        return queue.poll();
    }

    public boolean addTestPlan(TestPlan testPlan) {
        queue.add(testPlan);
        return true;
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }
}
