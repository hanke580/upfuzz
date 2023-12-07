package org.zlab.upfuzz.fuzzingengine.testplan.event;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;

/**
 * Event is the base class for all the operations during the upgrade process.
 *      (1) A user/admin command
 *      (2) A fault (Network/Crash)
 *      (3) An upgrade command
 */
public class Event implements Serializable {
    static Logger logger = LogManager.getLogger(Event.class);

    protected String type;

    public Event(String type) {
        this.type = type;
    };

}
