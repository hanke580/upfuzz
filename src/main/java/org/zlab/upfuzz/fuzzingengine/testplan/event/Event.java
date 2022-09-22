package org.zlab.upfuzz.fuzzingengine.testplan.event;

/**
 * Event is the base class for all the operations during the upgrade process.
 *      (1) A user/admin command
 *      (2) A fault (Netowrk/Crash)
 *      (3) An upgrade command
 */
public class Event {
    int type;

    public Event() {
    };

}
