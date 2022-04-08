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

    Process cassandraProcess;
    static final String jacocoOptions = "=append=false,includes=org.apache.cassandra.*,output=dfe,address=localhost,sessionid=";

    public CassandraExecutor(CommandSequence testSeq) {
        super(testSeq, "cassandra");
    }

    public boolean isCassandraReady(String cassandraPath) {
        ProcessBuilder isReadyBuilder = new ProcessBuilder();
        Process isReady;
        int ret = 0;
        try {
            isReady = Utilities.exec(new String[] { "bin/cqlsh", "-e", "describe cluster" }, cassandraPath);
            BufferedReader in = new BufferedReader(new InputStreamReader(isReady.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
            }
            isReady.waitFor();
            in.close();
            ret = isReady.exitValue();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return ret == 0;
    }

    public boolean isNewCassandraReady(String cassandraPath) {
        ProcessBuilder isReadyBuilder = new ProcessBuilder();
        Process isReady;
        int ret = 0;
        try {
            isReady = Utilities.exec(new String[] { "bin/cqlsh", "-e", "describe cluster" }, cassandraPath);
            BufferedReader in = new BufferedReader(new InputStreamReader(isReady.getInputStream()));
            String line;

            while ((line = in.readLine()) != null) {
                //    System.out.println(line);
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
        System.out.println("Start the new version");
        // Remove the data folder in the old version
        // ProcessBuilder pb = new ProcessBuilder("rm", "-rf", "data");
        // pb.directory(new File(Config.getConf().cassandraPath));
        // try {
        //     pb.start();
        // } catch (IOException e) {
        //     e.printStackTrace();
        // }

        ProcessBuilder cassandraProcessBuilder = new ProcessBuilder("bin/cassandra", "-f");
        Map<String, String> env = cassandraProcessBuilder.environment();
        env.put("JAVA_TOOL_OPTIONS",
                "-javaagent:" + Config.getConf().jacocoAgentPath + jacocoOptions + systemID + "-" + executorID);
        cassandraProcessBuilder.directory(new File(Config.getConf().cassandraPath));
        cassandraProcessBuilder.redirectErrorStream(true);
        cassandraProcessBuilder.redirectOutput(Paths.get(Config.getConf().cassandraPath, "logs.txt").toFile());
        try {
            System.out.println("Executor starting cassandra");
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
            while (!isNewCassandraReady(Config.getConf().cassandraPath)) {
                if (!cassandraProcess.isAlive()) {
                    // System.out.println("cassandra process crushed\nCheck " + Config.getConf().cassandraOutputFile
                    //         + " for details");
                    // System.exit(1);
                    throw new CustomExceptions.systemStartFailureException("Cassandra Start fails", null);
                }
                System.out.println("Wait for " + systemID + " ready...");
                Thread.sleep(500);
            }
            long endTime = System.currentTimeMillis();
            System.out.println(
                    "cassandra " + executorID + " ready \n time usage:" + (endTime - startTime) / 1000. + "\n");
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
            assert cassandraProcess.isAlive() == false;
            System.out.println("cassandra " + executorID + " shutdown ok!");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        // Remove the data folder
        pb = new ProcessBuilder("rm", "-rf", "data");
        pb.directory(new File(Config.getConf().cassandraPath));
        try {
            pb.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
                System.out.println(line);
                System.out.flush();
            }
            p.waitFor();
            in.close();
            System.out.println("cassandra " + executorID + " shutdown ok!");

            // p.wait();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        // Remove the data folder
        pb = new ProcessBuilder("rm", "-rf", "data");
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
            CassandraCqlshDaemon cqlsh = new CassandraCqlshDaemon(Config.getConf().cassandraPath);
            for (String cmd : commandList) {
                System.out
                        .println("\n\n------------------------------------------------------------\nexecutor command:\n"
                                + cmd + "\n------------------------------------------------------------\n");
                long startTime = System.currentTimeMillis();
                CqlshPacket cp = cqlsh.execute(cmd);
                long endTime = System.currentTimeMillis();

                ret.add(cp.message);
                System.out.println("ret is: " + cp.exitValue + "\ntime: " + cp.timeUsage + "\ntime usage(network):"
                        + (endTime - startTime) / 1000. + "\n");
            }
            cqlsh.destroy();
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
            CassandraCqlshDaemon cqlsh = new CassandraCqlshDaemon(Config.getConf().upgradeCassandraPath);
            for (String cmd : commandList) {
                System.out
                        .println("\n\n------------------------------------------------------------\nexecutor command:\n"
                                + cmd + "\n------------------------------------------------------------\n");
                long startTime = System.currentTimeMillis();
                CqlshPacket cp = cqlsh.execute(cmd);
                long endTime = System.currentTimeMillis();

                ret.add(cp.message);
                System.out.println("ret is: " + cp.exitValue + "\ntime: " + cp.timeUsage + "\ntime usage(network):"
                        + (endTime - startTime) / 1000. + "\n");
            }
            cqlsh.destroy();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
        return ret;
    }

    public int saveSnapshot() {
        // Flush
        ProcessBuilder pb = new ProcessBuilder("bin/nodetool", "flush");
        pb.directory(new File(Config.getConf().cassandraPath));
        Utilities.runProcess(pb, "[Executor] Old Version System Flush");

        // Copy the data dir
        Path oldFolderPath = Paths.get(Config.getConf().cassandraPath, "data");
        Path newFolderPath = Paths.get(Config.getConf().upgradeCassandraPath);

        pb = new ProcessBuilder("cp", "-r", oldFolderPath.toString(), newFolderPath.toString());
        pb.directory(new File(Config.getConf().cassandraPath));
        Utilities.runProcess(pb, "[Executor] Copy the data folder to the new version");
        return 0;
    }

    @Override
    public int upgradeTest(CommandSequence validationCommandSequence, List<String> oldVersionResult) {
        /**
         * 1. Move the data folder to the new version Cassandra
         * 2. Start the new version cassandra with the Upgrade symbol
         * 3. Check whether there is any exception happen during the
         * 4. Run some commands, check consistency
         *
         * upgrade process...
         *
         * Also need to control the java version.
         */

        // Delete the possible data folder in the old version
        ProcessBuilder pb = new ProcessBuilder("rm", "-rf", "data");
        pb.directory(new File(Config.getConf().cassandraPath));
        Utilities.runProcess(pb, "[Executor] Remove data folder in the old version");

        // Upgrade
        pb = new ProcessBuilder("bin/cassandra");
        pb.directory(new File(Config.getConf().upgradeCassandraPath));
        pb.redirectOutput(Paths.get(Config.getConf().upgradeCassandraPath, "logs.txt").toFile());
        Utilities.runProcess(pb, "Upgrade Cassandra");

        while (!isNewCassandraReady(Config.getConf().upgradeCassandraPath)) {
            // Problem : why would the process be dead?
            // TODO: Enable this checking, add a retry times
            //    if (!upgradeCassandraProcess.isAlive()) {
            //        // Throw a specific exception, if this is upgrade, it means we met a bug
            //        throw new CustomExceptions.systemStartFailureException(
            //                "New version cassandra start fails during" +
            //                " the upgrade process. Tt could be a bug", null);
            //    }
            try {
                System.out.println("Upgrade System Waiting...");
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Data consistency check
        /**
         * Data consistency check
         * If the return size is different, exception when executing the commands, or the results are different.
         * Record both results, report as a potential bug.
         */
        // If there is any exception when executing the commands, it should also be caught
        List<String> newVersionResult = newVersionExecuteCommands(validationCommandSequence);

        boolean isBug = false;
        if (newVersionResult.size() != oldVersionResult.size()) {
            isBug = true;
        } else {
            for (int i = 0; i < newVersionResult.size(); i++) {

                if (oldVersionResult.get(i).compareTo(newVersionResult.get(i)) != 0) {
                    System.out.println("Comparing the results...");
                    System.out.println("Old version: " + oldVersionResult.get(i));
                    System.out.println("new version: " + newVersionResult.get(i));
                    System.out.println();
                    isBug = false;
                    break;
                }
            }
        }

        // Shutdown
        upgradeteardown();
        System.out.println("Upgrade process shutdown successfully");

        return isBug ? 1 : 0;

    }

}
