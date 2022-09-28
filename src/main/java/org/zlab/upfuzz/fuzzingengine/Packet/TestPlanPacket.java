package org.zlab.upfuzz.fuzzingengine.Packet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import info.debatty.java.stringsimilarity.Levenshtein;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.fuzzingengine.testplan.TestPlan;
import org.zlab.upfuzz.fuzzingengine.testplan.event.Event;
import org.zlab.upfuzz.fuzzingengine.testplan.event.command.ShellCommand;
import org.zlab.upfuzz.fuzzingengine.testplan.event.fault.*;
import org.zlab.upfuzz.fuzzingengine.testplan.event.upgradeop.UpgradeOp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class TestPlanPacket extends Packet {
    static Logger logger = LogManager.getLogger(TestPlanPacket.class);

    public String systemID;
    public int nodeNum;
    public int testPacketID;
    TestPlan testPlan;

    static RuntimeTypeAdapterFactory<Event> runtimeTypeAdapterFactory;
    static Type listType;
    static Gson gson;

    static {
        runtimeTypeAdapterFactory = RuntimeTypeAdapterFactory
                .of(Event.class, "type")
                .registerSubtype(IsolateFailure.class, "IsolateFailure")
                .registerSubtype(IsolateFailureRecover.class,
                        "IsolateFailureRecover")
                .registerSubtype(LinkFailure.class, "LinkFailure")
                .registerSubtype(LinkFailureRecover.class, "LinkFailureRecover")
                .registerSubtype(NodeFailure.class, "NodeFailure")
                .registerSubtype(NodeFailureRecover.class, "NodeFailureRecover")
                .registerSubtype(PartitionFailure.class, "PartitionFailure")
                .registerSubtype(PartitionFailureRecover.class,
                        "PartitionFailureRecover")
                .registerSubtype(UpgradeOp.class, "UpgradeOp")
                .registerSubtype(ShellCommand.class, "ShellCommand");
        listType = new TypeToken<List<Event>>() {
        }.getType();
        gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapterFactory(runtimeTypeAdapterFactory)
                .create();
    }

    public TestPlanPacket(String systemID, int nodeNum, int testPacketID,
            TestPlan testPlan) {
        this.type = PacketType.TestPlanPacket;

        this.systemID = systemID;
        this.nodeNum = nodeNum;
        this.testPacketID = testPacketID;
        this.testPlan = testPlan;
    }

    public TestPlan getTestPlan() {
        return testPlan;
    }

    public static TestPlanPacket read(DataInputStream in) {
        try {
            int systemIDLen = in.readInt();
            byte[] systemIDBytes = new byte[systemIDLen];
            in.read(systemIDBytes, 0, systemIDLen);
            String systemID = new String(systemIDBytes, StandardCharsets.UTF_8);

            int nodeNum = in.readInt();

            int testPacketId = in.readInt();

            int eventsStrLen = in.readInt();
            byte[] eventsStrBytes = new byte[eventsStrLen];
            int len = 0;
            len = in.read(eventsStrBytes, len, eventsStrLen - len);
            logger.debug("packet length: " + eventsStrLen);
            while (len < eventsStrLen) {
                int size = in.read(eventsStrBytes, len, eventsStrLen - len);
                // logger.debug("packet read extra: " + size);
                len += size;
            }
            String eventsStr = new String(eventsStrBytes,
                    StandardCharsets.UTF_8);

            List<Event> events = gson.fromJson(eventsStr, listType);
            TestPlan testPlan = new TestPlan(events);
            return new TestPlanPacket(systemID, nodeNum, testPacketId,
                    testPlan);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void write(DataOutputStream out) throws IOException {

        out.writeInt(type.value);

        int systemIDLen = systemID.length();
        out.writeInt(systemIDLen);
        out.write(systemID.getBytes(StandardCharsets.UTF_8));

        out.writeInt(nodeNum);

        out.writeInt(testPacketID);

        String eventsStr = gson.toJson(testPlan.getEvents());
        byte[] eventsByte = eventsStr.getBytes(StandardCharsets.UTF_8);
        out.writeInt(eventsByte.length);
        out.write(eventsByte);

    }

}
