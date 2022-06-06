package org.zlab.upfuzz.fuzzingengine;

import java.util.Comparator;
import java.util.List;

import org.zlab.upfuzz.CommandSequence;

public class TestPacket {
    // Write commands
    public CommandSequence originalCommandSequence;
    public CommandSequence upgradedCommandSequence;

    public List<String> originalCommandSequenceList;
    public List<String> upgradeCommandSequenceList;

    // Read Commands
    public CommandSequence validationCommandSequnece;

    public String testPacketID;
    public FeedBack feedBack;
    public String systemID;

    public void serializeTo(String pathString) {
    }

    public void deserializeFrom(String pathString) {
    }

    public int calcInterests() {
        // TODO
        int res = 0;
        return res;
    }

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
