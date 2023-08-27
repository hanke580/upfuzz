package org.zlab.upfuzz.fuzzingengine;

import com.google.gson.GsonBuilder;
import java.lang.reflect.Field;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Config {

    static Logger logger = LogManager.getLogger(Config.class);

    public static Configuration instance;

    public static Configuration getConf() {
        return instance;
    }

    public Config() {
        instance = new Configuration();
    }

    public static class Configuration {
        public String serverHost = "localhost";
        public Integer serverPort = 6299;
        public String clientHost = "localhost";
        public Integer clientPort = 6300;
        public String instClassFilePath = null;

        public String originalVersion = null;
        public String upgradedVersion = null;
        public String depVersion = null;

        public String jacocoAgentPath = null;
        public String system = null;
        public String depSystem = null;
        public String initSeedDir = null;

        public String failureDir = null;
        public String corpusDir = null;

        public boolean nyxMode = false;
        public String nyxFuzzSH = null;

        // Mutation
        // for the first 10 seeds added to the corpus,
        // we only mutate them for relative few times
        public int firstMutationSeedLimit = 5;
        public int firstSequenceMutationEpoch = 10;
        public int sequenceMutationEpoch = 200;
        public int firstConfigMutationEpoch = 3;
        public int configMutationEpoch = 20;

        // violent mutation, usually fewer
        public int bothMutationEpoch = 20;
        public int testPlanMutationEpoch = 20;
        public int testPlanMutationRetry = 50;
        // Given a full-stop seed, we generate 20
        // test plan from it.
        public int testPlanGenerationNum = 20;

        public String targetSystemStateFile = "states.json";

        public int STACKED_TESTS_NUM = 50;
        public long timeInterval = 600; // seconds, record time
        public boolean keepDir = true; // set to false if start a long running
                                       // test
        public int nodeNum = 3;

        // ------------FeedBack------------
        public boolean useCodeCoverage = true;
        public boolean useLikelyInv = false;
        public boolean collUpFeedBack = true;

        // ------------Fault Injection-------------
        public boolean shuffleUpgradeOrder = false; // Whether shuffle the
                                                    // upgrade order
        public int faultMaxNum = 2; // disable faults for now
        public boolean alwaysRecoverFault = false;
        public float noRecoverProb = 0.5f;

        public int rebuildConnectionSecs = 5;

        // ------------Configuration Testing-------------
        public boolean verifyConfig = false;
        public boolean exportComposeOnly = false;
        public String configDir = "configtests";

        // single version
        public boolean testConfig = false;
        public double testSingleVersionConfigRatio = 0.1;

        public boolean testAddedConfig = false;
        public boolean testDeletedConfig = false;
        public boolean testCommonConfig = false;
        public double testUpgradeConfigRatio = 0.4; // We mutate testConfigRatio
        // configuration each
        // test, default is 40

        // ------------Test Mode-------------
        public boolean testDowngrade = false;
        // failureOver = true: if the seed node in the distributed is dead
        // another node can keep executing commands
        public boolean failureOver = false;

        // 0: only full-stop test using StackedTestPacket
        // 1: Bug Reproduction: Full-Stop Test
        // 2: mixed test using MixedTestPlan
        // 3: Bug Reproduction: Rolling upgrade (given a test plan)
        // 4: full-stop upgrade + mixed Test Plan iteratively (Final Version)
        public int testingMode = 0;
        public boolean testSingleVersion = false;

        // ------------State Comparison-------------
        public boolean enableStateComp = false;
        // Debug option
        public boolean startUpClusterForDebugging = false;
        public boolean useExampleTestPlan = false;
        public boolean debug = false;

        // ---------------Log Check------------------
        // check ERROR/WARN in log
        public boolean enableLogCheck = true;
        public int grepLineNum = 4;
        public boolean filterLogBeforeUpgrade = false;

        // ------------Priority Coverage-------------
        public boolean usePriorityCov = false;
        public double oldCovRatio = 0.4;

        // ---------------Likely Inv-----------------
        // if an invariant is broken over 20% test cases, ignore it
        public double ignoreInvRatio = 0.2;
        public int runtimeMonitorPort = 62000;
        // if a inv is broken, we immediate skip it!
        public boolean skip = false;
        // Prioritize likely invariants in the priority queue
        public int INVARIANT_PRIORITY_SCORE = 20;
        public final int INVARIANT_MAP_LENGTH = 1000;
        /**
         * ---------------Version Specific-----------------
         * To avoid FPs
         * If a command is supported only in the new/old version,
         * this can cause FP when comparing the read results.
         */
        // == cassandra ==
        public boolean eval_CASSANDRA13939 = false;

        // == hdfs ==
        // erasure coding commands
        public boolean support_EC = false; // > 2
        // Storage Type
        public boolean support_NVDIMM = false; // >= 3.4.0
        public boolean support_PROVIDED = false; // > 2
        // Count command
        public boolean support_e_opt = false; // > 2
        public double new_fs_state_prob = 0.005;

        @Override
        public String toString() {
            return new GsonBuilder()
                    .setPrettyPrinting()
                    .disableHtmlEscaping()
                    .create()
                    .toJson(this, Configuration.class);
        }

        public Boolean checkNull() {
            Field[] fields = this.getClass().getDeclaredFields();
            for (Field field : fields) {
                try {
                    Object fieldObject = field.get(this);
                    if (fieldObject == null) {
                        // logger.error("Configuration failed to find: " +
                        // field);
                    }
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }

            // assertTrue(Arrays.stream(fields).anyMatch(
            // field -> field.getName().equals(LAST_NAME_FIELD) &&
            // field.getType().equals(String.class)));
            return true;
        }
    }

    public static void setInstance(Configuration config) {
        instance = config;
    }
}
