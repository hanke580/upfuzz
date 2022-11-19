package org.zlab.upfuzz.fuzzingengine;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.plugins.convert.HexConverter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.zlab.upfuzz.CommandPool;
import org.zlab.upfuzz.cassandra.CassandraCommandPool;
import org.zlab.upfuzz.cassandra.CassandraState;
import org.zlab.upfuzz.docker.DockerCluster;
import org.zlab.upfuzz.fuzzingengine.packet.TestPlanPacket;
import org.zlab.upfuzz.fuzzingengine.server.FullStopSeed;
import org.zlab.upfuzz.fuzzingengine.server.FuzzingServer;
import org.zlab.upfuzz.fuzzingengine.server.Seed;
import org.zlab.upfuzz.fuzzingengine.executor.Executor;
import org.zlab.upfuzz.fuzzingengine.testplan.TestPlan;
import org.zlab.upfuzz.fuzzingengine.testplan.event.Event;
import org.zlab.upfuzz.hdfs.HdfsCommandPool;
import org.zlab.upfuzz.hdfs.HdfsState;

import java.io.*;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FuzzingServerTest {
    static Logger logger = LogManager.getLogger(FuzzingServerTest.class);

    @BeforeAll
    public static void prepare() {
        new Config();
    }

    @Test
    public void testTestPlanGeneration() {
        CommandPool commandPool = new HdfsCommandPool();
        Class stateClass = HdfsState.class;
        Seed seed = Executor.generateSeed(commandPool, stateClass);
        FullStopSeed fullStopSeed = new FullStopSeed(
                seed, 3, null, null);
        Config.getConf().system = "hdfs";
        FuzzingServer fuzzingServer = new FuzzingServer();
        TestPlan testPlan = fuzzingServer.generateTestPlan(fullStopSeed);

        System.out.println(testPlan);
        testPlan.mutate();
        System.out.println("\nMutate Test Plan\n");
        System.out.println(testPlan);
    }

    // @Test
    public void testTestPlanSerialization() {
        CassandraCommandPool commandPool = new CassandraCommandPool();
        Class stateClass = CassandraState.class;
        Seed seed = Executor.generateSeed(commandPool, stateClass);
        FullStopSeed fullStopSeed = new FullStopSeed(seed, 3, null, null);
        FuzzingServer fuzzingServer = new FuzzingServer();
        TestPlan testPlan = fuzzingServer.generateTestPlan(fullStopSeed);
        TestPlanPacket testPlanPacket;

        try {
            DataOutputStream os = new DataOutputStream(
                    new FileOutputStream("tmp.txt"));
            testPlanPacket = new TestPlanPacket("cassandra", 2,
                    testPlan);
            testPlanPacket.write(os);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            DataInputStream is = new DataInputStream(
                    new FileInputStream("tmp.txt"));
            is.readInt();
            testPlanPacket = TestPlanPacket.read(is);
            for (Event event : testPlanPacket.getTestPlan().getEvents()) {
                System.out.println(event);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test() {
        System.out.println(DockerCluster.getKthIP("123.2.2.44", 0));
    }

    @Test
    public void testSer() {
        Map<Integer, Map<String, String>> targetSystemStates = new HashMap<>();
        targetSystemStates.put(0, new HashMap<>());
        targetSystemStates.get(0).put("hh", "dd");

        String str = new Gson().toJson(targetSystemStates);
        byte[] strByte = str.getBytes();
        Type t = new TypeToken<Map<Integer, Map<String, String>>>() {
        }.getType();

        Map<Integer, Map<String, String>> deStr = new Gson().fromJson(
                new String(strByte, 0, strByte.length),
                t);

        logger.info(str);
        logger.info(deStr);
    }

//    @Test
//    public void encodeResult() throws IOException {
//        // grep -a "HKLOG"
//        String target = "\\[InconsistencyDetector\\]\\[org.apache.cassandra.gms.EndpointState.applicationState\\]";
//        String[] cmd = new String[] {
//                "/bin/bash", "-c", "grep -an " + target + " /Users/hanke/Desktop/Project/upfuzz/system.log | tail -n 1"
//        };
//        ProcessBuilder pb = new ProcessBuilder(cmd);
//        Process p = pb.start();
//        String res = new String(
//                p.getInputStream().readAllBytes()
//        );
//
//        String value = res.substring(res.indexOf("=") + 1);
//        logger.info("value1 = " + value);
//        String hexV = encode(value);
//        logger.info("hexV = " + hexV);
//        String value_back = decode(hexV);
//        logger.info("value2 = " + value_back);
//    }

}
