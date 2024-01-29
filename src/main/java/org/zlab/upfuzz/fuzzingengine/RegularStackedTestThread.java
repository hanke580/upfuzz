package org.zlab.upfuzz.fuzzingengine;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jacoco.core.data.ExecutionDataStore;
import org.zlab.upfuzz.cassandra.CassandraExecutor;
import org.zlab.upfuzz.fuzzingengine.packet.*;
import org.zlab.upfuzz.fuzzingengine.packet.Packet.PacketType;
import org.zlab.upfuzz.fuzzingengine.executor.Executor;
import org.zlab.upfuzz.fuzzingengine.configgen.ConfigGen;
import org.zlab.upfuzz.hdfs.HdfsExecutor;
import org.zlab.upfuzz.hbase.HBaseExecutor;
import org.zlab.upfuzz.utils.Pair;
import org.zlab.upfuzz.utils.Utilities;
import org.zlab.upfuzz.nyx.LibnyxInterface;

import static org.zlab.upfuzz.fuzzingengine.server.FuzzingServer.readState;
import static org.zlab.upfuzz.nyx.MiniClientMain.runTheTests;
import static org.zlab.upfuzz.nyx.MiniClientMain.setTestType;

class RegularStackedTestThread implements Callable<StackedFeedbackPacket> {

    static Logger logger = LogManager.getLogger(RegularStackedTestThread.class);

    private StackedFeedbackPacket stackedFeedbackPacket;
    private final Executor executor;
    private final int direction;
    private final StackedTestPacket stackedTestPacket;

    // If the cluster cannot start up for 3 times, it's serious
    int CLUSTER_START_RETRY = 3; // stop retry for now

    public RegularStackedTestThread(Executor executor, int direction,
            StackedTestPacket stackedTestPacket) {
        this.executor = executor;
        this.direction = direction;
        this.stackedTestPacket = stackedTestPacket;
    }

    public boolean startUpExecutor() {
        logger.info(
                "[HKLOG] Fuzzing client: starting up executor: " + direction);
        for (int i = 0; i < CLUSTER_START_RETRY; i++) {
            try {
                if (executor.startup()) {
                    if (Config.getConf().debug) {
                        logger.info(
                                "[Fuzzing Client] started up executor after trial "
                                        + i);
                    }
                    return true;
                }
            } catch (Exception e) {
                logger.error("An error occurred", e);
            }
            executor.teardown();
        }
        logger.error("original version cluster cannot start up");
        return false;
    }

    public StackedFeedbackPacket getStackedFeedbackPacket() {
        return stackedFeedbackPacket;
    }

    public void tearDownExecutor() {
        executor.upgradeTeardown();
        executor.clearState();
        executor.teardown();
    }

    @Override
    public StackedFeedbackPacket call() throws Exception {

        if (Config.getConf().debug) {
            logger.info("[Fuzzing Client] Call to start up executor: "
                    + (direction == 0 ? "upgrade" : "downgrade"));
        }
        boolean startUpStatus = startUpExecutor();
        if (!startUpStatus) {
            // old version **cluster** start up problem, this won't be upgrade
            // bugs
            logger.info("[Fuzzing Client] Cluster startup problem: "
                    + (direction == 0 ? "upgrade" : "downgrade"));
            stackedFeedbackPacket = null;
            return null;
        } else {
            if (Config.getConf().debug) {
                logger.info("[Fuzzing Client] started up executor");
            }

            if (Config.getConf().startUpClusterForDebugging) {
                logger.info("[Debugging Mode] Start up the cluster only");
                try {
                    Thread.sleep(36000 * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                logger.info("[Debugging Mode] System exit");
                System.exit(1);
            }

            if (Config.getConf().debug) {
                logger.info("[Fuzzing Client] Call to run the tests");
            }
            StackedFeedbackPacket stackedFeedbackPacket = runTheTests(executor,
                    stackedTestPacket, direction);
            if (Config.getConf().debug) {
                logger.info("[Fuzzing Client] completed the testing");
            }

            if (Config.getConf().debug) {
                logger.info("[Fuzzing Client] Call to teardown executor");
            }
            tearDownExecutor();
            if (Config.getConf().debug) {
                logger.info("[Fuzzing Client] Executor torn down");
            }
            return stackedFeedbackPacket;
        }
    }
}
