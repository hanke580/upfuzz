package org.zlab.upfuzz.ozone;

import org.junit.jupiter.api.Test;
import org.zlab.upfuzz.AbstractTest;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.fuzzingengine.server.Seed;
import org.zlab.upfuzz.utils.STRINGType;

public class CommandSequenceTest extends AbstractTest {
    public static OzoneCommandPool commandPool = new OzoneCommandPool();

    @Test
    public void test() {
        Config.getConf().system = "ozone";
        // Config.getConf().STACKED_TESTS_NUM = 5;
        Seed seed = generateSeed(commandPool, OzoneState.class, -1);
        assert seed != null;
        printSeed(seed);
        // for (int i = 0; i < 20; i++) {
        // boolean status = seed.mutate(commandPool, OzoneState.class);
        // System.out.println("mutate status = " + status);
        // printSeed(seed);
        // }
    }

    @Test
    public void testStringLowerCase() {
        Config.getConf().system = "ozone";
        new Config();

        STRINGType stringType = new STRINGType(true);
        Parameter p = stringType.generateRandomParameter(null, null);
        assert p.value instanceof String;
        assert ((String) p.value).equals(((String) p.value).toLowerCase());
    }

    // @Test
    public void testForNTimes() {
        Config.getConf().system = "ozone";
        Seed seed = generateSeed(commandPool, OzoneState.class, -1);

        System.out.println("debug = " + Config.getConf().debug);

        int N = 10;
        assert seed != null;
        printSeed(seed);
        for (int i = 0; i < 10; i++) {
            boolean status = seed.mutate(commandPool, OzoneState.class);
            System.out.println("mutate status = " + status);
            printSeed(seed);
        }
    }
}
