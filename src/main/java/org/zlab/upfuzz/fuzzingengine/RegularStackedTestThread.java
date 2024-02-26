package org.zlab.upfuzz.fuzzingengine;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.fuzzingengine.packet.*;
import org.zlab.upfuzz.fuzzingengine.executor.Executor;

import static org.zlab.upfuzz.nyx.MiniClientMain.runTheTestsBeforeChangingVersion;
import static org.zlab.upfuzz.nyx.MiniClientMain.changeVersionAndRunTheTests;
import static org.zlab.upfuzz.nyx.MiniClientMain.clearData;

class RegularStackedTestThread implements Callable<StackedFeedbackPacket> {

    static Logger logger = LogManager.getLogger(RegularStackedTestThread.class);

    private StackedFeedbackPacket stackedFeedbackPacket;
    private final Executor executor;
    private final int direction;
    private final StackedTestPacket stackedTestPacket;
    private AtomicInteger decision; // Shared decision variable
    private BlockingQueue<StackedFeedbackPacket> feedbackPacketQueueBeforeVersionChange;

    // If the cluster cannot start up for 3 times, it's serious
    int CLUSTER_START_RETRY = 3; // stop retry for now

    public RegularStackedTestThread(Executor executor, int direction,
            StackedTestPacket stackedTestPacket,
            AtomicInteger decision,
            BlockingQueue<StackedFeedbackPacket> feedbackPacketQueueBeforeVersionChange) {
        this.executor = executor;
        this.direction = direction;
        this.stackedTestPacket = stackedTestPacket;
        this.decision = decision;
        this.feedbackPacketQueueBeforeVersionChange = feedbackPacketQueueBeforeVersionChange;
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
            // StackedFeedbackPacket stackedFeedbackPacket =
            // runTheTests(executor,
            // stackedTestPacket, direction);
            StackedFeedbackPacket stackedFeedbackPacketBeforeVersionChange = runTheTestsBeforeChangingVersion(
                    executor,
                    stackedTestPacket, direction);

            // Send result of running tests before changing version to caller
            feedbackPacketQueueBeforeVersionChange
                    .put(stackedFeedbackPacketBeforeVersionChange);

            // Wait for signal to proceed
            // Wait for decision
            while (true) {
                int decisionValue = decision.get();
                if (decisionValue == 1) {
                    // Proceed with operation 2
                    // Peform version change and continue testing
                    StackedFeedbackPacket stackedFeedbackPacket = changeVersionAndRunTheTests(
                            executor,
                            stackedTestPacket, direction,
                            stackedFeedbackPacketBeforeVersionChange);
                    if (Config.getConf().debug) {
                        logger.info("[Fuzzing Client] completed the testing");
                    }

                    if (Config.getConf().debug) {
                        logger.info(
                                "[Fuzzing Client] Call to teardown executor");
                    }
                    tearDownExecutor();
                    if (Config.getConf().debug) {
                        logger.info("[Fuzzing Client] Executor torn down");
                    }
                    return stackedFeedbackPacket;
                } else if (decisionValue == 2) {
                    // Terminate thread
                    clearData();
                    if (Config.getConf().debug) {
                        logger.info("[Fuzzing Client] completed the testing");
                    }

                    if (Config.getConf().debug) {
                        logger.info(
                                "[Fuzzing Client] Call to teardown executor");
                    }
                    tearDownExecutor();
                    if (Config.getConf().debug) {
                        logger.info("[Fuzzing Client] Executor torn down");
                    }
                    return stackedFeedbackPacketBeforeVersionChange;
                }
                // Wait for a short time before checking again
                Thread.sleep(10);
            }
        }
    }
}
