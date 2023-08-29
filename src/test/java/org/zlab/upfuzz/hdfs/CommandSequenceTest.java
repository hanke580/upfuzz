package org.zlab.upfuzz.hdfs;

import org.junit.jupiter.api.Test;
import org.zlab.upfuzz.AbstractTest;
import org.zlab.upfuzz.CommandPool;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.fuzzingengine.server.Seed;
import org.zlab.upfuzz.fuzzingengine.executor.Executor;

public class CommandSequenceTest extends AbstractTest {
    public static HdfsCommandPool hdfsCommandPool = new HdfsCommandPool();

    @Test
    public void test() {
        Config.getConf().system = "hdfs";
        Seed seed = generateSeed(hdfsCommandPool, HdfsState.class, -1);
        assert seed != null;
        printSeed(seed);
        boolean status = seed.mutate(hdfsCommandPool, HdfsState.class);
        System.out.println("mutate status = " + status);
        printSeed(seed);
    }
}
