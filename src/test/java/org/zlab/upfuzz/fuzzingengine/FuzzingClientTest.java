package org.zlab.upfuzz.fuzzingengine;

import java.io.FileNotFoundException;
import java.io.FileReader;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.zlab.upfuzz.fuzzingengine.Config.Configuration;
import org.zlab.upfuzz.fuzzingengine.executor.Executor;

public class FuzzingClientTest {
    @BeforeAll
    static public void initAll() {
        String configFile = "./hdfsconfig.json";
        Configuration cfg;
        try {
            cfg = new Gson().fromJson(new FileReader(configFile),
                    Configuration.class);
            Config.setInstance(cfg);
        } catch (JsonSyntaxException | JsonIOException
                | FileNotFoundException e) {
            e.printStackTrace();
            assert false;
        }

    }

    @Test
    public void testJacoco() {
        Executor nullExecutor = new NullExecutor(null, null);
        FuzzingClient fc = new FuzzingClient();
        fc.start(nullExecutor);
    }
}
