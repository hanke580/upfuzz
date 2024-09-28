package org.zlab.upfuzz.fuzzingengine;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.zlab.upfuzz.AbstractTest;
import org.zlab.upfuzz.CommandPool;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.cassandra.CassandraCommandPool;
import org.zlab.upfuzz.cassandra.CassandraConfigGen;
import org.zlab.upfuzz.cassandra.CassandraState;
import org.zlab.upfuzz.docker.DockerCluster;
import org.zlab.upfuzz.fuzzingengine.packet.TestPlanPacket;
import org.zlab.upfuzz.fuzzingengine.server.*;
import org.zlab.upfuzz.fuzzingengine.configgen.ConfigGen;
import org.zlab.upfuzz.fuzzingengine.executor.Executor;
import org.zlab.upfuzz.fuzzingengine.testplan.TestPlan;
import org.zlab.upfuzz.fuzzingengine.testplan.event.Event;
import org.zlab.upfuzz.fuzzingengine.testplan.event.fault.*;
import org.zlab.upfuzz.fuzzingengine.testplan.event.upgradeop.HDFSStopSNN;
import org.zlab.upfuzz.fuzzingengine.testplan.event.upgradeop.UpgradeOp;
import org.zlab.upfuzz.hdfs.HdfsCommandPool;
import org.zlab.upfuzz.hdfs.HdfsState;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class FuzzingServerTest extends AbstractTest {
    static Logger logger = LogManager.getLogger(FuzzingServerTest.class);

    @Test
    public void testTestPlanGeneration() {
        Config.getConf().system = "hdfs";
        CommandPool commandPool = new HdfsCommandPool();
        Class<? extends State> stateClass = HdfsState.class;
        Seed seed = Seed.generateSeed(commandPool, stateClass, -1, -1);
        FullStopSeed fullStopSeed = new FullStopSeed(
                seed, new LinkedList<>());
        FuzzingServer fuzzingServer = new FuzzingServer();
        TestPlan testPlan;
        while ((testPlan = fuzzingServer
                .generateTestPlan(fullStopSeed)) == null)
            ;
        System.out.println(testPlan);
        testPlan.mutate();
        System.out.println("\nMutate Test Plan\n");
        System.out.println(testPlan);
    }

    // @Test
    public void testTestPlanSerialization() {
        CassandraCommandPool commandPool = new CassandraCommandPool();
        Class stateClass = CassandraState.class;
        Seed seed = Seed.generateSeed(commandPool, stateClass, -1, -1);
        FullStopSeed fullStopSeed = new FullStopSeed(seed, null);
        FuzzingServer fuzzingServer = new FuzzingServer();
        TestPlan testPlan = fuzzingServer.generateTestPlan(fullStopSeed);
        TestPlanPacket testPlanPacket;

        try {
            DataOutputStream os = new DataOutputStream(
                    new FileOutputStream("tmp.txt"));
            ConfigGen configGen = new CassandraConfigGen();

            int configIdx = configGen.generateConfig();
            String configFileName = "test" + configIdx;

            testPlanPacket = new TestPlanPacket("cassandra", 2, configFileName,
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
    public void test1() {
        List<Event> events = new LinkedList<>();

        Config.instance.system = "hdfs";

        events.add(new HDFSStopSNN());
        // events.add(new NodeFailure(2));
        events.add(new IsolateFailure(2));

        events.add(new UpgradeOp(0));
        events.add(new UpgradeOp(1));

        events.add(new NodeFailureRecover(2));

        assert !FuzzingServer.testPlanVerifier(events, 4);
    }

    // @Test
    public void testCorpus() {
        Config.instance.system = "cassandra";
        CassandraCommandPool pool = new CassandraCommandPool();
        Seed seed1 = Seed.generateSeed(pool, CassandraState.class, -1, 1);
        Seed seed2 = Seed.generateSeed(pool, CassandraState.class, -1, 2);

        assert seed1 != null;
        assert seed2 != null;

        Config.instance.formatCoverageChoiceProb = 0;

        CorpusDefault corpus = new CorpusDefault();
        Config.instance.saveCorpusToDisk = false;
        corpus.addSeed(seed1, true, false, false, false, false);
        corpus.addSeed(seed2, true, false, false, false, false);

        assert corpus.getSeed() == seed1;
        assert corpus.getSeed() == seed2;
        assert corpus.getSeed() == seed1;
    }

    // @Test
    public void testFuzzOne() throws InvocationTargetException,
            IllegalAccessException, NoSuchMethodException {
        /**
         * Need to explicitly set the 2.x and 3.x in the prebuild folder
         */
        Config.instance.system = "cassandra";
        Config.instance.originalVersion = "apache-cassandra-2.2.8";
        Config.instance.upgradedVersion = "apache-cassandra-3.0.17";
        Config.instance.testAddedConfig = true;

        CassandraCommandPool pool = new CassandraCommandPool();

        Seed seed1 = Seed.generateSeed(pool, CassandraState.class, 2, 1);

        FuzzingServer fuzzingServer = new FuzzingServer();

        Method initMethod = FuzzingServer.class.getDeclaredMethod("init");
        initMethod.setAccessible(true);
        initMethod.invoke(fuzzingServer);
        Config.instance.saveCorpusToDisk = false;
        fuzzingServer.corpus.addSeed(seed1, true, false, false, false, false);
        fuzzingServer.fuzzOne();
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
