package org.zlab.upfuzz.fuzzingengine.server.testtracker;

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
        String coverageInfoBuilder = "newOriBC: " + newOriBC
                + ", newUpBCAfterUpgrade: "
                + newUpBCAfterUpgrade
                + "\n" +
                "newUpBC: " + newUpBC + ", newOriBCAfterDowngrade: "
                + newOriBCAfterDowngrade
                + "\n" +
                "newOriFC: " + newOriFC + ", newUpFC: " + newUpFC + "\n";
        return coverageInfoBuilder + basicInfo;
    }
}
