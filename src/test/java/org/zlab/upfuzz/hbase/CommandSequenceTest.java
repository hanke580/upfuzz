package org.zlab.upfuzz.hbase;

import org.junit.jupiter.api.Test;
import org.zlab.upfuzz.AbstractTest;
import org.zlab.upfuzz.CommandPool;
import org.zlab.upfuzz.CommandSequence;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.fuzzingengine.executor.Executor;
import org.zlab.upfuzz.fuzzingengine.server.Seed;
import org.zlab.upfuzz.hdfs.HdfsCommandPool;
import org.zlab.upfuzz.hdfs.HdfsState;

public class CommandSequenceTest extends AbstractTest {
    @Test
    public void testSequenceGeneration() {
        new Config();
        Config.getConf().system = "hbase";

        CommandPool hbaseCommandPool = new HBaseCommandPool();
        Class<HBaseState> stateClass = HBaseState.class;

        Seed seed = Executor.generateSeed(hbaseCommandPool, stateClass, -1);
        assert seed != null;
        System.out.println("write commands");
        for (String str : seed.originalCommandSequence
                .getCommandStringList()) {
            System.out.println(str);
        }

        System.out.println();
        System.out.println("read commands");
        for (String str : seed.validationCommandSequence
                .getCommandStringList()) {
            System.out.println(str);
        }

        System.out.println("\n\n");

        boolean status = seed.mutate(hbaseCommandPool, stateClass);
        System.out.println("state = " + status);

        for (String str : seed.originalCommandSequence
                .getCommandStringList()) {
            System.out.println(str);
        }

    }
}
