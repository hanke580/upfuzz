package org.zlab.upfuzz.fuzzingengine.server.testtracker;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.util.List;

public class TestTrackerVersionDeltaNode extends BaseNode {
    private static final long serialVersionUID = 20240407L;

    private boolean newOriBC;
    private boolean newUpBCAfterUpgrade;
    private boolean newUpBC;
    private boolean newOriBCAfterDowngrade;
    private boolean newOriFC;
    private boolean newUpFC;

    public TestTrackerVersionDeltaNode(int nodeId, int pNodeId,
            List<String> writeCommands,
            List<String> readCommands, int configId) {
        super(nodeId, pNodeId, writeCommands, readCommands, configId);
    }

    @Override
    public boolean hasNewCoverage() {
        return newOriBC || newUpBCAfterUpgrade || newUpBC
                || newOriBCAfterDowngrade || newOriFC || newUpFC;
    }

    @Override
    public String printCovInfo() {
        // print them in one line, separated by comma
        // newOriBC = xxx, xxx
        return "newOriBC: " + newOriBC + ", " + "newUpBCAfterUpgrade: "
                + newUpBCAfterUpgrade + ", " +
                "newUpBC: " + newUpBC + ", " + "newOriBCAfterDowngrade: "
                + newOriBCAfterDowngrade + ", " +
                "newOriFC: " + newOriFC + ", " + "newUpF: " + newUpFC + ", ";
    }

    public void updateCoverage(boolean newOriBC, boolean newUpBCAfterUpgrade,
            boolean newUpBC,
            boolean newOriBCAfterDowngrade, boolean newOriFC, boolean newUpF) {
        this.newOriBC = newOriBC;
        this.newUpBCAfterUpgrade = newUpBCAfterUpgrade;
        this.newUpBC = newUpBC;
        this.newOriBCAfterDowngrade = newOriBCAfterDowngrade;
        this.newOriFC = newOriFC;
        this.newUpFC = newUpF;
    }

    @Override
    public String toString() {
        String basicInfo = printAsString();
        StringBuilder coverageInfoBuilder = new StringBuilder();
        coverageInfoBuilder.append(
                "newOriBC: " + newOriBC + ", newUpBCAfterUpgrade: "
                        + newUpBCAfterUpgrade
                        + "\n");
        coverageInfoBuilder.append(
                "newUpBC: " + newUpBC + ", newOriBCAfterDowngrade: "
                        + newOriBCAfterDowngrade
                        + "\n");
        coverageInfoBuilder.append(
                "newOriFC: " + newOriFC + ", newUpF: " + newUpFC + "\n");
        return coverageInfoBuilder.toString() + basicInfo;
    }

    public static void serializeNodeToDisk(Path filePath,
            TestTrackerUpgradeNode node)
            throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(filePath.toFile()))) {
            oos.writeObject(node);
        }
    }
}
