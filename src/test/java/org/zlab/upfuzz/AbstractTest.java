package org.zlab.upfuzz;

import org.junit.jupiter.api.BeforeAll;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.fuzzingengine.executor.Executor;
import org.zlab.upfuzz.fuzzingengine.server.Seed;

public abstract class AbstractTest {
    @BeforeAll
    public static void initConfig() {
        new Config();
    }

    public static Seed generateSeed(CommandPool commandPool,
            Class<? extends State> stateClass, int configIdx) {
        return Seed.generateSeed(commandPool, stateClass, configIdx, -1);
    }

    public static void printSeed(Seed seed) {
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
    }
}
