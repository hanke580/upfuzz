/* (C)2022 */
package org.zlab.upfuzz.fuzzingengine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.UnexpectedException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jacoco.core.data.ExecutionDataStore;
import org.zlab.upfuzz.CommandSequence;
import org.zlab.upfuzz.CustomExceptions;
import org.zlab.upfuzz.cassandra.CassandraExecutor;
import org.zlab.upfuzz.fuzzingengine.executor.Executor;
import org.zlab.upfuzz.utils.Pair;
import org.zlab.upfuzz.utils.Utilities;

public class FuzzingClient {
    public static final int epochNum = 60; // Validation per epochNum

    /** key: String -> agentId value: Codecoverage for this agent */
    public Map<String, ExecutionDataStore> agentStore;

    /* key: String -> agent Id
     * value: ClientHandler -> the socket to a agent */
    public Map<String, ClientHandler> agentHandler;

    /* key: UUID String -> executor Id
     * value: List<String> -> list of all alive agents with the executor Id */
    public Map<String, List<String>> sessionGroup;

    /* socket for client and agents to communicate*/
    public ClientSocket clientSocket;

    public static int epoch;
    public static int crashID;
    public static int epochStartTestId;

    public Executor executor;

    public static Map<Integer, Pair<CommandSequence, CommandSequence>> testId2Sequence;

