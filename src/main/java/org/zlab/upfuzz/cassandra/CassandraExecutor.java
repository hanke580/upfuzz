package org.zlab.upfuzz.cassandra;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.zlab.upfuzz.CommandSequence;
import org.zlab.upfuzz.cassandra.CassandraCqlshDaemon.CqlshPacket;
import org.zlab.upfuzz.CustomExceptions;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.fuzzingengine.executor.Executor;
import org.zlab.upfuzz.utils.SystemUtil;
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
            isReady = SystemUtil.exec(new String[] { "bin/cqlsh", "hk",  "-e", "describe cluster" }, cassandraPath);
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
        cassandraProcessBuilder.redirectOutput(new File(Config.getConf().cassandraOutputFile));
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
            while (!isCassandraReady(Config.getConf().cassandraPath)) {
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
    }

    public static CommandSequence prepareCommandSequence() {
        CassandraState state = new CassandraState();
        CommandSequence commandSequence = null;
        try {
            commandSequence = CommandSequence.generateSequence(CassandraCommands.commandClassList,
                    CassandraCommands.createCommandClassList, CassandraState.class);
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException
                | IllegalAccessException e) {
            e.printStackTrace();
        }
        return commandSequence;
    }

    @Override
    public int executeCommands(CommandSequence commandSequence) {
//        commandSequence = prepareCommandSequence();
        List<String> commandList = commandSequence.getCommandStringList();
        try {
            CassandraCqlshDaemon cqlsh = new CassandraCqlshDaemon();
            for (String cmd : commandList) {
                System.out
                        .println("\n\n------------------------------------------------------------\nexecutor command:\n"
                                + cmd + "\n------------------------------------------------------------\n");
                long startTime = System.currentTimeMillis();
                CqlshPacket cp = cqlsh.execute(cmd);
                long endTime = System.currentTimeMillis();
                System.out.println("ret is: " + cp.exitValue + "\ntime: " + cp.timeUsage + "\ntime usage(network):"
                        + (endTime - startTime) / 1000. + "\n");
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return 127;
        }
        return 0;
    }

    @Override
    public int upgradeTest() {
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

        Path oldFolderPath = Paths.get(Config.getConf().cassandraPath, "data");
        Path newFolderPath = Paths.get(Config.getConf().upgradeCassandraPath);


        // Delete the possible data folder in the new version
        ProcessBuilder pb = new ProcessBuilder("rm", "-rf",  Paths.get(newFolderPath.toString(), "data").toString());
        pb.directory(new File(Config.getConf().cassandraPath));
        Utilities.runProcess(pb, "mv data folder");

        // Move data folder
        pb = new ProcessBuilder("mv", oldFolderPath.toString(), newFolderPath.toString());
        pb.directory(new File(Config.getConf().cassandraPath));
        Utilities.runProcess(pb, "mv data folder");

        System.out.println("Moved data folder");

       // Upgrade
       // TODO: Add retry times to check whether the upgrade is failed.
       pb = new ProcessBuilder("bin/cassandra");
       pb.directory(new File(Config.getConf().upgradeCassandraPath));
    //    Utilities.runProcess(pb, "Upgrade Cassandra");
       Process upgradeCassandraProcess = Utilities.runProcess(pb, "Upgrade Cassandra");

       while (!isCassandraReady(Config.getConf().upgradeCassandraPath)) {
           // Problem : why would the process be dead?
        //    if (!upgradeCassandraProcess.isAlive()) {
        //        // Throw a specific exception, if this is upgrade, it means we met a bug
        //        throw new CustomExceptions.systemStartFailureException(
        //                "New version cassandra start fails during" +
        //                " the upgrade process. Tt could be a bug", null);
        //    }
           try {
               Thread.sleep(500);
           } catch (InterruptedException e) {
               e.printStackTrace();
           }
       }
       System.out.println("Upgrade process success, now shut down the new version, and clean the folder");

       upgradeteardown();
       System.out.println("Upgrade process shutdown successfully");

        // Execute validation commands
        return 0;
    }

}
