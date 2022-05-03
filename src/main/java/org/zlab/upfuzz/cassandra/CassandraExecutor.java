package org.zlab.upfuzz.cassandra;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.zlab.upfuzz.CommandSequence;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.cassandra.CassandraCqlshDaemon.CqlshPacket;
import org.zlab.upfuzz.CustomExceptions;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.fuzzingengine.executor.Executor;
import org.zlab.upfuzz.utils.Pair;
import org.zlab.upfuzz.utils.Utilities;

public class CassandraExecutor extends Executor {

    CassandraCqlshDaemon cqlsh = null;

    Process cassandraProcess;
    static final String jacocoOptions = "=append=false,includes=org.apache.cassandra.*,output=dfe,address=localhost,sessionid=";

    public CassandraExecutor() {
        super("cassandra");
    }

    public boolean isCassandraReady(String cassandraPath) {
    //    ProcessBuilder isReadyBuilder = new ProcessBuilder();
        Process isReady;
        int ret = 0;
        try {
            isReady = Utilities.exec(new String[] { "bin/cqlsh", "-e", "describe cluster" }, cassandraPath);
            BufferedReader in = new BufferedReader(new InputStreamReader(isReady.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                // System.out.println(line);
            }
            isReady.waitFor();
            in.close();
            ret = isReady.exitValue();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return ret == 0;
    }

    @Override
    public void startup() {
        System.out.println("[Old Version] Cassandra Start...");

        ProcessBuilder cassandraProcessBuilder = new ProcessBuilder("bin/cassandra", "-f");
        Map<String, String> env = cassandraProcessBuilder.environment();
        env.put("JAVA_TOOL_OPTIONS",
                "-javaagent:" + Config.getConf().jacocoAgentPath + jacocoOptions + systemID + "-" + executorID);
        cassandraProcessBuilder.directory(new File(Config.getConf().cassandraPath));
        cassandraProcessBuilder.redirectErrorStream(true);
        cassandraProcessBuilder.redirectOutput(Paths.get(Config.getConf().cassandraPath, "logs.txt").toFile());
        try {
            long startTime = System.currentTimeMillis();
            cassandraProcess = cassandraProcessBuilder.start();
            // byte[] out = cassandraProcess.getInputStream().readAllBytes();
            // BufferedReader in = new BufferedReader(new InputStreamReader(cassandraProcess.getInputStream()));
            // String line;
            // while ((line = in.readLine()) != null) {
            //     System.out.println(line);
            //     System.out.flush();
            // }
            // in.close();
            // cassandraProcess.waitFor();
            System.out.println("cassandra " + executorID + " started");
            while (!isCassandraReady(Config.getConf().cassandraPath)) {
                if (!cassandraProcess.isAlive()) {
                    // System.out.println("cassandra process crushed\nCheck " + Config.getConf().cassandraOutputFile
                    //         + " for details");
                    // System.exit(1);
                    throw new CustomExceptions.systemStartFailureException("Cassandra Start fails", null);
                }
                System.out.println("Wait for " + systemID + " ready...");
                Thread.sleep(2000);
            }
            long endTime = System.currentTimeMillis();
            System.out.println(
                    "cassandra " + executorID + " ready \n time usage:" + (endTime - startTime) / 1000. + "\n");

            cqlsh = new CassandraCqlshDaemon(Config.getConf().cassandraPath);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void teardown() {
        ProcessBuilder pb = new ProcessBuilder("bin/nodetool", "stopdaemon");
        pb.directory(new File(Config.getConf().cassandraPath));
        Process p;
        try {
            p = pb.start();
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println(line);
                System.out.flush();
            }
            p.waitFor();
            in.close();
            assert !cassandraProcess.isAlive();
            System.out.println("cassandra " + executorID + " shutdown ok!");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        // Remove the data folder
        pb = new ProcessBuilder("rm", "-rf", "data", "logs.txt");
        pb.directory(new File(Config.getConf().cassandraPath));
        try {
            pb.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.cqlsh.destroy();
    }

    public void upgradeteardown() {
        ProcessBuilder pb = new ProcessBuilder("bin/nodetool", "stopdaemon");
        pb.directory(new File(Config.getConf().upgradeCassandraPath));
        Process p;
        try {
            p = pb.start();
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                // System.out.println(line);
                // System.out.flush();
            }
            p.waitFor();
            in.close();
            System.out.println("Upgrade Cassandra " + executorID + " shutdown ok");

            // p.wait();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        // Remove the data folder
        pb = new ProcessBuilder("rm", "-rf", "data", "logs.txt");
        pb.directory(new File(Config.getConf().upgradeCassandraPath));
        try {
            pb.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Upgrade folder has been removed");

        // Stop all running cassandra instances
        // pgrep -u vagrant -f cassandra | xargs kill -9
        pb = new ProcessBuilder("pgrep", "-u", "vagrant", "cassandra", "| xargs kill -9");
        pb.directory(new File(Config.getConf().upgradeCassandraPath));
        Utilities.runProcess(pb, "kill cassandra instances");

        this.cqlsh.destroy();

    }

    public static Pair<CommandSequence, CommandSequence> prepareCommandSequence() {
        CommandSequence commandSequence = null;
        CommandSequence validationCommandSequence = null;
        try {
            commandSequence = CommandSequence.generateSequence(CassandraCommands.commandClassList,
                    CassandraCommands.createCommandClassList, CassandraState.class, null);
            // TODO: If it's generating read with a initial state, no need to generate with createTable...
            validationCommandSequence = CommandSequence.generateSequence(CassandraCommands.readCommandClassList, null,
                    CassandraState.class, commandSequence.state);
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException
                | IllegalAccessException e) {
            e.printStackTrace();
        }
        return new Pair<>(commandSequence, validationCommandSequence);
    }

    public static CommandSequence prepareValidationCommandSequence(State state) {
        CommandSequence validationCommandSequence = null;
        try {
            validationCommandSequence = CommandSequence.generateSequence(CassandraCommands.readCommandClassList, null,
                    CassandraState.class, state);
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException
                | IllegalAccessException e) {
            e.printStackTrace();
        }
        return validationCommandSequence;
    }

    @Override
    public List<String> executeCommands(CommandSequence commandSequence) {
        //        commandSequence = prepareCommandSequence();
        List<String> commandList = commandSequence.getCommandStringList();
        List<String> ret = new LinkedList<>();
        try {
            if (cqlsh == null)
                cqlsh = new CassandraCqlshDaemon(Config.getConf().cassandraPath);
            for (String cmd : commandList) {
                // System.out
                //         .println("\n\n------------------------------------------------------------\nexecutor command:\n"
                //                 + cmd + "\n------------------------------------------------------------\n");
                long startTime = System.currentTimeMillis();
                CqlshPacket cp = cqlsh.execute(cmd);
                long endTime = System.currentTimeMillis();

                ret.add(cp.message);
                // System.out.println("ret is: " + cp.exitValue + "\ntime: " + cp.timeUsage + "\ntime usage(network):"
                //         + (endTime - startTime) / 1000. + "\n");
            }
//            cqlsh.destroy();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
        return ret;
    }

    public List<String> newVersionExecuteCommands(CommandSequence commandSequence) {
        //        commandSequence = prepareCommandSequence();
        List<String> commandList = commandSequence.getCommandStringList();
        List<String> ret = new LinkedList<>();
        try {
            // TODO: Put the cqlsh daemon outside, so that one instance for one cqlsh daemon

            if (cqlsh == null)
                cqlsh = new CassandraCqlshDaemon(Config.getConf().upgradeCassandraPath);
            for (String cmd : commandList) {
                // System.out
                //         .println("\n\n------------------------------------------------------------\nexecutor command:\n"
                //                 + cmd + "\n------------------------------------------------------------\n");
                long startTime = System.currentTimeMillis();
                CqlshPacket cp = cqlsh.execute(cmd);
                long endTime = System.currentTimeMillis();

                ret.add(cp.message);
                // System.out.println("ret is: " + cp.exitValue + "\ntime: " + cp.timeUsage + "\ntime usage(network):"
                //         + (endTime - startTime) / 1000. + "\n");
            }
//            cqlsh.destroy();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
        return ret;
    }

    @Override
    public int saveSnapshot() {
        // Flush
        ProcessBuilder pb = new ProcessBuilder("bin/nodetool", "flush");
        pb.directory(new File(Config.getConf().cassandraPath));
        Utilities.runProcess(pb, "[Executor] Old Version System Flush");
        return 0;
    }

    @Override
    public int moveSnapShot() {
        // Copy the data dir
        Path oldFolderPath = Paths.get(Config.getConf().cassandraPath, "data");
        Path newFolderPath = Paths.get(Config.getConf().upgradeCassandraPath);

        ProcessBuilder pb = new ProcessBuilder("cp", "-r", oldFolderPath.toString(), newFolderPath.toString());
        pb.directory(new File(Config.getConf().cassandraPath));
        Utilities.runProcess(pb, "[Executor] Copy the data folder to the new version");
        return 0;
    }

    /**
     * 1. Move the data folder to the new version Cassandra
     * 2. Start the new version cassandra with the Upgrade symbol
     * 3. Check whether there is any exception happen during the
     * 4. Run some commands, check consistency
     *
     * upgrade process...
     * Also need to control the java version.
     */
    @Override
    public boolean upgradeTest() {
        /**
         * Data consistency check
         * If the return size is different, exception when executing the commands, or the results are different.
         * Record both results, report as a potential bug.
         * 
         * A crash seed should contain:
         * 1. Two command sequences.
         * 2. The results on old and new version.
         * 3. The reason why it's different
         *      - Upgrade process throw an exception
         *      - The result of a specific command is different
         */
        // If there is any exception when executing the commands, it should also be caught

        // Upgrade Startup
        ProcessBuilder pb = new ProcessBuilder("bin/cassandra");
        pb.directory(new File(Config.getConf().upgradeCassandraPath));
        pb.redirectOutput(Paths.get(Config.getConf().upgradeCassandraPath, "logs.txt").toFile());
        // long startTime = System.currentTimeMillis();
        Utilities.runProcess(pb, "Upgrade Cassandra");
        // Process upgradeCassandraProcess = Utilities.runProcess(pb, "Upgrade Cassandra");

        // Add a retry time here
        boolean started = false;
        int RETRY_START_UPGRADE = 25;
        for (int i = 0; i < RETRY_START_UPGRADE; i++) {
            if (isCassandraReady(Config.getConf().upgradeCassandraPath)) {
                started = true;
                break;
            }
            try {
                System.out.println("Upgrade System Waiting...");
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        FailureType failureType = null;
        String failureInfo = null;

        if (!started) {
            // Retry the upgrade, clear the folder, kill the hang process
            System.out.println("[FAILURE LOG] New version cannot start");
            failureType = FailureType.UPGRADE_FAIL;
            failureInfo = "New version cassandra cannot start\n";

            testId2Failure.put(-1, new Pair<>(failureType, failureInfo));
            return false;
        }

        // long endTime = System.currentTimeMillis();
        // System.out.println("Upgrade System Start Time = " + (endTime - startTime)/1000.  + "s");
        try {
            this.cqlsh = new CassandraCqlshDaemon(Config.getConf().upgradeCassandraPath);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // while (!isNewCassandraReady(Config.getConf().upgradeCassandraPath)) {
        //     // Problem : why would the process be dead?
        //     // TODO: Enable this checking, add a retry times
        //     if (!upgradeCassandraProcess.isAlive()) {
        //         // Throw a specific exception, if this is upgrade, it means we met a bug
        //         System.out.println("[FAILURE LOG] New version cannot start");
        //         failureType = FailureType.UPGRADE_FAIL;
        //         failureInfo = "New version cassandra cannot start";

        //         return false;
        //     }
        //     try {
        //         System.out.println("Upgrade System Waiting...");
        //         Thread.sleep(1000);
        //     } catch (InterruptedException e) {
        //         e.printStackTrace();
        //     }
        // }

        // startTime = System.currentTimeMillis();
        for (Integer testId : testId2commandSequence.keySet()) {
            testId2newVersionResult.put(
                    testId,
                    newVersionExecuteCommands(testId2commandSequence.get(testId).right)
            );
        }

        // Iterate all results, find out the one with difference
        for (Integer testId : testId2commandSequence.keySet()) {
            // System.out.println("\n\t testId = " + testId);
            boolean ret = true;
            failureType = null;
            failureInfo = null;

            List<String> oldVersionResult = testId2oldVersionResult.get(testId);
            List<String> newVersionResult = testId2newVersionResult.get(testId);

            if (newVersionResult.size() != oldVersionResult.size()) {
                failureType = FailureType.RESULT_INCONSISTENCY;
                failureInfo = "The result size is different, old version result size = " +
                        oldVersionResult.size() + "  while new version result size" + newVersionResult.size();
                ret = false;
            } else {
                for (int i = 0; i < newVersionResult.size(); i++) {

                    if (oldVersionResult.get(i).compareTo(newVersionResult.get(i)) != 0) {

                        // SyntaxException
                        if (oldVersionResult.get(i).contains("SyntaxException") && newVersionResult.get(i).contains("SyntaxException")) {
                            continue;
                        }

                        // InvalidRequest
                        if (oldVersionResult.get(i).contains("InvalidRequest") && newVersionResult.get(i).contains("InvalidRequest")) {
                            continue;
                        }

                        // System.out.println("old version result: " + oldVersionResult.get(i));
                        // System.out.println("new version result: " + newVersionResult.get(i));

                        failureType = FailureType.RESULT_INCONSISTENCY;

                        String errorMsg = "Result not the same at read sequence id = " + i + "\n" +
                                "Old Version Result: " + oldVersionResult.get(i) + "  " +
                                "New Version Result: " + newVersionResult.get(i) + "\n";

                        if (failureInfo == null) {
                            failureInfo = errorMsg;
                        } else {
                            failureInfo += errorMsg;
                        }
                        ret = false;
//                        break; // Try to log all the difference for this instance
                    }
                }
            }
            if (!ret) {
                testId2Failure.put(testId, new Pair<>(failureType, failureInfo));
            }
        }

        // endTime = System.currentTimeMillis();
        // System.out.println("Upgrade System comparing results = " + (endTime - startTime)/1000.  + "s");
        // Shutdown
        // startTime = System.currentTimeMillis();
        upgradeteardown();
        // endTime = System.currentTimeMillis();
        // System.out.println("New version Stop = " + (endTime - startTime)/1000.  + "s");

        // true means upgrade test succeeded, false means an inconsistency exists
        return testId2Failure.isEmpty();

    }

}
