package org.zlab.upfuzz.cassandra;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.zlab.upfuzz.CommandSequence;
import org.zlab.upfuzz.cassandra.CassandraCqlshDaemon.CqlshPacket;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.fuzzingengine.executor.Executor;
import org.zlab.upfuzz.utils.SystemUtil;

public class CassandraExecutor extends Executor {

    Process cassandraProcess;
    static final String jacocoOptions = "=append=false,includes=org.apache.cassandra.*,output=dfe,address=localhost,sessionid=";

    public CassandraExecutor(CommandSequence testSeq) {
        super(testSeq, "cassandra");
    }

    @Override
    public void startup() {
        ProcessBuilder cassandraProcessBuilder = new ProcessBuilder("bin/cassandra", "-f");
        Map<String, String> env = cassandraProcessBuilder.environment();
        env.put("JAVA_TOOL_OPTIONS",
                "-javaagent:" + Config.getConf().jacocoAgentPath + jacocoOptions + systemID + "-" + executorID);
        cassandraProcessBuilder.directory(new File(Config.getConf().cassandraPath));
        cassandraProcessBuilder.redirectErrorStream(true);
        cassandraProcessBuilder.redirectOutput(new File(Config.getConf().cassandraOutputFile));
        try {
            System.out.println("Executor starting cassandra");
            long startTime = System.currentTimeMillis();
            cassandraProcess = cassandraProcessBuilder.start();
            // byte[] out = cassandraProcess.getInputStream().readAllBytes();
            // BufferedReader in = new BufferedReader(new InputStreamReader(cassandraProcess.getInputStream()));
            // String line;
            // while ((line = in.readLine()) != null) {
            //     System.out.println(line);
            //     System.out.flush();
            // }
            // in.close();
            // cassandraProcess.waitFor();
            System.out.println("cassandra " + executorID + " started");
            while (!isCassandraReady()) {
                if (!cassandraProcess.isAlive()) {
                    System.out.println("cassandra process crushed\nCheck " + Config.getConf().cassandraOutputFile
                            + " for details");
                    System.exit(1);
                }
                System.out.println("Wait for " + systemID + " ready...");
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
        pb.directory(new File(Config.getConf().cassandraPath));
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
            assert cassandraProcess.isAlive() == false;
            System.out.println("cassandra " + executorID + " shutdown ok!");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public boolean isCassandraReady() {
        Process isReady;
        int ret = 0;
        try {
            isReady = SystemUtil.exec(new String[] { "bin/cqlsh", "-e", "describe cluster" },
                    Config.getConf().cassandraPath);
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
        try {
            CassandraCqlshDaemon cqlsh = new CassandraCqlshDaemon();
            for (String cmd : commandList) {
                System.out
                        .println("\n\n------------------------------------------------------------\nexecutor command:\n"
                                + cmd + "\n------------------------------------------------------------\n");
                long startTime = System.currentTimeMillis();
                CqlshPacket cp = cqlsh.execute(cmd);
                long endTime = System.currentTimeMillis();
                System.out.println("ret is: " + cp.exitValue + "\ntime: " + cp.timeUsage + "\ntime usage(network):"
                        + (endTime - startTime) / 1000. + "\n");
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return 127;
        }
        return 0;
    }
}
