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
import org.zlab.upfuzz.CustomExceptions;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.cassandra.CassandraCqlshDaemon.CqlshPacket;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.fuzzingengine.executor.Executor;
import org.zlab.upfuzz.utils.Pair;
import org.zlab.upfuzz.utils.Utilities;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * validity procedure
 *
 * 1. execute original command sequence
 * 2. execute validation command sequence
 * 3. upgrade
 * 4. execute upgraded command sequence(NOT NOW)
 * 5. execute validation command sequence
 */
public class CassandraExecutor extends Executor {

    static Logger logger = LogManager.getLogger(CassandraExecutor.class);

    CassandraCqlshDaemon cqlsh = null;

    Process cassandraProcess;
    // static final String jacocoOptions =
    // "=append=false,includes=org.apache.cassandra.*,output=dfe,address=localhost,sessionid=";

    // Over the JVM start up option limitation.

    static final String jacocoOptions = "=append=false";

    static final String classToIns = Config.getConf().instClassFilePath;

    static final String excludes = "org.apache.cassandra.metrics.*:org.apache.cassandra.net.*:org.apache.cassandra.io.sstable.format.SSTableReader.*:org.apache.cassandra.service.*";

    public CassandraExecutor() {
        super("cassandra");
    }

    public boolean isCassandraReady(String oldSystemPath) {
        // ProcessBuilder isReadyBuilder = new ProcessBuilder();
        Process isReady;
        int ret = 0;
        try {
            isReady = Utilities.exec(
                    new String[] { "bin/cqlsh", "-e", "describe cluster" },
                    oldSystemPath);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(isReady.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                // logger.info(line);
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

        // May change classToIns according to the system...
        logger.info("[Old Version] Cassandra Start...");

        ProcessBuilder cassandraProcessBuilder = new ProcessBuilder(
                "bin/cassandra", "-f");
        Map<String, String> env = cassandraProcessBuilder.environment();
        env.put("JAVA_TOOL_OPTIONS",
                "-javaagent:" + Config.getConf().jacocoAgentPath + jacocoOptions
                        + ",includes=" + classToIns + ",excludes=" + excludes
                        + ",output=dfe,address=localhost,sessionid=" + systemID
                        + "-" + executorID + "_original");
        cassandraProcessBuilder
                .directory(new File(Config.getConf().oldSystemPath));
        cassandraProcessBuilder.redirectErrorStream(true);
        cassandraProcessBuilder.redirectOutput(
                Paths.get(Config.getConf().oldSystemPath, "logs.txt").toFile());
        try {
            long startTime = System.currentTimeMillis();
            cassandraProcess = cassandraProcessBuilder.start();
            // byte[] out = cassandraProcess.getInputStream().readAllBytes();
            // BufferedReader in = new BufferedReader(new
            // InputStreamReader(cassandraProcess.getInputStream()));
            // String line;
            // while ((line = in.readLine()) != null) {
            // logger.info(line);
            // System.out.flush();
            // }
            // in.close();
            // cassandraProcess.waitFor();
            logger.info("cassandra " + executorID + " started");
            while (!isCassandraReady(Config.getConf().oldSystemPath)) {
                if (!cassandraProcess.isAlive()) {
                    // logger.info("cassandra process crushed\nCheck " +
                    // Config.getConf().cassandraOutputFile
                    // + " for details");
                    // System.exit(1);
                    throw new CustomExceptions.systemStartFailureException(
                            "Cassandra Start fails", null);
                }
                logger.info("Wait for " + systemID + " ready...");
                Thread.sleep(2000);
            }
            long endTime = System.currentTimeMillis();
            logger.info("cassandra " + executorID + " ready.. time usage:"
                    + (endTime - startTime) / 1000. + "\n");

            cqlsh = new CassandraCqlshDaemon(Config.getConf().oldSystemPath);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void teardown() {
        ProcessBuilder pb = new ProcessBuilder("bin/nodetool", "stopdaemon");
        pb.directory(new File(Config.getConf().oldSystemPath));
        Process p;
        try {
            p = pb.start();
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                logger.info(line);
                System.out.flush();
            }
            p.waitFor();
            in.close();
            assert !cassandraProcess.isAlive();
            logger.info("cassandra " + executorID + " shutdown successfully");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        // Remove the data folder
        pb = new ProcessBuilder("rm", "-rf", "data", "logs.txt");
        pb.directory(new File(Config.getConf().oldSystemPath));
        try {
            pb.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.cqlsh.destroy();
    }

    @Override
    public void upgradeteardown() {
        ProcessBuilder pb = new ProcessBuilder("bin/nodetool", "stopdaemon");
        pb.directory(new File(Config.getConf().newSystemPath));
        Process p;
        try {
            p = pb.start();
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                // logger.info(line);
                // System.out.flush();
            }
            p.waitFor();
            in.close();
            logger.info(
                    "new cassandra " + executorID + " shutdown successfully");

            // p.wait();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        // Remove the data folder
        pb = new ProcessBuilder("rm", "-rf", "data", "logs.txt");
        pb.directory(new File(Config.getConf().newSystemPath));
        try {
            pb.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        logger.info("Upgrade folder has been removed");

        // Stop all running cassandra instances
        // pgrep -u vagrant -f cassandra | xargs kill -9
        pb = new ProcessBuilder("pgrep", "-u", "vagrant", "cassandra",
                "| xargs kill -9");
        pb.directory(new File(Config.getConf().newSystemPath));
        Utilities.runProcess(pb, "kill cassandra instances");

        this.cqlsh.destroy();
    }

    public Pair<CommandSequence, CommandSequence> prepareCommandSequence() {
        CommandSequence commandSequence = null;
        CommandSequence validationCommandSequence = null;
        try {
            commandSequence = CommandSequence.generateSequence(
                    CassandraCommands.commandClassList,
                    CassandraCommands.createCommandClassList,
                    CassandraState.class, null);
            // TODO: If it's generating read with a initial state, no need to
            // generate with createTable...
            validationCommandSequence = CommandSequence.generateSequence(
                    CassandraCommands.readCommandClassList, null,
                    CassandraState.class, commandSequence.state);
        } catch (NoSuchMethodException | InvocationTargetException
                | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return new Pair<>(commandSequence, validationCommandSequence);
    }

    public static CommandSequence prepareValidationCommandSequence(
            State state) {
        CommandSequence validationCommandSequence = null;
        try {
            validationCommandSequence = CommandSequence.generateSequence(
                    CassandraCommands.readCommandClassList, null,
                    CassandraState.class, state);
        } catch (NoSuchMethodException | InvocationTargetException
                | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return validationCommandSequence;
    }

    @Override
    public List<String> executeCommands(List<String> commandList) {
        // commandSequence = prepareCommandSequence();
        List<String> ret = new LinkedList<>();
        try {
            if (cqlsh == null)
                cqlsh = new CassandraCqlshDaemon(
                        Config.getConf().oldSystemPath);
            for (String cmd : commandList) {
                // System.out.println(
                // "\n\n------------------------------------------------------------
                // executor command:\n"
                // + cmd
                // +
                // "\n------------------------------------------------------------\n");
                long startTime = System.currentTimeMillis();
                CqlshPacket cp = cqlsh.execute(cmd);
                long endTime = System.currentTimeMillis();

                ret.add(cp.message);
                // logger.info("ret is: " + cp.exitValue + "\ntime: " +
                // cp.timeUsage + "\ntime usage(network):"
                // + (endTime - startTime) / 1000. + "\n");
            }
            // cqlsh.destroy();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
        return ret;
    }

    public List<String> newVersionExecuteCommands(List<String> commandList) {
        List<String> ret = new LinkedList<>();
        try {
            // TODO: Put the cqlsh daemon outside, so that one instance for one
            // cqlsh daemon

            if (cqlsh == null)
                cqlsh = new CassandraCqlshDaemon(
                        Config.getConf().newSystemPath);
            for (String cmd : commandList) {
                // System.out
                // .println("\n\n------------------------------------------------------------\nexecutor
                // command:\n"
                // + cmd +
                // "\n------------------------------------------------------------\n");
                long startTime = System.currentTimeMillis();
                CqlshPacket cp = cqlsh.execute(cmd);
                long endTime = System.currentTimeMillis();

                ret.add(cp.message);
                // logger.info("ret is: " + cp.exitValue + "\ntime: " +
                // cp.timeUsage + "\ntime usage(network):"
                // + (endTime - startTime) / 1000. + "\n");
            }
            // cqlsh.destroy();
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
        pb.directory(new File(Config.getConf().oldSystemPath));
        Utilities.runProcess(pb, "[Executor] Old Version System Flush");
        return 0;
    }

    @Override
    public int moveSnapShot() {
        // Copy the data dir
        Path oldFolderPath = Paths.get(Config.getConf().oldSystemPath, "data");
        Path newFolderPath = Paths.get(Config.getConf().newSystemPath);

        ProcessBuilder pb = new ProcessBuilder("cp", "-r",
                oldFolderPath.toString(), newFolderPath.toString());
        pb.directory(new File(Config.getConf().oldSystemPath));
        Utilities.runProcess(pb,
                "[Executor] Copy the data folder to the new version");
        return 0;
    }

    @Override
    public boolean upgradeTest() {
        // Upgrade Startup
        ProcessBuilder pb = new ProcessBuilder("bin/cassandra");
        pb.directory(new File(Config.getConf().newSystemPath));
        pb.redirectOutput(
                Paths.get(Config.getConf().newSystemPath, "logs.txt").toFile());
        // long startTime = System.currentTimeMillis();
        Utilities.runProcess(pb, "Upgrade Cassandra");

        // Add a retry time here
        boolean started = false;
        int RETRY_START_UPGRADE = 25;
        for (int i = 0; i < RETRY_START_UPGRADE; i++) {
            if (isCassandraReady(Config.getConf().newSystemPath)) {
                started = true;
                break;
            }
            try {
                logger.info("Upgrade System Waiting...");
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (!started) {
            // Retry the upgrade, clear the folder, kill the hang process
            logger.info("[FAILURE LOG] New version cannot start");
            return false;
        }

        try {
            this.cqlsh = new CassandraCqlshDaemon(
                    Config.getConf().newSystemPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (Integer testId : testId2commandSequence.keySet()) {
            testId2newVersionResult.put(testId, newVersionExecuteCommands(
                    testId2commandSequence.get(testId).right));
        }

        return true;
    }

    public Pair<Boolean, String> checkResultConsistency(List<String> oriResult,
            List<String> upResult) {
        // This could be override by each system to filter some false positive
        // Such as: the exception is the same, but the print format is different

        StringBuilder failureInfo = new StringBuilder("");
        if (oriResult.size() != upResult.size()) {
            failureInfo.append("The result size is different\n");
            return new Pair<>(false, failureInfo.toString());
        } else {
            boolean ret = true;
            for (int i = 0; i < oriResult.size(); i++) {
                if (oriResult.get(i).compareTo(upResult.get(i)) != 0) {

                    // SyntaxException
                    if (oriResult.get(i).contains("SyntaxException")
                            && upResult.get(i).contains("SyntaxException")) {
                        continue;
                    }

                    // InvalidRequest
                    if (oriResult.get(i).contains("InvalidRequest")
                            && upResult.get(i).contains("InvalidRequest")) {
                        continue;
                    }

                    if (oriResult.get(i).contains("0 rows")
                            && upResult.get(i).contains("0 rows")) {
                        continue;
                    }

                    String errorMsg = "Result not the same at read sequence id = "
                            + i + "\n" + "Old Version Result: "
                            + oriResult.get(i) + "  " + "New Version Result: "
                            + upResult.get(i) + "\n";

                    failureInfo.append(errorMsg);
                    ret = false;
                }
            }
            return new Pair<>(ret, failureInfo.toString());
        }
    }

    @Override
    public void upgrade() throws Exception {
        // TODO Auto-generated method stub
    }
}
