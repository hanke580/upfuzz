package org.zlab.upfuzz.fuzzingengine.packet;

import java.util.Comparator;
import java.util.List;

public class TestPacket {

    public String systemID;
    public int testPacketID;
    // Write Commands
    public List<String> originalCommandSequenceList;
    public List<String> upgradeCommandSequenceList;
    // Read Commands
    public List<String> validationCommandSequenceList;

    public TestPacket(String systemID, int testPacketID,
            List<String> originalCommandSequenceList,
            List<String> upgradeCommandSequenceList,
            List<String> validationCommandSequenceList) {
        this.systemID = systemID;
        this.testPacketID = testPacketID;
        this.originalCommandSequenceList = originalCommandSequenceList;
        this.upgradeCommandSequenceList = upgradeCommandSequenceList;
        this.validationCommandSequenceList = validationCommandSequenceList;
    }

    public void serializeTo(String pathString) {
    }

    public void deserializeFrom(String pathString) {
    }

    public int calcInterests() {
        return 0;
    } // TODO

    public static class TestPacketComparator implements Comparator<TestPacket> {

        public int compare(TestPacket tp1, TestPacket tp2) {
            int v1 = tp1.calcInterests(), v2 = tp2.calcInterests();
            if (v1 == v2) {
                return 0;
            } else if (v1 < v2) {
                return -1;
            } else {
                return 1;
            }
        }
    }
}
