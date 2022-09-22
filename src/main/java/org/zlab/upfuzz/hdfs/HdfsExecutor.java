package org.zlab.upfuzz.hdfs;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.codehaus.plexus.util.FileUtils;
import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.CustomExceptions;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.fuzzingengine.executor.Executor;
import org.zlab.upfuzz.fuzzingengine.testplan.event.command.ShellCommand;
import org.zlab.upfuzz.utils.Utilities;

public class HdfsExecutor extends Executor {

    static final String jacocoOptions = "=append=false,includes=org.apache.hadoop.*,output=dfe,address=localhost,port=6300,sessionid=";
    Process hdfsProcess;
    Process hdfsDN;
    Process hdfsSNN;

    public HdfsExecutor() {
        super("hadoop");
    }

    @Override
    public boolean upgradeTest() {
        try {
            System.out.println("start hadoop...");
            ProcessBuilder hdfsProcessBuilder = new ProcessBuilder(
                    "sbin/start-dfs.sh");
            Map<String, String> env = hdfsProcessBuilder.environment();
            env.put("JAVA_TOOL_OPTIONS",
                    "-javaagent:" + Config.getConf().jacocoAgentPath +
                            jacocoOptions + systemID + "-" + executorID +
                            "_upgraded");
            hdfsProcessBuilder.directory(
                    new File(Config.getConf().newSystemPath));
            hdfsProcessBuilder.redirectErrorStream(true);
            hdfsProcessBuilder.redirectOutput(
                    Paths.get(Config.getConf().newSystemPath, "logs.txt")
                            .toFile());

            System.out.println("Executor starting hdfs");
            long startTime = System.currentTimeMillis();
            hdfsProcess = hdfsProcessBuilder.start();
            // byte[] out = hdfsProcess.getInputStream().readAllBytes();
            // BufferedReader in = new BufferedReader(new
            // InputStreamReader(hdfsProcess.getInputStream()));
            // String line;
            // while ((line = in.readLine()) != null) {
            // System.out.println(line);
            // System.out.flush();
            // }
            // in.close();
            // hdfsProcess.waitFor();
            System.out.println("hdfs " + executorID + " started");
            while (!isHdfsReady(Config.getConf().newSystemPath)) {
                if (!hdfsProcess.isAlive()) {
                    // System.out.println("hdfs process crushed\nCheck " +
                    // Config.getConf().hdfsOutputFile
                    // + " for details");
                    // System.exit(1);
                    throw new CustomExceptions.systemStartFailureException(
                            "Hdfs Start fails", null);
                }
                System.out.println("Wait for " + systemID + " ready...");
                Thread.sleep(1000);
            }
            long endTime = System.currentTimeMillis();
            System.out.println("hdfs " + executorID + " ready \n time usage:" +
                    (endTime - startTime) / 1000. + "\n");

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return false;
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
        stopDfs();

        int ret = 0;
        System.out.println("hadoop format disk...");
        try {
            FileUtils.deleteDirectory(Config.getConf().dataDir);
            ProcessBuilder hdfsFormatProcessBuilder = new ProcessBuilder(
                    "bin/hdfs", "namenode", "-format");
            hdfsFormatProcessBuilder.redirectErrorStream(true);
            hdfsFormatProcessBuilder.directory(
                    new File(Config.getConf().oldSystemPath));
            hdfsFormatProcessBuilder.redirectOutput(
                    Paths.get(Config.getConf().oldSystemPath, "format_logs.txt")
                            .toFile());

            Process hdfsFormatProcess = hdfsFormatProcessBuilder.start();

            // hdfsFormatProcess.waitFor(1000, TimeUnit.MILLISECONDS);
            // byte[] bytes = new byte[65536];
            // int n = hdfsFormatProcess.getInputStream().read(bytes);
            // while (n != -1) {
            // System.out.println(
            // "read n bytes: " + n + "\n" + new String(bytes, 0, n));
            // n = hdfsFormatProcess.getInputStream().read(bytes);
            // }
            // String formatMessage = Utilities.readProcess(hdfsFormatProcess);
            // System.out.println("format messsage: " + formatMessage);
            // hdfsFormatProcess.getOutputStream().write("Y\n".getBytes());
            ret = hdfsFormatProcess.waitFor();
            System.out.println("format result: " + ret);
            if (ret != 0) {
                throw new CustomExceptions.systemStartFailureException(
                        "hdfs format exception", null);
            }

            System.out.println("start hadoop...");
            ProcessBuilder hdfsProcessBuilder = new ProcessBuilder("bin/hdfs",
                    "--daemon", "start", "namenode");
            ProcessBuilder hdfsDNBuilder = new ProcessBuilder("bin/hdfs",
                    "--daemon", "start", "datanode");
            ProcessBuilder hdfsSNNBuilder = new ProcessBuilder(
                    "bin/hdfs", "--daemon", "start", "secondarynamenode");
            // "sbin/start-dfs.sh");
            String hdfsJacocoOption = "-javaagent:"
                    + Config.getConf().jacocoAgentPath +
                    jacocoOptions + systemID + "-" + executorID + "_original";
            String path = Config.getConf().oldSystemPath;
            setupProcess(hdfsProcessBuilder, path, hdfsJacocoOption, "NN.log");
            setupProcess(hdfsDNBuilder, path, hdfsJacocoOption, "DN.log");
            setupProcess(hdfsSNNBuilder, path, hdfsJacocoOption, "SNN.log");

            System.out.println("Executor starting hdfs");
            long startTime = System.currentTimeMillis();
            hdfsProcess = hdfsProcessBuilder.start();
            hdfsDN = hdfsDNBuilder.start();
            hdfsSNN = hdfsSNNBuilder.start();
            // byte[] out = hdfsProcess.getInputStream().readAllBytes();
            // BufferedReader in = new BufferedReader(new
            // InputStreamReader(hdfsProcess.getInputStream()));
            // String line;
            // while ((line = in.readLine()) != null) {
            // System.out.println(line);
            // System.out.flush();
            // }
            // in.close();
            // hdfsProcess.waitFor();
            System.out.println("hdfs " + executorID + " started");
            while (!isHdfsReady(Config.getConf().oldSystemPath)) {
                // if (!hdfsProcess.isAlive()) {
                // // System.out.println("hdfs process crushed\nCheck " +
                // // Config.getConf().hdfsOutputFile
                // // + " for details");
                // // System.exit(1);
                // throw new CustomExceptions.systemStartFailureException(
                // "Hdfs Start fails", null);
                // }
                System.out.println("Wait for " + systemID + " ready...");
                Thread.sleep(1000);
            }
            long endTime = System.currentTimeMillis();
            System.out.println("hdfs " + executorID + " ready \n time usage:" +
                    (endTime - startTime) / 1000. + "\n");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void teardown() {
        ProcessBuilder pb = new ProcessBuilder("sbin/stop-dfs.sh");
        pb.directory(new File(Config.getConf().oldSystemPath));
        Process p;
        try {
            p = pb.start();
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println(line);
                System.out.flush();
            }
            p.waitFor();
            in.close();
            System.out.println("hdfs " + executorID + " shutdown ok!");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void upgradeTeardown() {
    }

    @Override
    public List<String> executeCommands(List<String> commandList) {
        List<String> ret = new LinkedList<>();
        for (String cmd : commandList) {
            System.out.println("cmd: " + cmd);
            try {
                Process p = Utilities.exec(new String[] { "bin/hdfs", cmd },
                        Config.getConf().oldSystemPath);
                p.waitFor();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    @Override
    public String execShellCommand(ShellCommand command) {
        return null;
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
    public void upgrade() throws IOException, InterruptedException {

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

    @Override
    public int moveSnapShot() {
        // TODO Auto-generated method stub
        return 0;
    }
}
