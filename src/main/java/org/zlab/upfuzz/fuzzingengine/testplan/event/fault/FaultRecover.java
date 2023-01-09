package org.zlab.upfuzz.fuzzingengine.testplan.event.fault;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.fuzzingengine.executor.Executor;
import org.zlab.upfuzz.fuzzingengine.testplan.event.Event;

import java.util.concurrent.TimeUnit;

public abstract class FaultRecover extends Event {
    protected static final Logger logger = LogManager.getLogger(Executor.class);

    public static void waitToRebuildConnection() {
        logger.debug(String.format(
                "wait for %d seconds to rebuild network connection",
                Config.getConf().rebuildConnectionSecs));
        try {
            TimeUnit.SECONDS.sleep(Config.getConf().rebuildConnectionSecs);
        } catch (InterruptedException ignore) {
        }
    }

    public FaultRecover(String type) {
        super(type);
    }
}
