package org.zlab.upfuzz.cassandra;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang3.RandomUtils;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.utils.SystemUtil;

public class CassandraCqlshDaemon {
    private int port;
    private int MAX_RETRY = 100;
    private Process cqlsh;

    public CassandraCqlshDaemon() {

    }

    private void start() throws IOException {
        boolean flag = false;
        for (int i = 0; i < MAX_RETRY; ++i) {
            port = RandomUtils.nextInt(1024, 65536);
            if (testPortAvailable(port) && testPortAvailable(port ^ 1)) {
                flag = true;
                break;
            }
        }
        if (flag) {
            cqlsh = SystemUtil.exec(
                    new String[] { "/home/yayu/Project/Upgrade-Fuzzing/cassandra/cassandra/bin/cqlsh_daemon.py" },
                    new File(Config.cassandraPath));

        } else {
            throw new IllegalStateException("Cannot fina an available port");
        }
    }

    public void execute() {

    }

    public static boolean testPortAvailable(int port) {
        Process p;
        try {
            p = SystemUtil.exec(new String[] { "netstat", "-tunlp", "|", "grep", "-P", ":" + port + "[\\s$]" },
                    new File("/"));
            int ret = p.waitFor();
            return ret == 1;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static class CqlshPacket {
        public String cmd;
        public int exitValue;
        public String message;
        public double timeUsage;

        public CqlshPacket() {
            cmd = "";
            exitValue = 0;
            message = "";
            timeUsage = -1;
        }
    }
}
