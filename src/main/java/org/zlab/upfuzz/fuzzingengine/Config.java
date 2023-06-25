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

        public String oldSystemPath = null;
        public String newSystemPath = null;
        public String jacocoAgentPath = null;
        public String system = null;
        public String dataDir = null;
        public String logFile = null;
        public String initSeedDir = null;

        public String failureDir = null;

        public String corpusDir = null;

        public boolean nyxMode = false;
        public String nyxFuzzSH = null;

        // Mutation Epoch
        public int configMutationEpoch = 20;
        public int sequenceMutationEpoch = 200;
        // violent mutation, usually fewer
        public int bothMutationEpoch = 20;
        public int testPlanMutationEpoch = 20;

        // Given a full-stop seed, we generate 20
        // test plan from it.
        public int testPlanGenerationNum = 20;

        public String targetSystemStateFile = "states.json";

        public int STACKED_TESTS_NUM = 60;
        public long timeInterval = 600; // seconds, record time
        public boolean keepDir = true; // set to false if start a long running
                                       // test
        public int nodeNum = 3;

        // ------------Fault Injection-------------
        public boolean shuffleUpgradeOrder = false; // Whether shuffle the
                                                    // upgrade order
        public int faultMaxNum = 2; // disable faults for now
        public boolean alwaysRecoverFault = false;
        public float noRecoverProb = 0.5f;

        public boolean useFeedBack = true;
        public boolean collUpFeedBack = true;
        public int rebuildConnectionSecs = 5;

        // ------------Configuration-------------
        public Boolean verifyConfig = false;
        public String configDir = "configtests";

        public Boolean testAddedConfig = false;
        public Boolean testDeletedConfig = false;
        public Boolean testCommonConfig = false;
        public double testConfigRatio = 0.4; // We mutate testConfigRatio
                                             // configuration each
                                             // test, default is 40

        // ------------Test Plan-------------
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

        // ------------State Comparison-------------
        public boolean enableStateComp = false;

        // Debug option
        public boolean startUpClusterForDebugging = false;
        public boolean useExampleTestPlan = false;
        public boolean debug = false;

        // ------------Log Check-------------
        // check ERROR/WARN in log
        public boolean enableLogCheck = true;
        public int grepLineNum = 4;

        // ------------Priority Coverage-------------
        public boolean usePriorityCov = false;
        public double oldCovRatio = 0.4;

        // ------------Likely Inv-------------
        public boolean useLikelyInv = false;
        // if an invariant is broken for 80% times, ignore it
        public double ignoreInvRatio = 0.8;
        public int runtimeMonitorPort = 62000;

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
