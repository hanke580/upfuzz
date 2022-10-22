package org.zlab.upfuzz.fuzzingengine;

import org.junit.jupiter.api.Test;
import org.zlab.upfuzz.CommandPool;
import org.zlab.upfuzz.cassandra.CassandraCommandPool;
import org.zlab.upfuzz.cassandra.CassandraState;
import org.zlab.upfuzz.docker.DockerCluster;
import org.zlab.upfuzz.fuzzingengine.packet.TestPlanPacket;
import org.zlab.upfuzz.fuzzingengine.server.FuzzingServer;
import org.zlab.upfuzz.fuzzingengine.server.Seed;
import org.zlab.upfuzz.fuzzingengine.executor.Executor;
import org.zlab.upfuzz.fuzzingengine.testplan.TestPlan;
import org.zlab.upfuzz.fuzzingengine.testplan.event.Event;
import org.zlab.upfuzz.hdfs.HdfsCommandPool;
import org.zlab.upfuzz.hdfs.HdfsState;

import java.io.*;

public class FuzzingServerTest {
    @Test
    public void testTestPlanGeneration() {

        CommandPool commandPool = new HdfsCommandPool();
        Class stateClass = HdfsState.class;
        Seed seed = Executor.generateSeed(commandPool, stateClass);
        Config config = new Config();

        Config.getConf().system = "hdfs";
        FuzzingServer fuzzingServer = new FuzzingServer();
        TestPlan testPlan = fuzzingServer.generateTestPlan(seed);

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
        Config config = new Config();
        FuzzingServer fuzzingServer = new FuzzingServer();
        TestPlan testPlan = fuzzingServer.generateTestPlan(seed);
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

    // @Test
    // public void testGrepState() throws IOException {
    // String stateName = "applicationState";
    // Path filePath = Paths
    // .get("/Users/hanke/Desktop/Project/upfuzz/system.log");
    // String targetStart = String.format("[InconsistencyDetectorStart][%s]",
    // stateName);
    // String cmd = "grep -A 5 \"" + targetStart + "\" " + filePath
    // + " | tail -n 5";
    // System.out.println("cmd = " + cmd);
    //
    // ProcessBuilder processBuilder = new ProcessBuilder("/bin/sh", "-c",
    // cmd); @Test
    //// public void testGrepState() throws IOException {
    //// String stateName = "applicationState";
    //// Path filePath = Paths
    //// .get("/Users/hanke/Desktop/Project/upfuzz/system.log");
    //// String targetStart = String.format("[InconsistencyDetectorStart][%s]",
    //// stateName);
    //// String cmd = "grep -A 5 \"" + targetStart + "\" " + filePath
    //// + " | tail -n 5";
    //// System.out.println("cmd = " + cmd);
    ////
    //// ProcessBuilder processBuilder = new ProcessBuilder("/bin/sh", "-c",
    //// cmd);
    //// Process process = processBuilder.start();
    //// String result = new String(process.getInputStream().readAllBytes());
    ////
    //// int lastIdx = result.lastIndexOf(
    //// "[InconsistencyDetectorStart][applicationState] = ");
    //// String sub = result.substring(lastIdx);
    //// String sub1 = sub.substring(sub.indexOf("\n") + 1,
    //// sub.lastIndexOf("[InconsistencyDetectorEnd]") - 1);
    //// System.out.println("state = " + sub1);
    //// }
    // Process process = processBuilder.start();
    // String result = new String(process.getInputStream().readAllBytes());
    //
    // int lastIdx = result.lastIndexOf(
    // "[InconsistencyDetectorStart][applicationState] = ");
    // String sub = result.substring(lastIdx);
    // String sub1 = sub.substring(sub.indexOf("\n") + 1,
    // sub.lastIndexOf("[InconsistencyDetectorEnd]") - 1);
    // System.out.println("state = " + sub1);
    // }

}
