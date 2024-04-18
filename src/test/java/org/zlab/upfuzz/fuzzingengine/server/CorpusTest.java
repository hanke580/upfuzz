package org.zlab.upfuzz.fuzzingengine.server;

import org.junit.jupiter.api.Test;
import org.zlab.upfuzz.fuzzingengine.Config;

public class CorpusTest {
    // @Test
    public void testCorpusLoad() {
        new Config();
        Config.instance.system = "cassandra";
        CorpusDefault corpusDefault = new CorpusDefault();
        corpusDefault.initCorpus();
        corpusDefault.printInfo();
    }
}
