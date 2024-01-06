package org.zlab.upfuzz.fuzzingengine.server.testtracker;

import org.zlab.upfuzz.fuzzingengine.Config;

import java.io.*;
import java.nio.file.Path;
import java.util.List;

public class TestTrackerNode implements Serializable {
    public int nodeId;
    public int pNodeId;
    public List<String> writeCommands;
    public List<String> readCommands;
    public int configId;
    // keep full coverage
    public boolean newOldVersionBranchCoverage = false;
    public boolean newNewVersionBranchCoverage = false;
    public boolean newFormatCoverage = false;

    public TestTrackerNode(int nodeId, int pNodeId, List<String> writeCommands,
            List<String> readCommands, int configId) {
        this.nodeId = nodeId;
        this.pNodeId = pNodeId;
        this.writeCommands = writeCommands;
        this.readCommands = readCommands;
        this.configId = configId;
    }

    public String toString() {
        // print everything
        StringBuilder sb = new StringBuilder();
        sb.append("nodeId: " + nodeId + "\n");
        sb.append("pNodeId: " + pNodeId + "\n");
        // coverage
        sb.append("newOldVersionBranchCoverage: " + newOldVersionBranchCoverage
                + "\n");
        sb.append("newNewVersionBranchCoverage: " + newNewVersionBranchCoverage
                + "\n");
        sb.append("newFormatCoverage: " + newFormatCoverage + "\n");
        sb.append("configId: " + configId + "\n");
        sb.append("writeCommands: size = " + writeCommands.size() + "\n");
        for (String cmd : writeCommands) {
            sb.append("\t" + cmd + "\n");
        }
        sb.append("readCommands: size = " + readCommands.size() + "\n");
        for (String cmd : readCommands) {
            sb.append("\t" + cmd + "\n");
        }
        return sb.toString();
    }

    public static void serializeNodeToDisk(Path filePath, TestTrackerNode node)
            throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(filePath.toFile()))) {
            oos.writeObject(node);
        }
    }

    public static TestTrackerNode deserializeNodeFromDisk(String filename)
            throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(filename))) {
            return (TestTrackerNode) ois.readObject();
        }
    }

    public static TestTrackerNode deserializeNodeFromDisk(File file)
            throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(file))) {
            return (TestTrackerNode) ois.readObject();
        }
    }

    public static void printNodeFromFile(File file) {
        try {
            TestTrackerNode testTrackerNode = deserializeNodeFromDisk(file);
            System.out.println(testTrackerNode);
        } catch (Exception e) {
            System.out.println("Error: " + e);
        }
    }

    public static void main(String[] args) {
        // runs separately
        // args contains the filename
        new Config();
        assert args.length == 1;
        System.out.println("filename: " + args[0]);

        // Find subdir
        Path subDirPath = TestTrackerGraph
                .getSubDirPath(Integer.parseInt(args[0].split("\\.")[0]));
        Path filePath = subDirPath
                .resolve(args[0]);
        printNodeFromFile(filePath.toFile());
    }

}
