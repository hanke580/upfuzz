package org.zlab.upfuzz.hdfs;

import org.junit.jupiter.api.Test;
import org.zlab.upfuzz.AbstractTest;
import org.zlab.upfuzz.CommandPool;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.fuzzingengine.server.Seed;
import org.zlab.upfuzz.fuzzingengine.executor.Executor;

public class HDFSCommandSequenceTest extends AbstractTest {

    @Test
    public void test() {
        new Config();
        Config.getConf().system = "hdfs";

        CommandPool hdfsCommandPool = new HdfsCommandPool();
        Class<HdfsState> stateClass = HdfsState.class;

        for (int i = 0; i < 2; i++) {
            Seed seed = Executor.generateSeed(hdfsCommandPool, stateClass, -1);
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

            boolean status = seed.mutate(hdfsCommandPool, stateClass);
            System.out.println("state = " + status);

            for (String str : seed.originalCommandSequence
                    .getCommandStringList()) {
                System.out.println(str);
            }
        }

    }
}
