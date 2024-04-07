package org.zlab.upfuzz.fuzzingengine.server.testanalyzer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.fuzzingengine.server.testtracker.BaseNode;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * == Offline analysis ==
 * Graph for the seed evolution
 */
public class TestGraph implements Serializable {
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
                    BaseNode baseNode = BaseNode
                            .deserializeNodeFromDisk(file);

                    // Currently Only Support TestTrackerNode
                    // TODO: support version delta node

                    addNode(baseNode);
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

    public List<TestNode> getRootNodes() {
        return rootNodes;
    }

    public void analyze(BufferedWriter writer) {
        try {
            print(writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addNode(BaseNode baseNode) {
        TestNode node = new TestNode(baseNode);
        if (baseNode.pNodeId == -1)
            rootNodes.add(node);
        nodeMap.put(baseNode.nodeId, node);

        // double pointer
        TestNode pNode = nodeMap.get(baseNode.pNodeId);
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

        if (node.baseNode.hasNewCoverage())
            writer.write(prefix + node.baseNode.nodeId + ":"
                    + node.baseNode.printCovInfo()
                    + "\n");
        else
            writer.write(prefix + node.baseNode.nodeId + "\n");

        List<TestNode> children = node.getChildren();
        if (children != null && !children.isEmpty()) {
            for (TestNode child : children) {
                printNode(child, prefix + "  ", writer); // 2 spaces for
                                                         // indentation
            }
        }
    }

    public void serializeToDisk(String filename) {
        try (ObjectOutputStream out = new ObjectOutputStream(
                new FileOutputStream(filename))) {
            out.writeObject(this);
        } catch (IOException e) {
            logger.error("Error during serialization", e);
        }
    }

    public static TestGraph deserializeFromDisk(String filename) {
        try (ObjectInputStream in = new ObjectInputStream(
                new FileInputStream(filename))) {
            return (TestGraph) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            logger.error("Error during deserialization", e);
            e.printStackTrace();
            System.exit(1);
        }
        return null;
    }

    public static void main(String[] args) {
        // runs separately
        new Config();

        TestGraph testGraph = new TestGraph();

        // Store testGraph
        testGraph.serializeToDisk("testGraph.ser");

        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter("testGraph.txt"))) {
            testGraph.analyze(writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
