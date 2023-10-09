package org.zlab.upfuzz.fuzzingengine.server.testanalyzer;

import org.zlab.upfuzz.fuzzingengine.Config;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class Analyzer {

    public boolean isStrict = false;

    public Analyzer(boolean isStrict) {
        this.isStrict = isStrict;
    }

    public void analyze() {
        // load the graph, find all nodes with new coverage
        TestGraph testGraph = TestGraph.deserializeFromDisk("testGraph.ser");
        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter("analysis.txt"))) {
            traverse(testGraph, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void traverse(TestGraph testGraph, BufferedWriter writer)
            throws IOException {
        for (TestNode rootNode : testGraph.getRootNodes()) {
            traverseNode(rootNode, "", writer);
        }
    }

    private void traverseNode(TestNode node, String prefix,
            BufferedWriter writer)
            throws IOException {
        if (node.newCoverage) {
            // Only print the new coverage nodes
            boolean check;
            if (isStrict)
                check = strictChecker(node);
            else
                check = normalChecker(node);
            writer.write(prefix + node.nodeId + ": new coverage"
                    + ", checker = " + check + "\n");
        }

        List<TestNode> children = node.getChildren();
        if (children != null && !children.isEmpty()) {
            for (TestNode child : children) {
                traverseNode(child, prefix + "  ", writer);
            }
        }
    }

    public boolean strictChecker(TestNode testNode) {
        List<String> writeCommands = testNode.writeCommands;
        // It should contain (1) Create table + set (2) INSERT (3) DROP Table

        boolean createTable = false;
        boolean insert = false;
        boolean drop = false;

        for (String cmd : writeCommands) {
            if (cmd.contains("CREATE TABLE")
                    && cmd.toLowerCase().contains("set<") && !createTable)
                createTable = true;

            if (createTable && cmd.contains("INSERT") && !insert)
                insert = true;

            if (insert && cmd.contains(" DROP ") && !drop)
                drop = true;
        }

        return drop;
    }

    public boolean normalChecker(TestNode testNode) {
        // create + insert
        List<String> writeCommands = testNode.writeCommands;
        // It should contain (1) Create table + set (2) INSERT (3) DROP Table

        boolean createTable = false;
        boolean insert = false;

        for (String cmd : writeCommands) {
            if (cmd.contains("CREATE TABLE")
                    && cmd.toLowerCase().contains("set<") && !createTable) {
                createTable = true;
                continue;
            }

            if (createTable && cmd.contains("INSERT") && !insert) {
                insert = true;
                break;
            }
        }

        return insert;
    }

    public static void main(String[] args) {
        new Config();

        boolean isStrict = false;

        if (args.length == 0) {
            // default
        } else if (args.length == 1) {
            if (args[0].equals("strict"))
                isStrict = true;
            else if (args[0].equals("normal"))
                isStrict = false;
            else {
                System.out.println("Error: invalid argument");
                System.exit(1);
            }
        } else {
            System.out.println("Error: invalid argument");
            System.exit(1);
        }

        Analyzer analyzer = new Analyzer(isStrict);
        analyzer.analyze();
    }
}
