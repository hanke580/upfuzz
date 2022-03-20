package org.zlab.upfuzz.cassandra;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.zlab.upfuzz.CommandSequence;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.fuzzingengine.executor.Executor;

public class CassandraExecutor extends Executor {

    Process cassandraProcess;

    public CassandraExecutor(Config conf, CommandSequence testSeq) {
        super(conf, testSeq);
    }

    @Override
    public void startup() {
        ProcessBuilder pb = new ProcessBuilder("bin/cassandra");
        Map<String, String> env = pb.environment();
        env.put("JAVA_TOOL_OPTIONS",
                "-javaagent:/home/yayu/Project/Upgrade-Fuzzing/jacoco/org.jacoco.agent.rt/target/org.jacoco.agent.rt-0.8.8-SNAPSHOT-all.jar=append=false,includes=org.apache.hadoop.*,output=dfe,address=localhost,sessionid=cassandra-1.0-1");
        // env.put("VAR1", "myValue");
        // env.remove("OTHERVAR");
        // env.put("VAR2", env.get("VAR1") + "suffix");
        pb.directory(new File(conf.cassandraPath));
        // pb.redirectErrorStream(true);
        // pb.redirectOutput(Redirect.appendTo(log));
        try {
            System.out.println("cassandra start");
            cassandraProcess = pb.start();
            Thread.sleep(10000);

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
            p.wait();
            System.out.println("cassandra shutdowm");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
