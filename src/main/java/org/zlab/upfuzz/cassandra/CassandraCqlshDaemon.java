package org.zlab.upfuzz.cassandra;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.file.Paths;

import com.google.gson.Gson;

import org.apache.commons.lang3.RandomUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.utils.Utilities;

public class CassandraCqlshDaemon {
    static Logger logger = LogManager.getLogger(CassandraCqlshDaemon.class);

    private int port;
    private int MAX_RETRY = 100;
    private Process cqlsh;
    private Socket socket;

    public static String cqlshPython2Script;
    public static String cqlshPython3Script;

    static {
        InputStream cqlsh_daemon2 = CassandraCqlshDaemon.class.getClassLoader()
                .getResourceAsStream("cqlsh_daemon2.py");
        InputStream cqlsh_daemon3 = CassandraCqlshDaemon.class.getClassLoader()
                .getResourceAsStream("cqlsh_daemon3.py");
        if (cqlsh_daemon2 == null) {
            System.err.println("cannot find cqlsh_daemon.py");
        }
        byte[] bytes = new byte[65536];
        int cnt;
        try {
            cnt = cqlsh_daemon2.read(bytes);
            cqlshPython2Script = new String(bytes, 0, cnt);
            cnt = cqlsh_daemon3.read(bytes);
            cqlshPython3Script = new String(bytes, 0, cnt);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public CassandraCqlshDaemon(String cassandraRoot)
            throws IOException, InterruptedException {
        String cassandraVersion = Utilities.getGitTag(cassandraRoot);
        String cqlshPythonScript = null;
        String python = null;
        // Get path, two options
        // System.out.println("cassandra version\n" + cassandraVersion);

        char majorVersion;

        try {
            majorVersion = cassandraVersion.split("-")[1].charAt(0);
        } catch (Exception e) {
            majorVersion = cassandraRoot.split("cassandra-")[1].charAt(0);
        }
        logger.debug("cassandra version: " + majorVersion);
        majorVersion = '3';

        if (majorVersion <= '3') {
            logger.info("use python2 cqlsh script");
            python = "python2";
            cqlshPythonScript = cqlshPython2Script;
        } else {
            logger.info("use python3 cqlsh script");
            python = "python3";
            cqlshPythonScript = cqlshPython3Script;
        }

        boolean flag = false;
        flag = true;
        for (int i = 0; i < MAX_RETRY; ++i) {
            port = RandomUtils.nextInt(1024, 65536);

            if (testPortAvailable(port)) {
                flag = true;
                break;
            }
        }
        if (flag) {
            // port = port & 0xFFFE;
            // System.out.println("Use port:" + port);
            File cqlshDaemonFile = Paths
                    .get(cassandraRoot, "/bin/cqlsh_daemon.py").toFile();
            if (cqlshDaemonFile.exists()) {
                Boolean res = cqlshDaemonFile.delete();
                // System.out.println("cqlsh_daemon.py exists. Delete it: " +
                // res);
            }
            BufferedWriter bw = new BufferedWriter(
                    new FileWriter(cqlshDaemonFile));
            bw.write(cqlshPythonScript.replace("__reserved_port__",
                    Integer.toString(port)));
            bw.close();

            cqlsh = Utilities.exec(
                    new String[] { python, cqlshDaemonFile.toString() },
                    new File(cassandraRoot));

            // byte[] bytes = new byte[102400];
            // int cnt1 = cqlsh.getInputStream().read(bytes);
            // System.out.println("cqlsh:\n" + new String(bytes, 0, cnt1));

            Thread.sleep(1000);
            socket = new Socket("localhost", port);
            // TODO: Add a catch, when the connection is rejected regenerate it.
        } else {
            throw new IllegalStateException("Cannot fina an available port");
        }
    }

    public CqlshPacket execute(String cmd) throws IOException {
        BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(socket.getOutputStream()));

        bw.write(cmd);
        bw.flush();
        // System.out.println("executor write " + cmd);
        BufferedReader br = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));

        // // Socket
        // System.out.println("Socket Debug");
        // byte[] output = new byte[10240];
        // cqlsh.getInputStream().read(output);
        // System.out.println(new String(output));

        char[] chars = new char[10240];

        int cnt = br.read(chars);
        String cqlshMess = new String(chars, 0, cnt);

        // System.out.println("receive size: " + cqlshMess.length() + " \n" +
        // cqlshMess);

        CqlshPacket cqlshPacket = new Gson().fromJson(cqlshMess,
                CqlshPacket.class);
        return cqlshPacket;
    }

    public static boolean testPortAvailable(int port) {
        Process p;
        try {
            p = Utilities.exec(new String[] { "bin/sh", "-c",
                    "netstat -tunlp | grep -P \":" + port + "[\\s$]\"" },
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

    /**
     * Destroy the current process.
     */
    public void destroy() {
        cqlsh.destroy();
    }
}
