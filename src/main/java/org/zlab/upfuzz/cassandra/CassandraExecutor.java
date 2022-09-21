package org.zlab.upfuzz.cassandra;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.CommandSequence;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.cassandra.CassandraCqlshDaemon.CqlshPacket;
import org.zlab.upfuzz.fuzzingengine.AgentServerSocket;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.fuzzingengine.executor.Executor;
import org.zlab.upfuzz.fuzzingengine.testplan.TestPlan;
import org.zlab.upfuzz.utils.Pair;
import org.zlab.upfuzz.utils.Utilities;

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

        timestamp = System.currentTimeMillis();

        // TODO: GC the old coverage since we already get the overall coverage.
        agentStore = new HashMap<>();
        agentHandler = new HashMap<>();
        sessionGroup = new HashMap<>();
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
        try {
            agentSocket = new AgentServerSocket(this);
            agentSocket.setDaemon(true);
            agentSocket.start();
            agentPort = agentSocket.getPort();
        } catch (Exception e) {
            logger.error(e);
            System.exit(1);
        }

        dockerCluster = new CassandraDockerCluster(
                this, Config.getConf().originalVersion,
                Config.getConf().nodeNum);

        try {
            dockerCluster.build();
        } catch (Exception e) {
            logger.error("docker cluster cannot build with exception: ", e);
            System.exit(1);
        }

        // May change classToIns according to the system...
        logger.info("[Old Version] Cassandra Start...");

        // What should we do if the docker cluster start up throws an exception?
        try {
            int ret = dockerCluster.start();
            if (ret != 0) {
                logger.error("cassandra " + executorID + " failed to started");
                System.exit(1);
            }
        } catch (Exception e) {
            // TODO: try to clear the state and restart
            logger.error("docker cluster start up failed", e);
        }

        logger.info("cassandra " + executorID + " started");
        cqlsh = ((CassandraDocker) dockerCluster.getDocker(0)).cqlsh;
        logger.info("cqlsh daemon connected");
    }

    @Override
    public void teardown() {
        dockerCluster.teardown();
    }

    @Override
    public void upgradeTeardown() {
    }

    public Pair<CommandSequence, CommandSequence> prepareCommandSequence() {
        CommandSequence commandSequence = null;
        CommandSequence validationCommandSequence = null;
        try {
            commandSequence = CommandSequence.generateSequence(
                    CassandraCommands.cassandraCommandPool.commandClassList,
                    CassandraCommands.cassandraCommandPool.createCommandClassList,
                    CassandraState.class, null);
            // TODO: If it's generating read with a initial state, no need to
            // generate with createTable...
            validationCommandSequence = CommandSequence.generateSequence(
                    CassandraCommands.cassandraCommandPool.readCommandClassList,
                    null,
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
                    CassandraCommands.cassandraCommandPool.readCommandClassList,
                    null,
                    CassandraState.class, state);
        } catch (NoSuchMethodException | InvocationTargetException
                | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return validationCommandSequence;
    }

    // We only execute commmands within the cqlsh shell
    @Override
    public List<String> executeCommands(List<String> commandList) {
        // TODO: Use Event here, since not all commands are executed
        // through the cqlsh shell

        // commandSequence = prepareCommandSequence();
        List<String> ret = new LinkedList<>();
        try {
            // TODO: support sending commands to different nodes
            if (cqlsh == null)
                cqlsh = ((CassandraDocker) dockerCluster.getDocker(0)).cqlsh;
            for (String cmd : commandList) {
                logger.trace("cqlsh execute: " + cmd);
                long startTime = System.currentTimeMillis();
                CqlshPacket cp = cqlsh.execute(cmd);
                long endTime = System.currentTimeMillis();

                ret.add(cp.message);
            }
            // When the first tp is executed, inject a fault here for testing
            // method
            // Set the nodeNum in cluster as 2
            // dockerCluster.partition(0, 1);

            // if (Config.getConf().nodeNum > 1 && !partition_test) {
            // // kill a container
            // dockerCluster.killContainer(1);
            // }

            // cqlsh.destroy();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return ret;
    }

    @Override
    public void execShellCommand(Command command) {

    }

    @Override
    public void execNormalCommand(Command command) {

    }

    public List<String> newVersionExecuteCommands(List<String> commandList) {
        List<String> ret = new LinkedList<>();
        try {
            // TODO: Put the cqlsh daemon outside, so that one instance for one
            // cqlsh daemon

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
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return ret;
    }

    @Override
    public int saveSnapshot() {
        // Do nothing when using docker-compose
        return 0;
    }

    @Override
    public int moveSnapShot() {
        return 0;
    }

    @Override
    public boolean upgradeTest() {
        try {
            dockerCluster.upgrade();
            this.cqlsh = ((CassandraDocker) dockerCluster.getDocker(0)).cqlsh;
        } catch (Exception e) {
            logger.error("Failed to connect to upgraded cassandra cluster", e);
        }
        for (Integer testId : testId2commandSequence.keySet()) {
            testId2newVersionResult.put(
                    testId, newVersionExecuteCommands(
                            testId2commandSequence.get(testId).right));
        }
        logger.info("upgrade test done");

        return true;
    }

    public Pair<Boolean, String> checkResultConsistency(List<String> oriResult,
            List<String> upResult) {
        // This could be override by each system to filter some false positive
        // Such as: the exception is the same, but the print format is different

        if (oriResult == null) {
            logger.error("original result are null!");
        }
        if (upResult == null) {
            logger.error("upgraded result are null!");
        }

        StringBuilder failureInfo = new StringBuilder("");
        if (oriResult.size() != upResult.size()) {
            failureInfo.append("The result size is different\n");
            return new Pair<>(false, failureInfo.toString());
        } else {
            boolean ret = true;
            for (int i = 0; i < oriResult.size(); i++) {
                if (oriResult.get(i).compareTo(upResult.get(i)) != 0) {

                    // SyntaxException
                    if (oriResult.get(i).contains("SyntaxException") &&
                            upResult.get(i).contains("SyntaxException")) {
                        continue;
                    }

                    // InvalidRequest
                    if (oriResult.get(i).contains("InvalidRequest") &&
                            upResult.get(i).contains("InvalidRequest")) {
                        continue;
                    }

                    if (oriResult.get(i).contains("0 rows") &&
                            upResult.get(i).contains("0 rows")) {
                        continue;
                    }

                    String errorMsg = "Result not the same at read sequence id = "
                            + i + "\n"
                            + "Old Version Result: " + oriResult.get(i) + "  "
                            + "New Version Result: " + upResult.get(i) + "\n";

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
