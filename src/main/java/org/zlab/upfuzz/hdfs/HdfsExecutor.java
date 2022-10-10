package org.zlab.upfuzz.hdfs;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.codehaus.plexus.util.FileUtils;
import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.CustomExceptions;
import org.zlab.upfuzz.cassandra.CassandraCqlshDaemon;
import org.zlab.upfuzz.cassandra.CassandraDocker;
import org.zlab.upfuzz.fuzzingengine.AgentServerSocket;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.fuzzingengine.executor.Executor;
import org.zlab.upfuzz.fuzzingengine.testplan.event.command.ShellCommand;
import org.zlab.upfuzz.utils.Pair;
import org.zlab.upfuzz.utils.Utilities;

public class HdfsExecutor extends Executor {

    // static final String jacocoOptions =
    // "=append=false,includes=org.apache.hadoop.*,output=dfe,address=localhost,port=6300,sessionid=";

    HDFSShellDaemon hdfsShell = null;

    public HdfsExecutor() {
        super("hdfs");

        timestamp = System.currentTimeMillis();

        agentStore = new HashMap<>();
        agentHandler = new HashMap<>();
        sessionGroup = new ConcurrentHashMap<>();

        this.nodeNum = Config.getConf().nodeNum;
    }

    public HdfsExecutor(int nodeNum) {
        super("hdfs");

        timestamp = System.currentTimeMillis();

        agentStore = new HashMap<>();
        agentHandler = new HashMap<>();
        sessionGroup = new ConcurrentHashMap<>();

        this.nodeNum = nodeNum;
    }

    @Override
    public boolean upgrade() {
        // Only perform upgrade
        try {
            dockerCluster.upgrade();
        } catch (Exception e) {
            logger.error("Failed to connect to upgraded cassandra cluster", e);
        }
        return true;
    }

