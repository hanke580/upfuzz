package org.zlab.upfuzz.fuzzingengine.server.testanalyzer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.fuzzingengine.server.testtracker.TestTrackerNode;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Graph for the seed evolution
 */
public class TestGraph {
    static Logger logger = LogManager.getLogger(TestGraph.class);

    private Map<Integer, TestNode> nodeMap = new HashMap<>();
    private List<TestNode> rootNodes = new ArrayList<>();

    public TestGraph() {
        loadGraph();
    }

    public void loadGraph() {
        // load all the nodes from disk
        // load from small to large

        // Directory containing the files
        Path graphDirPath = Paths.get(Config.getConf().testGraphDirPath);

        List<Path> subDirs = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files
                .newDirectoryStream(graphDirPath)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    subDirs.add(entry);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        subDirs.sort(Comparator.comparingInt(
                dir -> Integer.parseInt(dir.getFileName().toString())));

        for (Path subDir : subDirs) {
            // Get all the ".ser" files from the directory
            File[] files = subDir.toFile()
                    .listFiles((dir, name) -> name.endsWith(".ser"));

            // Sort the files based on their numeric value
            Arrays.sort(files, Comparator.comparingInt(
                    file -> Integer
                            .parseInt(file.getName().replace(".ser", ""))));

            // Process the files in ascending order
            int i = 0;
            for (File file : files) {
                // Replace this print statement with your file processing logic
                if (i % 1000 == 0)
                    System.out.println("Processing file: " + file.getName());
                try {
                    TestTrackerNode testTrackerNode = TestTrackerNode
                            .deserializeNodeFromDisk(file);
                    addNode(testTrackerNode);
                } catch (Exception e) {
                    logger.error(
                            "Error loading graph file: " + file.getName() + " "
                                    + e.getMessage());
                    e.printStackTrace();
                }
                i++;
            }
        }
    }

    public void analyze(BufferedWriter writer) {
        try {
            print(writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addNode(TestTrackerNode testTrackerNode) {
        TestNode node = new TestNode(testTrackerNode);
        if (node.pNodeId == -1)
            rootNodes.add(node);
        nodeMap.put(node.nodeId, node);

        // double pointer
        TestNode pNode = nodeMap.get(node.pNodeId);
        if (pNode != null) {
            pNode.addChild(node);
        }
    }

    public void print(BufferedWriter writer) throws IOException {
        for (TestNode rootNode : rootNodes) {
            printNode(rootNode, "", writer);
        }
    }

    private void printNode(TestNode node, String prefix, BufferedWriter writer)
            throws IOException {
        writer.write(prefix + node.nodeId + "\n");

        List<TestNode> children = node.getChildren();
        if (children != null && !children.isEmpty()) {
            for (TestNode child : children) {
                printNode(child, prefix + "  ", writer); // 2 spaces for
                                                         // indentation
            }
        }
    }

    public static void main(String[] args) {
        // runs separately
        new Config();

        TestGraph testGraph = new TestGraph();
        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter("testGraph.txt"))) {
            testGraph.analyze(writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
