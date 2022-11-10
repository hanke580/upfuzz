package org.zlab.upfuzz.hdfs;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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
        super("hdfs", Config.getConf().nodeNum);

        timestamp = System.currentTimeMillis();

        agentStore = new HashMap<>();
        agentHandler = new HashMap<>();
        sessionGroup = new ConcurrentHashMap<>();
    }

    public HdfsExecutor(int nodeNum,
            Set<String> targetSystemStates, Path configPath) {
        super("hdfs", nodeNum);

        timestamp = System.currentTimeMillis();

        this.targetSystemStates = targetSystemStates;
        this.configPath = configPath;

        agentStore = new HashMap<>();
        agentHandler = new HashMap<>();
        sessionGroup = new ConcurrentHashMap<>();
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
                nodeNum, null, configPath);

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
