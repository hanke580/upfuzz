package org.zlab.upfuzz.fuzzingengine.server.testanalyzer;

import org.junit.jupiter.api.Test;
import org.zlab.upfuzz.AbstractTest;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class TestGraphTest extends AbstractTest {
    // @Test
    public void testSerialize() {
        TestGraph graph = new TestGraph();
        graph.serializeToDisk("testGraph.ser");
    }

    // @Test
    public void testDeserialize() {
        TestGraph graph = TestGraph.deserializeFromDisk("testGraph.ser");
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(System.out))) {
            graph.analyze(writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
