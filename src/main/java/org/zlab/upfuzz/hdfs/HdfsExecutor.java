package org.zlab.upfuzz.hdfs;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.zlab.upfuzz.CommandSequence;
import org.zlab.upfuzz.CustomExceptions;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.fuzzingengine.executor.Executor;
import org.zlab.upfuzz.utils.Utilities;

public class HdfsExecutor extends Executor {

    static final String jacocoOptions = "=append=false,includes=org.apache.hadoop.*,output=dfe,address=localhost,sessionid=";
    Process hdfsProcess;

    protected HdfsExecutor(CommandSequence commandSequence, CommandSequence validationCommandSequence) {
        super(commandSequence, validationCommandSequence, "hadoop");
    }

    @Override
    public boolean upgradeTest() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isHdfsReady(String hdfsPath) {
        ProcessBuilder isReadyBuilder = new ProcessBuilder();
        Process isReady;
        int ret = 0;
        try {
            isReady = Utilities.exec(new String[] { "bin/hdfs", "dfsadmin", "-report" }, hdfsPath);
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
        System.out.println("start hadoop...");
        ProcessBuilder hdfsProcessBuilder = new ProcessBuilder("sbin/start-dfs.sh");
        Map<String, String> env = hdfsProcessBuilder.environment();
        env.put("JAVA_TOOL_OPTIONS",
                "-javaagent:" + Config.getConf().jacocoAgentPath + jacocoOptions + systemID + "-" + executorID);
        hdfsProcessBuilder.directory(new File(Config.getConf().oldSystemPath));
        hdfsProcessBuilder.redirectErrorStream(true);
        hdfsProcessBuilder.redirectOutput(Paths.get(Config.getConf().oldSystemPath, "logs.txt").toFile());

        try {
            System.out.println("Executor starting hdfs");
            long startTime = System.currentTimeMillis();
            hdfsProcess = hdfsProcessBuilder.start();
            // byte[] out = hdfsProcess.getInputStream().readAllBytes();
            // BufferedReader in = new BufferedReader(new InputStreamReader(hdfsProcess.getInputStream()));
            // String line;
            // while ((line = in.readLine()) != null) {
            //     System.out.println(line);
            //     System.out.flush();
            // }
            // in.close();
            // hdfsProcess.waitFor();
            System.out.println("hdfs " + executorID + " started");
            while (!isHdfsReady(Config.getConf().oldSystemPath)) {
                if (!hdfsProcess.isAlive()) {
                    // System.out.println("hdfs process crushed\nCheck " + Config.getConf().hdfsOutputFile
                    //         + " for details");
                    // System.exit(1);
                    throw new CustomExceptions.systemStartFailureException("Hdfs Start fails", null);
                }
                System.out.println("Wait for " + systemID + " ready...");
                Thread.sleep(1000);
            }
            long endTime = System.currentTimeMillis();
            System.out.println("hdfs " + executorID + " ready \n time usage:" + (endTime - startTime) / 1000. + "\n");
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
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
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
    public List<String> executeCommands(CommandSequence commandSequence) {
        List<String> commandList = commandSequence.getCommandStringList();
        List<String> ret = new LinkedList<>();
        for (String cmd : commandList) {
            ProcessBuilder pb = new ProcessBuilder("bin/hdfs", cmd);
            pb.directory(new File(Config.getConf().oldSystemPath));
            Process p;
            try {
                p = pb.start();
                p.wait();
            } catch (IOException | InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return null;
    }

}
