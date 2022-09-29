package org.zlab.upfuzz.fuzzingengine.testplan.event;

import com.fasterxml.jackson.annotation.JsonTypeId;
import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.fuzzingengine.Packet.TestPlanPacket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;

/**
 * Event is the base class for all the operations during the upgrade process.
 *      (1) A user/admin command
 *      (2) A fault (Netowrk/Crash)
 *      (3) An upgrade command
 */
public class Event implements Serializable {
    static Logger logger = LogManager.getLogger(Event.class);

    protected String type;

    public Event(String type) {
        this.type = type;
    };

}
