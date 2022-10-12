package org.zlab.upfuzz.fuzzingengine.testplan.event.upgradeop;

import org.zlab.upfuzz.fuzzingengine.testplan.event.Event;

public class HDFSStopSNN extends Event {
    public HDFSStopSNN() {
        super("HDFSStopSNN");
    }

    @Override
    public String toString() {
        return "[HDFS_Specific] Shutdown secondary namenode";
    }
}
