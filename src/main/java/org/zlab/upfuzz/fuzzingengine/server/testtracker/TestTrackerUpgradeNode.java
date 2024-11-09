package org.zlab.upfuzz.fuzzingengine.server.testtracker;

import java.util.List;

public class TestTrackerUpgradeNode extends BaseNode {
    private static final long serialVersionUID = 20240407L;

    private boolean oriBC = false; // BC before upgrade
    private boolean upBC = false; // BC after upgrade
    private boolean FC = false;
    private boolean FC_MOD = false;

    public TestTrackerUpgradeNode(int nodeId, int pNodeId,
            List<String> writeCommands,
            List<String> readCommands, int configId) {
        super(nodeId, pNodeId, writeCommands, readCommands, configId);
    }

    @Override
    public boolean hasNewCoverage() {
        return oriBC || upBC || FC || FC_MOD;
    }

    @Override
    public String printCovInfo() {
        return "oriBC: " + oriBC + ", " + "upBC: "
                + upBC + ", " + "FC: " + FC + ", " + "FC_MOD: "
                + FC_MOD;
    }

    public void updateCoverage(boolean newOldVersionBranchCoverage,
            boolean newNewVersionBranchCoverage,
            boolean newFormatCoverage, boolean FC_MOD) {
        this.oriBC = newOldVersionBranchCoverage;
        this.upBC = newNewVersionBranchCoverage;
        this.FC = newFormatCoverage;
        this.FC_MOD = FC_MOD;
    }

    @Override
    public String toString() {
        String basicInfo = printAsString();
        String coverageInfoBuilder = "oriBC: " + oriBC + "\n" + "upBC: " + upBC
                + "\n" + "FC: " + FC + "\n" + "FC_MOD: " + FC_MOD + "\n";
        return coverageInfoBuilder + basicInfo;
    }
}
