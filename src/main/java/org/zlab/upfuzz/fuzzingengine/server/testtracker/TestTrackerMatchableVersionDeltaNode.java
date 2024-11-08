package org.zlab.upfuzz.fuzzingengine.server.testtracker;

import java.util.List;

public class TestTrackerMatchableVersionDeltaNode extends BaseNode {
    private static final long serialVersionUID = 20240407L;

    private Boolean newOriBC = false;
    private Boolean newUpBCAfterUpgrade = false;
    private Boolean newUpBC = false;
    private Boolean newOriBCAfterDowngrade = false;
    private Boolean newOriFC = false;
    private Boolean newUpFC = false;
    private Boolean newOriMFC = false; // Matchable format
    private Boolean newUpMFC = false; // Matchable format

    public TestTrackerMatchableVersionDeltaNode(int nodeId, int pNodeId,
            List<String> writeCommands, List<String> readCommands,
            int configId) {
        super(nodeId, pNodeId, writeCommands, readCommands, configId);
    }

    public void updateCoverage(boolean newOriBC, boolean newUpBCAfterUpgrade,
            boolean newUpBC,
            boolean newOriBCAfterDowngrade, boolean newOriFC, boolean newUpFC,
            boolean newOriMFC, boolean newUpMFC) {
        this.newOriBC = newOriBC;
        this.newUpBCAfterUpgrade = newUpBCAfterUpgrade;
        this.newUpBC = newUpBC;
        this.newOriBCAfterDowngrade = newOriBCAfterDowngrade;
        this.newOriFC = newOriFC;
        this.newUpFC = newUpFC;
        this.newOriMFC = newOriMFC;
        this.newUpMFC = newUpMFC;
    }

    @Override
    public boolean hasNewCoverage() {
        return newOriBC || newUpBC || newOriFC || newUpFC || newUpBCAfterUpgrade
                || newOriBCAfterDowngrade || newOriMFC
                || newUpMFC;
    }

    @Override
    public String printCovInfo() {
        // print them in one line, separated by comma
        return "newOriBC: " + newOriBC + ", " + "newUpBCAfterUpgrade: "
                + newUpBCAfterUpgrade + ", " + "newUpBC: " + newUpBC + ", "
                + "newOriBCAfterDowngrade: " + newOriBCAfterDowngrade + ", "
                + "newOriFC: " + newOriFC + ", " + "newUpFC: " + newUpFC + ", "
                + "newOriMFC: " + newOriMFC + ", "
                + "newUpMFC: " + newUpMFC + ", ";
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
                "newOriFC: " + newOriFC + ", newUpFC: " + newUpFC + "\n" +
                "newOriMFC: " + newOriMFC
                + ", newUpMFC: " + newUpMFC + "\n";

        return coverageInfoBuilder + basicInfo;
    }
}
