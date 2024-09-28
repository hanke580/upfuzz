package org.zlab.upfuzz.cassandra;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.zlab.upfuzz.cassandra.CassandraCqlshDaemon.CqlshPacket;
import org.zlab.upfuzz.fuzzingengine.AgentServerSocket;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.fuzzingengine.executor.Executor;
import org.zlab.upfuzz.fuzzingengine.testplan.event.command.ShellCommand;
import org.zlab.upfuzz.utils.Pair;

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
                nodeNum, collectFormatCoverage, configPath,
                direction);
    }

    public CassandraExecutor(int nodeNum, boolean collectFormatCoverage,
            Path configPath, int direction) {
        super("cassandra", nodeNum);
        timestamp = System.currentTimeMillis();

        this.collectFormatCoverage = collectFormatCoverage;
        this.configPath = configPath;
        this.direction = direction;
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

        logger.info("[HKLOG] executor direction: " + direction);
        if (direction == 0) {
            logger.info("[HKLOG] Docker Cluster startup, original version: "
                    + Config.getConf().originalVersion);
            dockerCluster = new CassandraDockerCluster(
                    this, Config.getConf().originalVersion,
                    nodeNum, collectFormatCoverage,
                    configPath, direction);
        } else {
            logger.info("[HKLOG] Docker Cluster startup, upgraded version: "
                    + Config.getConf().upgradedVersion);
            dockerCluster = new CassandraDockerCluster(
                    this, Config.getConf().upgradedVersion,
                    nodeNum, collectFormatCoverage,
                    configPath, direction);
        }

        try {
            dockerCluster.build();
        } catch (Exception e) {
            logger.error("docker cluster cannot build with exception: ", e);
            return false;
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
        cqlsh = ((CassandraDocker) dockerCluster.getDocker(0)).cqlsh;
        logger.info(
                "cassandra " + executorID + " started, cqlsh daemon connected");
        return true;
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
            int cqlshNodeIndex = 0;
            for (int i = 0; i < dockerCluster.nodeNum; i++) {
                if (dockerCluster.dockerStates[i].alive) {
                    cqlsh = ((CassandraDocker) dockerCluster
                            .getDocker(i)).cqlsh;
                    cqlshNodeIndex = i;
                    break;
                }
            }
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
        assert oriResult != null;
        assert upResult != null;
        if (oriResult.size() != upResult.size()) {
            failureInfo.append("The result size is different\n");
            return new Pair<>(false, failureInfo.toString());
        } else {
            boolean ret = true;
            for (int i = 0; i < oriResult.size(); i++) {
                String ori = oriResult.get(i);
                String up = upResult.get(i);

                if (ori.compareTo(up) != 0) {
                    // SyntaxException
                    if (ori.contains("SyntaxException") &&
                            up.contains("SyntaxException")) {
                        continue;
                    }

                    // InvalidRequest
                    if (ori.contains("InvalidRequest") &&
                            up.contains("InvalidRequest")) {
                        continue;
                    }

                    if (ori.contains("0 rows") &&
                            up.contains("0 rows")) {
                        continue;
                    }

                    String errorMsg;
                    if (((ori.contains("InvalidRequest")) &&
                            !(up.contains("InvalidRequest")))
                            || ((up.contains("InvalidRequest")) &&
                                    !(ori
                                            .contains("InvalidRequest")))) {
                        errorMsg = "Insignificant Result inconsistency at read id: "
                                + i
                                + "\n";
                    } else {
                        errorMsg = "Result inconsistency at read id: " + i
                                + "\n";
                    }
                    if (compareOldAndNew) {
                        errorMsg += "Old Version Result: "
                                + ori.strip()
                                + "\n"
                                + "New Version Result: "
                                + up.strip()
                                + "\n";
                    } else {
                        errorMsg += "Full Stop Result:\n"
                                + ori.strip()
                                + "\n"
                                + "Rolling Upgrade Result:\n"
                                + up.strip()
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
