package org.zlab.upfuzz.fuzzingengine.server.testanalyzer;

import org.zlab.upfuzz.fuzzingengine.Config;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class Analyzer {

    public int state = 0;

    public Analyzer(int state) {
        this.state = state;
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
        if (node.baseNode.hasNewCoverage()) {
            // Only print the new coverage nodes
            boolean check = false;
            switch (state) {
            case 0:
                check = checker0(node);
                break;
            case 1:
                check = checker1(node);
                break;
            case 2:
                check = checker2(node);
                break;
            }
            writer.write(prefix + node.baseNode.nodeId + ": "
                    + node.baseNode.printCovInfo()
                    + ", checker = " + check + "\n");
        }

        List<TestNode> children = node.getChildren();
        if (children != null && !children.isEmpty()) {
            for (TestNode child : children) {
                traverseNode(child, prefix + "  ", writer);
            }
        }
    }

    public boolean checker0(TestNode testNode) {
        // create + insert
        List<String> writeCommands = testNode.baseNode.writeCommands;
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

    public boolean checker1(TestNode testNode) {
        List<String> writeCommands = testNode.baseNode.writeCommands;
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

    public boolean checker2(TestNode testNode) {
        List<String> writeCommands = testNode.baseNode.writeCommands;
        // It should contain (1) Create table + set (2) INSERT (3) DROP Table
        // (4) Add another column

        boolean createTable = false;
        boolean insert = false;
        boolean drop = false;
        boolean addColumn = false;

        for (String cmd : writeCommands) {
            if (cmd.contains("CREATE TABLE")
                    && cmd.toLowerCase().contains("set<") && !createTable)
                createTable = true;

            if (createTable && cmd.contains("INSERT") && !insert)
                insert = true;

            if (insert && cmd.contains("ALTER TABLE") && cmd.contains(" DROP ")
                    && !drop)
                drop = true;

            if (drop && cmd.contains("ALTER TABLE") && cmd.contains(" ADD ")
                    && !addColumn)
                addColumn = true;
        }

        return addColumn;
    }

    public static void main(String[] args) {
        new Config();

        assert args.length == 1
                : "Error: invalid argument, use strict or normal";
        int state = 0;
        if (args[0].equals("s0"))
            state = 0;
        else if (args[0].equals("s1"))
            state = 1;
        else if (args[0].equals("s2")) {
            state = 2;
        } else {
            System.out.println("Error: invalid argument");
            System.exit(1);
        }
        Analyzer analyzer = new Analyzer(state);
        analyzer.analyze();
    }
}
