package org.zlab.upfuzz.fuzzingengine;

import com.google.gson.GsonBuilder;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuration for the fuzzing engine
 * - Do not modify the default configurations!
 * - Modify it in the config.json file to override them
 */
public class Config {

    public static Configuration instance;

    public static Configuration getConf() {
        return instance;
    }

    public Config() {
        instance = new Configuration();

        // Path for format coverage
        if (instance.useFormatCoverage) {
            try {
                URL sysInfoURL = getClass().getResource(
                        instance.system + "/"
                                + instance.originalVersion);
                Path sysInfoPath = Paths.get(sysInfoURL.toURI());
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }

    }

    public static class Configuration {
        // ------ debug coverage ------
        public boolean debugCoverage = false;

        // ----------- general ------------
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

        public boolean nyxMode = false;
        public String nyxFuzzSH = null;

        // -------------- GC --------------
        public int gcInterval = 5; // minutes

        // ------------ Corpus ------------
        /**
         * 0: 6 queues (reuse bishal's implementation)
         * 1: 4 queues (ke's implementation)
         */
        public String corpus = "corpus";
        public boolean saveCorpusToDisk = true;
        public boolean loadInitCorpus = false;
        public boolean reuseInitSeedConfig = false;

        // ------------ Input Generation ------------
        // Debug use the same command sequence
        public boolean useFixedCommand = false;

        // Sequence Generation
        public int MIN_CMD_SEQ_LEN = 15;
        public int MAX_CMD_SEQ_LEN = 100;

        // Expected len = ~20
        // Base for the exponential function
        // Skew model of command sequence length
        public double CMD_SEQ_LEN_LAMBDA = 0.2;

        public int SET_TYPE_MAX_SIZE = 10;

        // 95% get seed from corpus, 5% generate new seed
        public double getSeedFromCorpusRatio = 0.95;

        // ---------------- Mutation ---------------

        // Mutation
        // For the first firstMutationSeedLimit seeds added
        // to the corpus, mutate them for relative few times
        public int firstMutationSeedLimit = 5;
        public int firstSequenceMutationEpoch = 10;
        public int sequenceMutationEpoch = 800;
        public int firstConfigMutationEpoch = 3;
        public int limitConfigForVersionDeltaRound = 100;
        public int configMutationEpoch = 20;

        // Fix config and random generate new command sequences
        // Focus on fuzzing
        public int firstSequenceRandGenEpoch = 10;
        public int sequenceRandGenEpoch = 200;

        /**
         * When we only mutate config, we cannot stack them
         * together. For throughput, we can stack
         * other tests here. But this is a hack, it looks like
         * that we already think that this config is interesting,
         * which is not reasonable.
         * Also, with NYX, there's no need for doing this.
         *
         * This can be enabled when using stacked tests and aiming
         * only for largest throughput.
         */
        public boolean paddingStackedTestPackets = false;

        /* Special Mutation */
        public boolean enableAddMultiCommandMutation = true;
        // If choose to add command, 30% add multiple commands
        public double addCommandWithSameTypeProb = 0.3;
        public int addCommandWithSameTypeNum = 3;
        // violent mutation, usually fewer
        public int bothMutationEpoch = 20;
        public int testPlanMutationEpoch = 20;
        public int testPlanMutationRetry = 50;
        // Given a full-stop seed, we generate 20
        // test plan from it.
        public int testPlanGenerationNum = 20;

        // deprecated
        public String targetSystemStateFile = "states.json";

        public int STACKED_TESTS_NUM = 50;
        public int batchSizeInGroup2 = 30;
        public long timeInterval = 600; // seconds, record time
        public boolean keepDir = true; // set to false if start a long running
                                       // test
        public int nodeNum = 3;

        // ------------Branch Coverage------------
        public boolean useBranchCoverage = true;
        public boolean collUpFeedBack = true;
        public boolean collDownFeedBack = true;

        // ------------Fault Injection-------------
        public boolean shuffleUpgradeOrder = false; // Whether shuffle the
                                                    // upgrade order
        public int faultMaxNum = 2; // disable faults for now
        public boolean alwaysRecoverFault = false;
        public float noRecoverProb = 0.5f;

        public int rebuildConnectionSecs = 5;

        // ------------Configuration Testing-------------
        public boolean verifyConfig = false;
        public String configDir = "configtests";

        // == single version ==
        public boolean testConfig = false;
        public double testSingleVersionConfigRatio = 0.1;

        // == upgrade ==
        public boolean testBoundaryConfig = false;
        // Mutate all boundary related configs
        public double testBoundaryUpgradeConfigRatio = 1;

        public boolean testAddedConfig = false;
        public boolean testDeletedConfig = false;
        // marked "deprecated"
        public boolean testCommonConfig = false;
        public boolean testRemainConfig = false;
        // Mutate 40% of the upgrade configs
        public double testUpgradeConfigRatio = 0.4;
        public double testRemainUpgradeConfigRatio = 0.2;

        // ------------Test Mode-------------
        public boolean testDowngrade = false;
        // failureOver = true: if the seed node in the distributed is dead
        // another node can keep executing commands
        public boolean failureOver = false;

        // 0: only full-stop test using StackedTestPacket
        // 1: Bug Reproduction: Full-Stop Test
        // 2: mixed test using MixedTestPlan
        // 3: Bug Reproduction: Rolling upgrade (given a test plan)
        // 4: full-stop upgrade + rolling upgrade iteratively (Final Version)
        public int testingMode = 0;
        public boolean testSingleVersion = false;
        // This make the test plan interleave with
        // full-stop upgrade
        public boolean fullStopUpgradeWithFaults = false;

        // ------------State Comparison (deprecated) -------------
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

        // ---------------Test Graph-----------------
        public String testGraphDirPath = "graph";

        // ---------------Format Coverage-----------------
        // whether to use format coverage to guide the test (add to corpus)
        // If disabled, we also won't collect format coverage
        public boolean useFormatCoverage = false;

        // NonVerisonDeltaMode
        // 70% fuzz FC, 30% fuzz BC
        public double BC_CorpusNonVersionDelta = 0.2;
        public double FC_CorpusNonVersionDelta = 0.6;
        public double BoundaryChange_CorpusNonVersionDelta = 0.2;

        public int formatCoveragePort = 62000;

        public String baseClassInfoFileName = "serializedFields_alg1.json";
        public String topObjectsFileName = "topObjects.json";
        public String comparableClassesFileName = "comparableClasses.json";
        public String branch2CollectionFileName = "branch2Collection.json";

        // ---------------Version Delta-----------------
        public boolean useVersionDelta = false;

        public int versionDeltaApproach = 1;

        // Approach 1: Four Queue Implementation
        public double FC_VD_PROB_CorpusVersionDeltaFourQueue = 0.4;
        public double FC_PROB_CorpusVersionDeltaFourQueue = 0.3;
        public double BC_VD_PROB_CorpusVersionDeltaFourQueue = 0.2;
        public double BC_PROB_CorpusVersionDeltaFourQueue = 0.1;

        // Approach 1: Five Queue Implementation with boundary: no boundary
        // delta
        public double FC_VD_PROB_CorpusVersionDeltaFiveQueueWithBoundary = 0.3;
        public double FC_PROB_CorpusVersionDeltaFiveQueueWithBoundary = 0.2;
        public double BC_VD_PROB_CorpusVersionDeltaFiveQueueWithBoundary = 0.1;
        public double BC_PROB_CorpusVersionDeltaFiveQueueWithBoundary = 0.1;
        public double BoundaryChange_PROB_CorpusVersionDeltaFiveQueueWithBoundary = 0.3;

        // Approach 1: Six Queue Implementation with boundary: compute boundary
        // delta
        public double FC_VD_PROB_CorpusVersionDeltaSixQueueWithBoundary = 0.25;
        public double FC_PROB_CorpusVersionDeltaSixQueueWithBoundary = 0.15;
        public double BC_VD_PROB_CorpusVersionDeltaSixQueueWithBoundary = 0.1;
        public double BC_PROB_CorpusVersionDeltaSixQueueWithBoundary = 0.1;
        public double BoundaryChange_VD_PROB_CorpusVersionDeltaSixQueueWithBoundary = 0.25;
        public double BoundaryChange_PROB_CorpusVersionDeltaSixQueueWithBoundary = 0.15;

        // Approach 2: Six Queue Implementation
        // Group1
        public double branchVersionDeltaChoiceProb = 0.2;
        public double formatVersionDeltaChoiceProb = 0.4;
        public double branchCoverageChoiceProb = 0.1;
        public double formatCoverageChoiceProb = 0.2;
        public double boundaryRelatedSeedsChoiceProb = 0.1;

        public double nonInterestingTestsUpgradeProb = 0.6;

        // Group2
        // Seeds that trigger new coverage before version change
        public double branchCovSeedChoiceProb = 0.1;
        // Seeds that trigger new format before version change
        public double formatCovSeedChoiceProb = 0.15;
        // Seeds that trigger version delta before version change
        public double branchDeltaSeedChoiceProb = 0.25;
        // Seeds that trigger format delta before version change
        public double formatDeltaSeedChoiceProb = 0.4;
        // Seeds that trigger new coverage after upgrade
        public double branchCovAfterUpgSeedChoiceProb = 0.1;
        // Seeds that trigger new coverage after downgrade
        public double branchCovAfterDowngSeedChoiceProb = 0;

        // Approach 2: Eight Queue Implementation with boundary
        // Group1
        public double FC_VD_PROB_CorpusVersionDeltaEightQueueWithBoundary_G1 = 0.25;
        public double FC_PROB_CorpusVersionDeltaEightQueueWithBoundary_G1 = 0.15;
        public double BC_VD_PROB_CorpusVersionDeltaEightQueueWithBoundary_G1 = 0.1;
        public double BC_PROB_CorpusVersionDeltaEightQueueWithBoundary_G1 = 0.1;
        public double BoundaryChange_VD_PROB_CorpusVersionDeltaEightQueueWithBoundary_G1 = 0.25;
        public double BoundaryChange_PROB_CorpusVersionDeltaEightQueueWithBoundary_G1 = 0.15;

        // Group2
        public double FC_VD_PROB_CorpusVersionDeltaEightQueueWithBoundary_G2 = 0.2;
        public double FC_PROB_CorpusVersionDeltaEightQueueWithBoundary_G2 = 0.15;
        public double BC_VD_PROB_CorpusVersionDeltaEightQueueWithBoundary_G2 = 0.15;
        public double BC_PROB_CorpusVersionDeltaEightQueueWithBoundary_G2 = 0.05;
        public double BC_After_Upgrade_PROB_CorpusVersionDeltaEightQueueWithBoundary_G2 = 0.05;
        public double BC_After_Downgrade_PROB_CorpusVersionDeltaEightQueueWithBoundary_G2 = 0.05;
        public double BoundaryChange_VD_PROB_CorpusVersionDeltaEightQueueWithBoundary_G2 = 0.2;
        public double BoundaryChange_PROB_CorpusVersionDeltaEightQueueWithBoundary_G2 = 0.15;

        /**
         * ---------------Version Specific-----------------
         * To avoid FPs
         * If a command is supported only in the new/old version,
         * this can cause FP when comparing the read results.
         *
         * Do not modify these default configurations!
         */
        // == cassandra ==
        public boolean cassandraEnableTimeoutCheck = true;

        public boolean eval_CASSANDRA13939 = false;
        public boolean eval_CASSANDRA14912 = false;
        public int CASSANDRA_LIST_TYPE_MAX_SIZE = 10;
        public boolean CASSANDRA_ENABLE_SPECULATIVE_RETRY = true;

        // Three choices: disable, flush or drain
        public boolean flushAfterTest = true;
        // Drain: remove all commit logs
        public boolean drain = true;

        // == hdfs ==
        // If true: first create fsimage, then execute some commands
        // to test the edits log replay. If false, no edits log will
        // be replayed in the new version.
        public boolean prepareImageFirst = true;
        // If false: it won't create FSImage before upgrade
        public boolean enable_fsimage = true;
        public double new_fs_state_prob = 0.005;

        public boolean support_EC = false; // > 2
        public boolean support_StorageType_PROVIDED = false; // > 2
        public boolean support_count_e_opt = false; // > 2
        public boolean support_du_v_opt = false; // > 2
        public boolean enable_du = false; // du can be tested for version > 2
        public boolean support_StorageType_NVDIMM = false; // >= 3.4.0
        public boolean support_checksum_v_opt = false; // > 3.3.x

        // == hbase ==
        public boolean enableQuota = true;
        public int MAX_CF_NUM = 7;
        public final String[] REGIONSERVERS = { "hregion1", "hregion2" };
        public final String[] SERVERS = { "hmaster", "hregion1", "hregion2" };
        public int REGIONSERVER_PORT = 16020;

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
