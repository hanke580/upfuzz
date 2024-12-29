package org.zlab.upfuzz.ozone;

import org.junit.jupiter.api.Test;
import org.zlab.upfuzz.AbstractTest;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.fuzzingengine.server.Seed;

public class CommandSequenceTest extends AbstractTest {
    public static OzoneCommandPool commandPool = new OzoneCommandPool();

    @Test
    public void test() {
        Config.getConf().system = "ozone";
        Seed seed = generateSeed(commandPool, OzoneState.class, -1);
        assert seed != null;
        printSeed(seed);
        boolean status = seed.mutate(commandPool, OzoneState.class);
        System.out.println("mutate status = " + status);
        printSeed(seed);
    }
}
