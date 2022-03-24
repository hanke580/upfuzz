package org.zlab.upfuzz.cassandra;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

import org.zlab.upfuzz.CommandSequence;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.fuzzingengine.executor.Executor;

public class CassandraExecutor extends Executor {

    Process cassandraProcess;

    public CassandraExecutor(Config conf, CommandSequence testSeq) {
        super(conf, testSeq, "cassandra");
    }

    @Override
    public void startup() {
        ProcessBuilder pb = new ProcessBuilder("bin/cassandra");
        Map<String, String> env = pb.environment();
        env.put("JAVA_TOOL_OPTIONS",
                "-javaagent:/home/yayu/Project/Upgrade-Fuzzing/jacoco/org.jacoco.agent.rt/target/org.jacoco.agent.rt-0.8.8-SNAPSHOT-all.jar=append=false,includes=org.apache.cassandra.*,output=dfe,address=localhost,sessionid="
                        + systemID + "-" + executorID + "-null");
        // env.put("VAR1", "myValue");
        // env.remove("OTHERVAR");
        // env.put("VAR2", env.get("VAR1") + "suffix");
        pb.directory(new File(conf.cassandraPath));
        // pb.redirectErrorStream(true);
        // pb.redirectOutput(Redirect.appendTo(log));
        try {
            System.out.println("cassandra start");
            cassandraProcess = pb.start();
            // cassandraProcess.d
            BufferedReader in = new BufferedReader(new InputStreamReader(cassandraProcess.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                // System.out.println(line);
                // System.out.flush();
            }
            cassandraProcess.waitFor();
            System.out.println("cassandra " + executorID + " start");
            in.close();

            Thread.sleep(8000);

        } catch (IOException | InterruptedException e) {
            // TODO Auto-generated catch block
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
            System.out.println("cassandra " + executorID + " shutdown ok!");

            in.close();

            // p.wait();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