    FuzzingClient() {
        init();
        executor = new CassandraExecutor();
        try {
            executor.startup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void init() {
        // TODO: GC the old coverage since we already get the overall coverage.
        agentStore = new HashMap<>();
        agentHandler = new HashMap<>();
        sessionGroup = new HashMap<>();

        epoch = 0;
        crashID = 0;
        epochStartTestId = 0; // FIXME: It might not be zero

        testId2Sequence = new HashMap<>();

        try {
            clientSocket = new ClientSocket(this);
            clientSocket.setDaemon(true);
            clientSocket.start();
        } catch (Exception e) {
            e.printStackTrace();
            // System.exit(1);
        }
    }

    public FeedBack start(
            CommandSequence commandSequence,
            CommandSequence validationCommandSequence,
            int testId) {

        try {
            System.out.println("Main Class Name: " + Utilities.getMainClassName());
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        /** Every epochNum tests, do one upgradeProcess + checking */
        FeedBack fb = new FeedBack();

        // Execute the commands on the running cassandra
        testId2Sequence.put(testId, new Pair<>(commandSequence, validationCommandSequence));

        List<String> oldVersionResult = null;

        try {
            oldVersionResult = executor.execute(commandSequence, validationCommandSequence, testId);

            if (oldVersionResult != null) {
                fb.originalCodeCoverage = collect(executor, "original");
                if (fb.originalCodeCoverage == null) {
                    System.out.println("ERROR: null origin code coverage");
                    System.exit(1);
                }
                // Actually the code coverage do not need to be stored in disk
                // String destFile = executor.getSysExecID() + String.valueOf(testId) + ".exec";
                // try {
                // 	FileOutputStream localFile = new FileOutputStream(destFile);
                // 	ExecutionDataWriter localWriter = new ExecutionDataWriter(localFile);
                // 	codeCoverage.accept(localWriter);
                // 	// localWriter.visitClassExecution(codeCoverage);
                // 	System.out.println("write codecoverage to " + destFile);
                // } catch (IOException e) {
                // 	e.printStackTrace();
                // }
            }
        } catch (CustomExceptions.systemStartFailureException e) {
            System.out.println("old version system start up failed");
        }

        if (epochNum == 1 || testId != 0 && testId % epochNum == 0) {
            /**
             * Perform a validation 1. Stop the running instance 2. Perform Upgrade check 3. Restart
             * the executor
             */
            // long startTime = System.currentTimeMillis();
            executor.saveSnapshot();
            executor.moveSnapShot();
            executor.teardown();
            // long endTime = System.currentTimeMillis();
            // System.out.println("Stop the old version Time: " + (endTime - startTime)/1000 + "s");

            // Upgrade test
            // 1. Upgrade check
            // 2. Read sequence check
            try {
                // Feed it with all the read
                boolean ret = executor.upgradeTest();
                // fb.upgradedCodeCoverage = collect(executor, "upgraded");
                // if (fb.upgradedCodeCoverage == null) {
                // 	System.out.println("ERROR: null upgrade code coverage");
                // 	System.exit(1);
                // }

                if (ret == false) {
                    /**
                     * An inconsistency has been found 1. It could be exception during the upgrade
                     * process 2. The result is different between two versions Serialize them into
                     * the folder, 2 sequences + failureType + failureInfo
                     */
                    while (Paths.get(Config.getConf().crashDir, "crash_" + epoch)
                            .toFile()
                            .exists()) {
                        epoch++;
                    }
                    // 1. Serialize all sequences into one file (String format)

                    // Make an crash Dir
                    new File(Paths.get(Config.getConf().crashDir, "crash_" + epoch).toString())
                            .mkdir();

                    // Inside this Dir, serialize all the executed sequence
                    // File1: CommandSequence
                    StringBuilder commandSequenceString = new StringBuilder();

                    for (int i = epochStartTestId; i <= testId; i++) {
                        for (String cmdStr : testId2Sequence.get(i).left.getCommandStringList()) {
                            commandSequenceString.append(cmdStr + "\n");
                        }
                        commandSequenceString.append("\n");
                    }

                    // Serialize CommandSequence into File
                    Path cmdSeqsPath =
                            Paths.get(
                                    Config.getConf().crashDir,
                                    "crash_" + epoch,
                                    "epoch_cmd_seq_" + epochStartTestId + "_" + testId + ".txt");
                    Utilities.write2TXT(cmdSeqsPath.toFile(), commandSequenceString.toString());

                    // When ret is false, while the testId2Failure is empty!!!

                    // Serialize the bug information
                    for (int testIdx : executor.testId2Failure.keySet()) {

                        if (testIdx == -1) {
                            // The upgrade test failed, directly write report
                            // Serialize the single bug info
                            StringBuilder sb = new StringBuilder();
                            // For each failure, log into a separate file
                            sb.append(
                                    "Failure Type: "
                                            + executor.testId2Failure.get(testIdx).left
                                            + "\n");
                            sb.append(
                                    "Failure Info: "
                                            + executor.testId2Failure.get(testIdx).right
                                            + "\n");
                            Path crashReportPath =
                                    Paths.get(
                                            Config.getConf().crashDir,
                                            "crash_" + epoch,
                                            "crash_" + testIdx + ".txt");
                            Utilities.write2TXT(crashReportPath.toFile(), sb.toString());

                            crashID++;
                            break;
                        }

                        // Serialize the single bug sequence
                        Pair<CommandSequence, CommandSequence> commandSequencePair =
                                new Pair<>(
                                        testId2Sequence.get(testIdx).left,
                                        testId2Sequence.get(testIdx).right);
                        Path commandSequencePairPath =
                                Paths.get(
                                        Config.getConf().crashDir,
                                        "crash_" + epoch,
                                        "crash_" + testIdx + ".ser");
                        Utilities.writeCmdSeq(
                                commandSequencePairPath.toFile(), commandSequencePair);

                        // Serialize the single bug info
                        StringBuilder sb = new StringBuilder();
                        // For each failure, log into a separate file
                        sb.append(
                                "Failure Type: "
                                        + executor.testId2Failure.get(testIdx).left
                                        + "\n");
                        sb.append(
                                "Failure Info: "
                                        + executor.testId2Failure.get(testIdx).right
                                        + "\n");
                        sb.append("Command Sequence\n");
                        for (String commandStr :
                                testId2Sequence.get(testIdx).left.getCommandStringList()) {
                            sb.append(commandStr);
                            sb.append("\n");
                        }
                        sb.append("\n\n");
                        sb.append("Read Command Sequence\n");
                        for (String commandStr :
                                testId2Sequence.get(testIdx).right.getCommandStringList()) {
                            sb.append(commandStr);
                            sb.append("\n");
                        }
                        Path crashReportPath =
                                Paths.get(
                                        Config.getConf().crashDir,
                                        "crash_" + epoch,
                                        "crash_" + testIdx + ".txt");
                        Utilities.write2TXT(crashReportPath.toFile(), sb.toString());
                        crashID++;
                    }
                }
            } catch (CustomExceptions.systemStartFailureException e) {
                System.out.println("New version cassandra start up failed, this could be a bug");
            } catch (Exception e) {
                e.printStackTrace();
                // System.exit(1);
            }
            // Clear the sequence set
            epoch++;
            testId2Sequence.clear();
            executor.clearState();
            epochStartTestId = testId + 1;
            // Restart the old version executor

            System.out.println("\n\nRestart the executor\n");
            executor = new CassandraExecutor();
            executor.startup();
        }
        return fb;
    }

    public ExecutionDataStore collect(Executor executor, String version) {
        List<String> agentIdList = sessionGroup.get(executor.executorID + "_" + version);
        if (agentIdList == null) {
            new UnexpectedException(
                            "No agent connection with executor " + executor.executorID.toString())
                    .printStackTrace();
            return null;
        } else {
            // Add to the original coverage
            for (String agentId : agentIdList) {
                System.out.println("collect conn " + agentId);
                ClientHandler conn = agentHandler.get(agentId);
                if (conn != null) {
                    try {
                        conn.waitSessionData = false;
                        conn.collect();
                        while (!conn.waitSessionData) {
                            // System.out.println("wait Session");
                            Thread.sleep(100);
                        }
                        // System.out.println("wait data1: currentTimeMillis = " +
                        // System.currentTimeMillis() + "  Conn.lastTime = " + conn.lastUpdateTime);
                        while (System.currentTimeMillis() - conn.lastUpdateTime < 300) {
                            // System.out.println("wait data2: currentTimeMillis = " +
                            // System.currentTimeMillis() + "  Conn.lastTime = " +
                            // conn.lastUpdateTime);
                            Thread.sleep(100);
                        }
                    } catch (IOException | InterruptedException e) {
                        // e.printStackTrace();
                    }
                }
            }

            ExecutionDataStore execStore = new ExecutionDataStore();
            for (String agentId : agentIdList) {
                System.out.println("get coverage from " + agentId);
                ExecutionDataStore astore = agentStore.get(agentId);
                if (astore == null) {
                    System.out.println("no data");
                } else {
                    /**
                     * astore: Map: Classname -> int[] probes. this will merge the probe of each
                     * classes.
                     */
                    execStore.merge(astore);
                    System.out.println("astore size: " + execStore.getContents().size());
                }
            }
            System.out.println("codecoverage size: " + execStore.getContents().size());
            // Send coverage back

            return execStore;
        }
    }

    enum FuzzingClientActions {
        start,
        collect;
    }
}
