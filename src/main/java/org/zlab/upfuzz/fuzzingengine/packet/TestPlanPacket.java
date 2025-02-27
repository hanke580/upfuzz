package org.zlab.upfuzz.fuzzingengine.packet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.fuzzingengine.testplan.TestPlan;
import org.zlab.upfuzz.fuzzingengine.testplan.event.Event;
import org.zlab.upfuzz.fuzzingengine.testplan.event.command.NodetoolCommand;
import org.zlab.upfuzz.fuzzingengine.testplan.event.command.ShellCommand;
import org.zlab.upfuzz.fuzzingengine.testplan.event.downgradeop.DowngradeOp;
import org.zlab.upfuzz.fuzzingengine.testplan.event.fault.*;
import org.zlab.upfuzz.fuzzingengine.testplan.event.upgradeop.FinalizeUpgrade;
import org.zlab.upfuzz.fuzzingengine.testplan.event.upgradeop.HDFSStopSNN;
import org.zlab.upfuzz.fuzzingengine.testplan.event.upgradeop.PrepareUpgrade;
import org.zlab.upfuzz.fuzzingengine.testplan.event.upgradeop.UpgradeOp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.io.Serializable;

public class TestPlanPacket extends Packet implements Serializable {
    static Logger logger = LogManager.getLogger(TestPlanPacket.class);

    public String systemID;
    public int testPacketID;
    public String configFileName;
    private final TestPlan testPlan;

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
                .registerSubtype(RestartFailure.class, "RestartFailure")
                .registerSubtype(UpgradeOp.class, "UpgradeOp")
                .registerSubtype(DowngradeOp.class, "DowngradeOp")
                .registerSubtype(PrepareUpgrade.class, "PrepareUpgrade")
                .registerSubtype(FinalizeUpgrade.class, "FinalizeUpgrade")
                .registerSubtype(HDFSStopSNN.class, "HDFSStopSNN")
                .registerSubtype(NodetoolCommand.class, "NodetoolCommand")
                .registerSubtype(ShellCommand.class, "ShellCommand");
        listType = new TypeToken<List<Event>>() {
        }.getType();
        gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapterFactory(runtimeTypeAdapterFactory)
                .create();
    }

    public TestPlanPacket(String systemID, int testPacketID,
            String configFileName,
            TestPlan testPlan) {
        this.type = PacketType.TestPlanPacket;

        this.systemID = systemID;
        this.testPacketID = testPacketID;
        this.configFileName = configFileName;
        this.testPlan = testPlan;
    }

    public TestPlan getTestPlan() {
        return testPlan;
    }

    public int getNodeNum() {
        return testPlan.nodeNum;
    }

    public static TestPlanPacket read(DataInputStream in) {
        try {
            String systemID = readString(in);

            int testPacketId = in.readInt();

            String configFileName = readString(in);

            int nodeNum = in.readInt();

            // validationCommands
            String validationCommandsStr = readString(in);
            Type t3 = new TypeToken<List<String>>() {
            }.getType();
            List<String> validationCommands = gson
                    .fromJson(validationCommandsStr, t3);

            // validationReadResultsOracle
            String validationReadResultsOracleStr = readString(in);
            Type t4 = new TypeToken<List<String>>() {
            }.getType();
            List<String> validationReadResultsOracle = gson
                    .fromJson(validationReadResultsOracleStr, t4);

            // events
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
            TestPlan testPlan = new TestPlan(nodeNum, events,
                    validationCommands, validationReadResultsOracle);
            return new TestPlanPacket(systemID, testPacketId, configFileName,
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

        out.writeInt(testPacketID);

        int configFileNameLen = configFileName.length();
        out.writeInt(configFileNameLen);
        out.write(configFileName.getBytes(StandardCharsets.UTF_8));

        // test plan
        out.writeInt(testPlan.nodeNum);

        String validationCommandsStr = new Gson()
                .toJson(testPlan.validationCommands);
        int validationCommandsStrLen = validationCommandsStr.length();
        out.writeInt(validationCommandsStrLen);
        out.write(validationCommandsStr.getBytes(StandardCharsets.UTF_8));

        String validationReadResultsOracleStr = new Gson()
                .toJson(testPlan.validationReadResultsOracle);
        int validationReadResultsOracleStrLen = validationReadResultsOracleStr
                .length();
        out.writeInt(validationReadResultsOracleStrLen);
        out.write(validationReadResultsOracleStr
                .getBytes(StandardCharsets.UTF_8));

        String eventsStr = gson.toJson(testPlan.getEvents());
        byte[] eventsByte = eventsStr.getBytes(StandardCharsets.UTF_8);
        out.writeInt(eventsByte.length);
        out.write(eventsByte);

    }

    public static String readString(DataInputStream in) throws IOException {
        int len = in.readInt();
        byte[] bytes = new byte[len];
        in.read(bytes, 0, len);
        return new String(bytes, StandardCharsets.UTF_8);
    }

}
