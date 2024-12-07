package org.zlab.upfuzz.hbase;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.zlab.upfuzz.fuzzingengine.AgentServerSocket;
import org.zlab.upfuzz.fuzzingengine.ClusterStuckException;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.fuzzingengine.executor.Executor;
import org.zlab.upfuzz.fuzzingengine.testplan.event.command.ShellCommand;
import org.zlab.upfuzz.hbase.HBaseShellDaemon.HBasePacket;
import org.zlab.upfuzz.utils.Pair;
import org.zlab.upfuzz.utils.Utilities;

public class HBaseExecutor extends Executor {

    // static final String jacocoOptions =
    // "=append=false,includes=org.apache.hadoop.*,output=dfe,address=localhost,port=6300,sessionid=";

    HBaseShellDaemon HBaseShell = null;

    public HBaseExecutor() {
        super("hbase", Config.getConf().nodeNum);

        timestamp = System.currentTimeMillis();

        agentStore = new HashMap<>();
        agentHandler = new HashMap<>();
        sessionGroup = new ConcurrentHashMap<>();

        try {
            agentSocket = new AgentServerSocket(this);
            agentSocket.setDaemon(true);
            agentSocket.start();
            agentPort = agentSocket.getPort();
        } catch (Exception e) {
            logger.error(e);
        }

        dockerCluster = new HBaseDockerCluster(this,
                Config.getConf().originalVersion,
                nodeNum, collectFormatCoverage, configPath, direction);
    }

    public HBaseExecutor(int nodeNum, boolean collectFormatCoverage,
            Path configPath, int direction) {
        super("hbase", nodeNum);

        timestamp = System.currentTimeMillis();

        this.collectFormatCoverage = collectFormatCoverage;
        this.configPath = configPath;
        this.direction = direction;

        agentStore = new HashMap<>();
        agentHandler = new HashMap<>();
        sessionGroup = new ConcurrentHashMap<>();

        try {
            agentSocket = new AgentServerSocket(this);
            agentSocket.setDaemon(true);
            agentSocket.start();
            agentPort = agentSocket.getPort();
        } catch (Exception e) {
            logger.error(e);
        }

        if (direction == 0) {
            dockerCluster = new HBaseDockerCluster(this,
                    Config.getConf().originalVersion,
                    nodeNum, collectFormatCoverage, configPath,
                    direction);
        } else {
            dockerCluster = new HBaseDockerCluster(this,
                    Config.getConf().upgradedVersion,
                    nodeNum, collectFormatCoverage, configPath,
                    direction);
        }
    }

    @Override
    public boolean startup() {
        try {
            dockerCluster.build();
        } catch (Exception e) {
            logger.error("docker cluster cannot build with exception: ", e);
            return false;
        }

        logger.info("[Old Version] HBase Start...");

        try {
            int ret = dockerCluster.start();
            if (ret != 0) {
                logger.error("HBase " + executorID + " failed to started");
                return false;
            }
        } catch (Exception e) {
            logger.error("docker cluster start up failed", e);
        }

        logger.info("HBase " + executorID + " started");
        return true;
    }

    @Override
    public String execShellCommand(ShellCommand command) {
        // execute with HBase
        String ret = "null cp message";
        if (command.getCommand().isEmpty())
            return ret;
        try {
            // Cannot perform test plan
            // We shouldn't crash nn
            int nodeIndex = 0; // NN

            assert dockerCluster.dockerStates[nodeIndex].alive;
            HBaseShell = ((HBaseDocker) dockerCluster
                    .getDocker(nodeIndex)).HBaseShell;

            long startTime = System.currentTimeMillis();
            if (Config.getConf().debug) {
                logger.trace("Execute command " + command.getCommand());
            }
            HBasePacket cp = HBaseShell
                    .execute(command.getCommand());
            long endTime = System.currentTimeMillis();

            long timeElapsed = TimeUnit.SECONDS.convert(
                    endTime - startTime, TimeUnit.MILLISECONDS);
            if (cp != null) {
                ret = cp.message;
            }
        } catch (ClusterStuckException e) {
            logger.error("ClusterStuckException occurred: " + e.getMessage(),
                    e);
            throw e;
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

        if (!Config.getConf().enableHBaseReadResultComparison)
            return new Pair<>(true, "");

        StringBuilder failureInfo = new StringBuilder();

        assert oriResult != null;
        assert upResult != null;
        if (oriResult.size() != upResult.size()) {
            failureInfo.append("The result size is different\n");
            return new Pair<>(false, failureInfo.toString());
        } else {
            boolean ret = true;
            for (int i = 0; i < oriResult.size(); i++) {
                // Mask timestamp
                String str1 = Utilities.maskTimeStampYYYYMMDD(
                        Utilities.maskTimeStampHHSS(oriResult.get(i)));
                String str2 = Utilities.maskTimeStampYYYYMMDD(
                        Utilities.maskTimeStampHHSS(upResult.get(i)));
                // HBase unique: Mask Took 0.0052 seconds
                str1 = Utilities.maskScanTime(str1);
                str2 = Utilities.maskScanTime(str2);
                // HBase unique: Ruby objects
                str1 = Utilities.maskRubyObject(str1);
                str2 = Utilities.maskRubyObject(str2);
                // Mask all spaces
                str1 = str1.replaceAll("\\s", "");
                str2 = str2.replaceAll("\\s", "");

                if (str1.compareTo(str2) != 0) {
                    // Handle FP
                    if (str1.contains("ERROR:")
                            && str2.contains("ERROR:")) {
                        continue;
                    }
                    if (str1.contains("NoSuchColumnFamilyException:") &&
                            str2.contains("NoSuchColumnFamilyException:")) {
                        continue;
                    }
                    if (str1.contains(str2) || str2.contains(str1)) {
                        continue;
                    }
                    if (str1.contains("wrong number of arguments")
                            && str2.contains("wrong number of arguments")) {
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
