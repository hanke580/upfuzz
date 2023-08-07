package org.zlab.upfuzz.cassandra;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.zlab.upfuzz.cassandra.CassandraCqlshDaemon.CqlshPacket;
import org.zlab.upfuzz.fuzzingengine.AgentServerSocket;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.fuzzingengine.executor.Executor;
import org.zlab.upfuzz.fuzzingengine.testplan.event.command.ShellCommand;
import org.zlab.upfuzz.utils.Pair;

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
        super("cassandra", Config.getConf().nodeNum);

        timestamp = System.currentTimeMillis();

        agentStore = new HashMap<>();
        agentHandler = new HashMap<>();
        sessionGroup = new ConcurrentHashMap<>();

        dockerCluster = new CassandraDockerCluster(
                this, Config.getConf().originalVersion,
                nodeNum, targetSystemStates, configPath, exportComposeOnly);
    }

    public CassandraExecutor(int nodeNum) {
        super("cassandra", nodeNum);

        timestamp = System.currentTimeMillis();
        agentStore = new HashMap<>();
        agentHandler = new HashMap<>();
        sessionGroup = new ConcurrentHashMap<>();
    }

    public CassandraExecutor(int nodeNum,
            Set<String> targetSystemStates, Path configPath,
            Boolean exportComposeOnly) {
        super("cassandra", nodeNum);

        timestamp = System.currentTimeMillis();
        this.targetSystemStates = targetSystemStates;
        this.configPath = configPath;
        this.exportComposeOnly = exportComposeOnly;
        agentStore = new HashMap<>();
        agentHandler = new HashMap<>();
        sessionGroup = new ConcurrentHashMap<>();
    }

    @Override
    public boolean startup() {
        try {
            agentSocket = new AgentServerSocket(this);
            agentSocket.setDaemon(true);
            agentSocket.start();
            agentPort = agentSocket.getPort();
        } catch (Exception e) {
            logger.error(e);
            return false;
        }

        dockerCluster = new CassandraDockerCluster(
                this, Config.getConf().originalVersion,
                nodeNum, targetSystemStates, configPath, exportComposeOnly);

        try {
            dockerCluster.build();
        } catch (Exception e) {
            logger.error("docker cluster cannot build with exception: ", e);
            return false;
        }

        if (exportComposeOnly) {
            dockerCluster.start();
            return true;
        }

        // May change classToIns according to the system...
        logger.info("[Old Version] Cassandra Start...");

        // What should we do if the docker cluster start up throws an exception?
        try {
            int ret = dockerCluster.start();
            if (ret != 0) {
                logger.error("cassandra " + executorID + " failed to started");
                return false;
            }
        } catch (Exception e) {
            logger.error("docker cluster start up failed", e);
            return false;
        }
        logger.info("cassandra " + executorID + " started");
        cqlsh = ((CassandraDocker) dockerCluster.getDocker(0)).cqlsh;
        logger.info("cqlsh daemon connected");
        return true;
    }

    @Override
    public void teardown() {
        dockerCluster.teardown();
    }

    @Override
    public void upgradeTeardown() {
    }

    @Override
    public String execShellCommand(ShellCommand command) {
        String ret = "null cp message";
        if (command.getCommand().isEmpty())
            return ret;
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

            long timeElapsed = endTime - startTime;
            if (Config.getConf().debug) {
                logger.debug(String.format(
                        "Command is sent to node[%d], exec time: %dms",
                        cqlshNodeIndex, timeElapsed));
                if (cp != null)
                    logger.debug(String.format(
                            "command = {%s}, result = {%s}, error = {%s}, exitValue = {%d}",
                            command.getCommand(), cp.message, cp.error,
                            cp.exitValue));
            }

            if (cp != null) {
                if (cp.message.isEmpty())
                    ret = cp.error;
                else
                    ret = cp.message;
            }
        } catch (Exception e) {
            logger.error(e);
            ret = "shell daemon execution problem " + e;
        }
        return ret;
    }

    public Pair<Boolean, String> checkResultConsistency(List<String> oriResult,
            List<String> upResult, boolean compareOldAndNew) {
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
                // What should we do if
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

                    String errorMsg = "Result inconsistency at read id: " + i
                            + "\n";
                    if (compareOldAndNew) {
                        errorMsg += "Old Version Result: "
                                + oriResult.get(i).strip()
                                + "\n"
                                + "New Version Result: "
                                + upResult.get(i).strip()
                                + "\n";
                    } else {
                        errorMsg += "Full Stop Result:\n"
                                + oriResult.get(i).strip()
                                + "\n"
                                + "Rolling Upgrade Result:\n"
                                + upResult.get(i).strip()
                                + "\n";
                    }
                    failureInfo.append(errorMsg);
                    ret = false;
                }
            }
            return new Pair<>(ret, failureInfo.toString());
        }
    }

}
