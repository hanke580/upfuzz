package org.zlab.upfuzz.fuzzingengine.server.testtracker;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.util.List;

public class TestTrackerVersionDeltaNode extends BaseNode {
    private static final long serialVersionUID = 20240407L;

    private Boolean newOriBC = null;
    private Boolean newUpBCAfterUpgrade = null;
    private Boolean newUpBC = null;
    private Boolean newOriBCAfterDowngrade = null;
    private Boolean newOriFC = null;
    private Boolean newUpFC = null;

    public TestTrackerVersionDeltaNode(int nodeId, int pNodeId,
            List<String> writeCommands,
            List<String> readCommands, int configId) {
        super(nodeId, pNodeId, writeCommands, readCommands, configId);
    }

    @Override
    public boolean hasNewCoverage() {
        if (newUpBCAfterUpgrade != null && newOriBCAfterDowngrade != null) {
            return newOriBC || newUpBC || newOriFC || newUpFC
                    || newUpBCAfterUpgrade || newOriBCAfterDowngrade;
        }
        return newOriBC || newUpBC || newOriFC || newUpFC;
    }

    @Override
    public String printCovInfo() {
        // print them in one line, separated by comma
        // newOriBC = xxx, xxx
        return "newOriBC: " + newOriBC + ", " + "newUpBCAfterUpgrade: "
                + newUpBCAfterUpgrade + ", " +
                "newUpBC: " + newUpBC + ", " + "newOriBCAfterDowngrade: "
                + newOriBCAfterDowngrade + ", " +
                "newOriFC: " + newOriFC + ", " + "newUpFC: " + newUpFC + ", ";
    }

    public void updateCoverage(boolean newOriBC, boolean newUpBCAfterUpgrade,
            boolean newUpBC,
            boolean newOriBCAfterDowngrade, boolean newOriFC, boolean newUpFC) {
        this.newOriBC = newOriBC;
        this.newUpBCAfterUpgrade = newUpBCAfterUpgrade;
        this.newUpBC = newUpBC;
        this.newOriBCAfterDowngrade = newOriBCAfterDowngrade;
        this.newOriFC = newOriFC;
        this.newUpFC = newUpFC;
    }

    public void updateCoverageGroup1(boolean newOriBC,
            boolean newUpBC,
            boolean newOriFC, boolean newUpFC) {
        this.newOriBC = newOriBC;
        this.newUpBC = newUpBC;
        this.newOriFC = newOriFC;
        this.newUpFC = newUpFC;
    }

    public void updateCoverageGroup2(boolean newUpBCAfterUpgrade,
            boolean newOriBCAfterDowngrade) {
        this.newUpBCAfterUpgrade = newUpBCAfterUpgrade;
        this.newOriBCAfterDowngrade = newOriBCAfterDowngrade;
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
                "newOriFC: " + newOriFC + ", newUpFC: " + newUpFC + "\n");
        return coverageInfoBuilder.toString() + basicInfo;
    }
}
