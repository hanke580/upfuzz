package org.zlab.upfuzz.cassandra;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import org.zlab.upfuzz.CommandSequence;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.fuzzingengine.executor.Executor;
import org.zlab.upfuzz.utils.SystemUtil;

public class CassandraExecutor extends Executor {

    Process cassandraProcess;
    static final String jacocoOptions = "=append=false,includes=org.apache.cassandra.*,output=dfe,address=localhost,sessionid=";

    public CassandraExecutor(Config conf, CommandSequence testSeq) {
        super(conf, testSeq, "cassandra");
    }

    public boolean isCassandraReady() {
        ProcessBuilder isReadyBuilder = new ProcessBuilder();
        Process isReady;
        int ret = 0;
        try {
            isReady = SystemUtil.exec(new String[] { "bin/cqlsh", "-e", "describe cluster" }, conf.cassandraPath);
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
        ProcessBuilder cassandraProcessBuilder = new ProcessBuilder("bin/cassandra");
        Map<String, String> env = cassandraProcessBuilder.environment();
        env.put("JAVA_TOOL_OPTIONS",
                "-javaagent:" + Config.jacocoAgentPath + jacocoOptions + systemID + "-" + executorID);
        cassandraProcessBuilder.directory(new File(conf.cassandraPath));
        try {
            System.out.println("cassandra start");
            long startTime = System.currentTimeMillis();
            cassandraProcess = cassandraProcessBuilder.start();
            BufferedReader in = new BufferedReader(new InputStreamReader(cassandraProcess.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                // System.out.println(line);
                // System.out.flush();
            }
            cassandraProcess.waitFor();
            System.out.println("cassandra " + executorID + " start");
            in.close();
            while (!isCassandraReady()) {
                Thread.sleep(500);
            }
            long endTime = System.currentTimeMillis();
            System.out.println(
                    "cassandra " + executorID + " ready \n time usage:" + (endTime - startTime) / 1000. + "\n");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void teardown() {
        ProcessBuilder pb = new ProcessBuilder("bin/nodetool", "stopdaemon");
        pb.directory(new File(conf.cassandraPath));
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
            System.out.println("cassandra " + executorID + " shutdown ok!");

            // p.wait();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    CommandSequence prepareCommandSequence() {
        CassandraState state = new CassandraState();
        CommandSequence commandSequence = null;
        try {
            commandSequence = CommandSequence.generateSequence(CassandraCommands.commandClassList,
                    CassandraCommands.createCommandClassList, state);
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException
                | IllegalAccessException e) {
            e.printStackTrace();
        }
        return commandSequence;
    }

    @Override
    public int executeCommands() {
        commandSequence = prepareCommandSequence();
        List<String> commandList = commandSequence.getCommandStringList();
        for (String cmd : commandList) {
            int ret = 0;
            String[] cqlshCmd = new String[] { "bin/cqlsh", "-e", cmd };
            System.out.println("\n\n------------------------------------------------------------\nexecutor command:\n"
                    + String.join(" ", cqlshCmd));
            Process cqlProcess;
            try {
                cqlProcess = SystemUtil.exec(cqlshCmd, new File(conf.cassandraPath));
                long startTime = System.currentTimeMillis();
                BufferedReader in = new BufferedReader(new InputStreamReader(cqlProcess.getInputStream()));
                String line;
                while ((line = in.readLine()) != null) {
                    System.out.println(line);
                    System.out.flush();
                }
                cqlProcess.waitFor();
                in.close();
                ret = cqlProcess.exitValue();
                long endTime = System.currentTimeMillis();
                System.out.println("ret is: " + ret + "\ntime usage:" + (endTime - startTime) / 1000. + "\n");
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        return 0;
    }
}
