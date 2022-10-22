package org.zlab.upfuzz.cassandra;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.CommandSequence;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.cassandra.CassandraCqlshDaemon.CqlshPacket;
import org.zlab.upfuzz.fuzzingengine.AgentServerSocket;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.fuzzingengine.executor.Executor;
import org.zlab.upfuzz.fuzzingengine.testplan.event.command.ShellCommand;
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
    static final String jacocoOptions = "=append=false";
    static final String classToIns = Config.getConf().instClassFilePath;
    static final String excludes = "org.apache.cassandra.metrics.*:org.apache.cassandra.net.*:org.apache.cassandra.io.sstable.format.SSTableReader.*:org.apache.cassandra.service.*";

    public CassandraExecutor() {
        super("cassandra");

        timestamp = System.currentTimeMillis();

        agentStore = new HashMap<>();
        agentHandler = new HashMap<>();
        sessionGroup = new ConcurrentHashMap<>();

        this.nodeNum = Config.getConf().nodeNum; // Using default value in the
                                                 // configuration
    }

    public CassandraExecutor(int nodeNum) {
        super("cassandra");

        timestamp = System.currentTimeMillis();

        agentStore = new HashMap<>();
        agentHandler = new HashMap<>();
        sessionGroup = new ConcurrentHashMap<>();

        this.nodeNum = nodeNum;
    }

    public CassandraExecutor(int nodeNum,
            Set<String> targetSystemStates) {
        super("cassandra");

        timestamp = System.currentTimeMillis();
        this.nodeNum = nodeNum;
        this.targetSystemStates = targetSystemStates;

        agentStore = new HashMap<>();
        agentHandler = new HashMap<>();
        sessionGroup = new ConcurrentHashMap<>();

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
                nodeNum, targetSystemStates);

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

    @Override
    public List<String> executeCommands(List<String> commandList) {
        // TODO: Use Event here, since not all commands are executed
        List<String> ret = new LinkedList<>();
        for (String command : commandList) {
            ret.add(execShellCommand(new ShellCommand(command)));
        }
        return ret;
    }

    @Override
    public String execShellCommand(ShellCommand command) {
        String ret;
        try {
            // We update the cqlsh each time
            // (1) Try to find a working cqlsh
            // (2) When the cqlsh daemon crash, we catch this
            // exception, log it's test plan, report to the
            // server, and keep testing
            // TODO: If the cqlsh daemon crash
            int cqlshNodeIndex = 0;
            for (int i = 0; i < dockerCluster.nodeNum; i++) {
                if (dockerCluster.dockerStates[i].alive) {
                    cqlsh = ((CassandraDocker) dockerCluster
                            .getDocker(i)).cqlsh;
                    cqlshNodeIndex = i;
                    break;
                }
            }
            // cqlsh = ((CassandraDocker) dockerCluster.getDocker(0)).cqlsh;
            logger.trace("cqlsh execute: " + command);
            long startTime = System.currentTimeMillis();
            CqlshPacket cp = cqlsh.execute(command.getCommand());
            long endTime = System.currentTimeMillis();

            long timeElapsed = TimeUnit.SECONDS.convert(
                    endTime - startTime, TimeUnit.MILLISECONDS);
            if (Config.getConf().debug) {
                logger.info(String.format(
                        "Command is sent to node[%d], exec time: %ds",
                        cqlshNodeIndex, timeElapsed));
            }
            ret = cp.message;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return ret;
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

}
