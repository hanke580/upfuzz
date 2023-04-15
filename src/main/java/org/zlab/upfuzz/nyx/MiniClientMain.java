package org.zlab.upfuzz.nyx;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.zlab.upfuzz.fuzzingengine.packet.FeedbackPacket;
import org.zlab.upfuzz.fuzzingengine.packet.StackedTestPacket;
import org.zlab.upfuzz.fuzzingengine.packet.TestPacket;
import org.zlab.upfuzz.utils.Utilities;

public class MiniClientMain {

    static final String workdir = "~/workdir"; // not exactly sure what this
                                               // TODO

    public static void main(String[] args) throws IOException {
        System.out.println("Starting up MiniClient!");
        // setup docker compose!

        // there should be a "docker-compose.yaml" in whatever working dir this
        // java was called in
        // boot up the docker system
        Utilities.exec(new String[] { "docker", "compose", "up", "-d" },
                workdir);

        // tcp connection setup HERE

        System.out.print("READY_FOR_TESTS");

        Scanner stdin = new Scanner(System.in);
        if (!stdin.nextLine().equals("START_TESTING")) {
            // We have an issue here...
            throw new RuntimeException();
        }

        // Read the stacked packet
        StackedTestPacket stackedTestPacket = null;
        do { // keep trying until stackedTestPacket isnt null
            try {
                FileInputStream fileIn = new FileInputStream("filepath");
                ObjectInputStream objectIn = new ObjectInputStream(fileIn);
                stackedTestPacket = (StackedTestPacket) objectIn.readObject();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } while (stackedTestPacket == null);

        // Map<Integer, FeedbackPacket> testID2FeedbackPacket = new HashMap<>();
        // Map<Integer, List<String>> testID2oriResults = new HashMap<>();
        // Map<Integer, List<String>> testID2upResults = new HashMap<>();

        // for (TestPacket tp : stackedTestPacket.getTestPacketList()) {
        // executor.executeCommands(tp.originalCommandSequenceList);

        // FeedBack[] feedBacks = new FeedBack[stackedTestPacket.nodeNum];
        // for (int i = 0; i < stackedTestPacket.nodeNum; i++) {
        // feedBacks[i] = new FeedBack();
        // }
        // ExecutionDataStore[] oriCoverages = executor
        // .collectCoverageSeparate("original");
        // if (oriCoverages != null) {
        // for (int nodeIdx = 0; nodeIdx < stackedTestPacket.nodeNum; nodeIdx++)
        // {
        // feedBacks[nodeIdx].originalCodeCoverage = oriCoverages[nodeIdx];
        // }
        // }

        // testID2FeedbackPacket.put(
        // tp.testPacketID,
        // new FeedbackPacket(tp.systemID, stackedTestPacket.nodeNum,
        // tp.testPacketID, feedBacks, null));

        // List<String> oriResult = executor
        // .executeCommands(tp.validationCommandSequenceList);

        // testID2oriResults.put(tp.testPacketID, oriResult);
        // }

        // StackedFeedbackPacket stackedFeedbackPacket = new
        // StackedFeedbackPacket(
        // stackedTestPacket.configFileName);
        // stackedFeedbackPacket.fullSequence = recordStackedTestPacket(
        // stackedTestPacket);

        // // LOG checking1
        // Map<Integer, LogInfo> logInfoBeforeUpgrade = null;
        // if (Config.getConf().enableLogCheck) {
        // logger.info("[HKLOG] error log checking");
        // logInfoBeforeUpgrade = executor.grepLogInfo();
        // }

        // boolean ret = executor.fullStopUpgrade();

        // if (!ret) {
        // // upgrade failed
        // String upgradeFailureReport = genUpgradeFailureReport(
        // executor.executorID, stackedTestPacket.configFileName);
        // stackedFeedbackPacket.isUpgradeProcessFailed = true;
        // stackedFeedbackPacket.upgradeFailureReport = upgradeFailureReport;
        // tearDownExecutor();
        // return stackedFeedbackPacket;
        // }

        // logger.info("upgrade succeed");
        // stackedFeedbackPacket.isUpgradeProcessFailed = false;

        // for (TestPacket tp : stackedTestPacket.getTestPacketList()) {
        // List<String> upResult = executor
        // .executeCommands(tp.validationCommandSequenceList);
        // testID2upResults.put(tp.testPacketID, upResult);
        // if (Config.getConf().collUpFeedBack) {
        // ExecutionDataStore[] upCoverages = executor
        // .collectCoverageSeparate("upgraded");
        // if (upCoverages != null) {
        // for (int nodeIdx = 0; nodeIdx < stackedTestPacket.nodeNum; nodeIdx++)
        // {
        // testID2FeedbackPacket.get(
        // tp.testPacketID).feedBacks[nodeIdx].upgradedCodeCoverage =
        // upCoverages[nodeIdx];
        // }
        // }
        // }
        // }

        // // Check read results consistency
        // for (TestPacket tp : stackedTestPacket.getTestPacketList()) {
        // Pair<Boolean, String> compareRes = executor
        // .checkResultConsistency(
        // testID2oriResults.get(tp.testPacketID),
        // testID2upResults.get(tp.testPacketID), true);

        // FeedbackPacket feedbackPacket = testID2FeedbackPacket
        // .get(tp.testPacketID);

        // if (!compareRes.left) {
        // String failureReport = genInconsistencyReport(
        // executor.executorID, stackedTestPacket.configFileName,
        // compareRes.right, recordSingleTestPacket(tp));
        // feedbackPacket.isInconsistent = true;
        // feedbackPacket.inconsistencyReport = failureReport;
        // }
        // // logger.debug("testID2upResults = " + testID2upResults
        // // .get(tp.testPacketID));
        // feedbackPacket.validationReadResults = testID2upResults
        // .get(tp.testPacketID);
        // stackedFeedbackPacket.addFeedbackPacket(feedbackPacket);
        // }
        // logger.info(executor.systemID + " executor: " + executor.executorID
        // + " finished execution");

        // // test downgrade
        // if (Config.getConf().testDowngrade) {
        // logger.info("downgrade cluster");
        // boolean downgradeStatus = executor.downgrade();
        // if (!downgradeStatus) {
        // // downgrade failed
        // stackedFeedbackPacket.isDowngradeProcessFailed = true;
        // stackedFeedbackPacket.downgradeFailureReport =
        // genDowngradeFailureReport(
        // executor.executorID,
        // stackedFeedbackPacket.configFileName);
        // }
        // }

        // // LOG checking2
        // if (Config.getConf().enableLogCheck) {
        // logger.info("[HKLOG] error log checking: merge logs");
        // assert logInfoBeforeUpgrade != null;
        // Map<Integer, LogInfo> logInfo = filterErrorLog(logInfoBeforeUpgrade,
        // executor.grepLogInfo());
        // if (hasERRORLOG(logInfo)) {
        // stackedFeedbackPacket.hasERRORLog = true;
        // stackedFeedbackPacket.errorLogReport = genErrorLogReport(
        // executor.executorID, stackedTestPacket.configFileName,
        // logInfo);
        // }
        // }
        // tearDownExecutor();
        // return stackedFeedbackPacket;
    }

}
