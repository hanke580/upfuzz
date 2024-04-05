package org.zlab.upfuzz.fuzzingengine.server.testtracker;

import java.io.*;
import java.nio.file.Path;
import java.util.List;

public class TestTrackerUpgradeNode extends BaseNode {
    private boolean newOriBC = false;
    private boolean newUpBC = false;
    private boolean newFC = false;

    public TestTrackerUpgradeNode(int nodeId, int pNodeId,
            List<String> writeCommands,
            List<String> readCommands, int configId) {
        super(nodeId, pNodeId, writeCommands, readCommands, configId);
    }

    @Override
    public boolean hasNewCoverage() {
        return newOriBC || newUpBC || newFC;
    }

    @Override
    public String printCovInfo() {
        return "newOriBC: " + newOriBC + ", " + "newUpBC: "
                + newUpBC + ", " + "newFC: " + newFC + ", ";
    }

    public void updateCoverage(boolean newOldVersionBranchCoverage,
            boolean newNewVersionBranchCoverage,
            boolean newFormatCoverage) {
        this.newOriBC = newOldVersionBranchCoverage;
        this.newUpBC = newNewVersionBranchCoverage;
        this.newFC = newFormatCoverage;
    }

    @Override
    public String toString() {
        String basicInfo = printAsString();

        StringBuilder coverageInfoBuilder = new StringBuilder();
        coverageInfoBuilder.append(
                "newOldVersionBranchCoverage: " + newOriBC
                        + "\n");
        coverageInfoBuilder.append(
                "newNewVersionBranchCoverage: " + newUpBC
                        + "\n");
        coverageInfoBuilder
                .append("newFormatCoverage: " + newFC + "\n");
        return coverageInfoBuilder.toString() + basicInfo;
    }
}
