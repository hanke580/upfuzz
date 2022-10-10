package org.zlab.upfuzz.hdfs;

import org.junit.jupiter.api.Test;
import org.zlab.upfuzz.CommandPool;
import org.zlab.upfuzz.fuzzingengine.Server.Seed;
import org.zlab.upfuzz.fuzzingengine.executor.Executor;

public class HDFSCommandSequenceTest {

    @Test
    public void test() {
        System.out.println("hh");

        CommandPool hdfsCommandPool = new HdfsCommandPool();
        Class state = HdfsState.class;

        Seed seed = Executor.generateSeed(hdfsCommandPool, state);

        for (String str : seed.originalCommandSequence.getCommandStringList()) {
            System.out.println(str);
        }

        System.out.println("");

        for (String str : seed.validationCommandSequnece
                .getCommandStringList()) {
            System.out.println(str);
        }

    }
}