    public boolean isHdfsReady(String hdfsPath) {
        ProcessBuilder isReadyBuilder = new ProcessBuilder();
        Process isReady;
        int ret = 0;
        try {
            isReady = Utilities.exec(
                    new String[] { "bin/hdfs", "dfsadmin", "-report" },
                    hdfsPath);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(isReady.getInputStream()));
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

    public void stopDfs() {
        try {
            Process stopDfsProcess = Utilities.exec(
                    new String[] { "sbin/stop-dfs.sh" },
                    Config.getConf().oldSystemPath);
            int ret = stopDfsProcess.waitFor();
            System.out.println("stop dfs first: " + ret);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void setupProcess(ProcessBuilder processBuilder, String path,
            String option, String logFile) {
        Map<String, String> env = processBuilder.environment();
        env.put("JAVA_TOOL_OPTIONS", option);
        processBuilder.directory(new File(path));
        processBuilder.redirectErrorStream(true);
        processBuilder.redirectOutput(Paths.get(path, "logs.txt").toFile());
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

        dockerCluster = new HdfsDockerCluster(this,
                Config.getConf().originalVersion,
                nodeNum);

        try {
            dockerCluster.build();
        } catch (Exception e) {
            logger.error("docker cluster cannot build with exception: ", e);
            System.exit(1);
        }

        logger.info("[Old Version] HDFS Start...");

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

        logger.info("hdfs " + executorID + " started");
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
        List<String> ret = new LinkedList<>();
        for (String cmd : commandList) {
            execShellCommand(new ShellCommand(cmd));
        }
        return ret;
    }

    @Override
    public String execShellCommand(ShellCommand command) {
        // execute with HDFS
        String ret;
        try {
            // Cannot perform test plan
            // We shouldn't crash nn
            int nodeIndex = 0; // NN

            assert dockerCluster.dockerStates[nodeIndex].alive;
            hdfsShell = ((HdfsDocker) dockerCluster
                    .getDocker(nodeIndex)).hdfsShell;

            // cqlsh = ((CassandraDocker) dockerCluster.getDocker(0)).cqlsh;
            logger.trace("hdfs shell execute: " + command);
            long startTime = System.currentTimeMillis();
            CassandraCqlshDaemon.CqlshPacket cp = hdfsShell
                    .execute(command.getCommand());
            long endTime = System.currentTimeMillis();

            long timeElapsed = TimeUnit.SECONDS.convert(
                    endTime - startTime, TimeUnit.MILLISECONDS);

            if (Config.getConf().debug) {
                logger.info(String.format(
                        "Command is sent to node[%d], exec time: %ds",
                        nodeIndex, timeElapsed));
            }
            ret = cp.message;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return ret;
    }

    @Override
    public void execNormalCommand(Command command) {

    }

    /**
     * 1. Prepare Rolling Upgrade
     *     1. Run "hdfs dfsadmi n -rolli ngUpgrade prepare" to create a fsimage
     * for rollback.
     *     2. Run "hdfs dfsadmi n -rolli ngUpgrade query" to check the status of
     * the rollback image. Wait and re-run the command until the "Proceed with
     * rolling upgrade" message is shown.
     *
     * (without Downtime)
     * 2. Upgrade Active and Standby NNs
     *     1. Shutdown and upgrade NN2.
     *     2. Start NN2 as standby with the "-rollingUpgrade started" option.
     *     3. Failover from NN1 to NN2 so that NN2 becomes active and NN1
     * becomes standby.
     *     4. Shutdown and upgrade NN1. 5. Start NN1 as standby with the "-rolli
     * ngUpgrade started" option.
     *
     * (with Downtime)
     * 2. Upgrade NN and SNN
     *     1. Shutdown SNN
     *     2. Shutdown and upgrade NN.
     *     3. Start NN with the "-rollingUpgrade started" option.
     *     4. Upgrade and restart SNN
     *
     * 3. Upgrade DNs
     *     1. Choose a small subset of datanodes (e.g. all datanodes under a
     * particular rack).
     *         1. Run "hdfs dfsadmi n -shutdownDatanode <DATANODE_HOST :
     * IPC_PORT> upgrade" to shutdown one of the chosen datanodes.
     *         2. Run "hdfs dfsadmi n -getDatanodeInfo <DATANODE_HOST:
     * IPC_PORT>" to check and wait for the datanode to shutdown.
     *         3. Upgrade and restart the datanode.
     *         4. Perform the above steps for all the chosen datanodes in the
     * subset in parallel.
     *     2. Repeat the above steps until all datanodes in the cluster are
     * upgraded.
     * 4. Finalize Rolling Upgrade
     *     1. Run "hdfs dfsadmin -rollingUpgrade finalize" to finalize the
     * rolling upgrade.
     * @throws InterruptedException
     */
    public void upgrade_() throws IOException, InterruptedException {

        // Prepare Rolling Upgrade

        Process prepareProcess = Utilities.exec(
                new String[] { "bin/hdfs", "dfsadmin", "-rollingUpgrade",
                        "prepare" },
                Config.getConf().oldSystemPath);
        prepareProcess.waitFor();
        // Re-run until Proceed with rolling upgrade
        while (true) {
            Process queryProcess = Utilities.exec(
                    new String[] { "bin/hdfs", "dfsadmin",
                            "-rollingUpgrade", "query" },
                    Config.getConf().oldSystemPath);

            int ret = queryProcess.waitFor();
            if (ret == 0) {
                break;
            }
        }

        // 2 upgrade NN

        Process shutdownSNN = Utilities.exec(
                new String[] { "bin/hdfs", "--daemon", "stop",
                        "secondarynamenode" },
                Config.getConf().oldSystemPath);
        shutdownSNN.waitFor();

        Process shutdownNN = Utilities.exec(
                new String[] { "bin/hdfs", "--daemon", "stop", "namenode" },
                Config.getConf().oldSystemPath);
        shutdownNN.waitFor();

        Process upgradeNN = Utilities.exec(
                new String[] { "bin/hdfs", "--daemon", "start", "namenode",
                        "-rollingUpgrade", "started" },
                Config.getConf().newSystemPath);
        upgradeNN.waitFor();

        Process upgradeSNN = Utilities.exec(
                new String[] { "bin/hdfs", "--daemon", "start",
                        "secondaynamenode" },
                Config.getConf().newSystemPath);
        upgradeSNN.waitFor();

        // 3. Upgrade DNs
        Process shutdownDN = Utilities.exec(
                new String[] { "bin/hdfs", "--daemon", "stop", "datanode" },
                Config.getConf().oldSystemPath);
        shutdownDN.waitFor();

        Process upgradeDN = Utilities.exec(
                new String[] { "bin/hdfs", "--daemon", "start", "datanode",
                        "-rollingUpgrade", "started" },
                Config.getConf().newSystemPath);
        upgradeDN.waitFor();

        // TODO Finalize Rolling Upgrade
    }

    @Override
    public int saveSnapshot() {
        // TODO Auto-generated method stub
        return 0;
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
