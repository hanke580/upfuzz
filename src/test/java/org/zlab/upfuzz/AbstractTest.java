package org.zlab.upfuzz;

import org.junit.jupiter.api.BeforeAll;
import org.zlab.upfuzz.fuzzingengine.Config;

public abstract class AbstractTest {
    @BeforeAll
    static public void initConfig() {
        new Config();
    }
}
