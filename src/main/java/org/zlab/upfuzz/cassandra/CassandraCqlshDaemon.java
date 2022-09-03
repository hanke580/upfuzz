package org.zlab.upfuzz.cassandra;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Paths;
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
    private String cassandraVersion;
    private String cqlshPythonScript;
    private String python;

    public static String cqlshPython2Script;

    public static String cqlshPython3Script;

    static {
        InputStream cqlsh_daemon2 = CassandraCqlshDaemon.class.getClassLoader()
                .getResourceAsStream(
                        "cqlsh_daemon2.py");
        InputStream cqlsh_daemon3 = CassandraCqlshDaemon.class.getClassLoader()
                .getResourceAsStream(
                        "cqlsh_daemon3.py");
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

    public CassandraCqlshDaemon(String ipAddress, int port) {
        int retry = 30;
        logger.info("Connect to cqlsh:" + ipAddress + "...");
        for (int i = 0; i < retry; ++i) {
            try {
                logger.debug("Connect to cqlsh:" + ipAddress + "..." + i);
                socket = new Socket();
                socket.connect(new InetSocketAddress(ipAddress, port),
                        3 * 1000);
                logger.info("Cqlsh connected: " + ipAddress);
                return;
            } catch (IOException e) {
            }
            try {
                Thread.sleep(10 * 1000);
            } catch (InterruptedException e) {
            }
        }
        throw new IllegalAccessError("cannot connect to cqlsh at " + ipAddress);
    }

    public CassandraCqlshDaemon(String ipAddress, int port, String executorID) {
        int retry = 30;
        logger.info("[HKLOG] executor ID = " + executorID + "  "
                + "Connect to cqlsh:" + ipAddress + "...");
        for (int i = 0; i < retry; ++i) {
            try {
                logger.debug("[HKLOG] executor ID = " + executorID + "  "
                        + "Connect to cqlsh:" + ipAddress + "..." + i);
                socket = new Socket();
                socket.connect(new InetSocketAddress(ipAddress, port),
                        3 * 1000);
                logger.info("[HKLOG] executor ID = " + executorID + "  "
                        + "Cqlsh connected: " + ipAddress);
                return;
            } catch (IOException e) {
            }
            try {
                Thread.sleep(10 * 1000);
            } catch (InterruptedException e) {
            }
        }
        throw new IllegalAccessError("[HKLOG] executor ID = " + executorID
                + "  " + "cannot connect to cqlsh at " + ipAddress);
    }

    public CassandraCqlshDaemon(String ipAddress) {
        port = 18251;
        int retry = 30;
        for (int i = 0; i < retry; ++i) {
            try {
                logger.info("Connect to cqlsh:" + ipAddress + "...");
                socket = new Socket();
                socket.connect(new InetSocketAddress(ipAddress, port),
                        3 * 1000);
                logger.info("Cqlsh connected: " + ipAddress);
                return;
            } catch (IOException e) {
            }
            try {
                Thread.sleep(3 * 1000);
            } catch (InterruptedException e) {
            }
        }
        throw new IllegalAccessError("cannot connect to cqlsh at " + ipAddress);
    }

    // public CassandraCqlshDaemon(String cassandraRoot)
    // throws IOException, InterruptedException {
    // cassandraVersion = Utilities.getGitTag(cassandraRoot);
    // cqlshPythonScript = null;
    // python = null;
    // // Get path, two options
    // // System.out.println("cassandra version\n" + cassandraVersion);

    // char majorVersion;

    // // FIXME use majorversion to determine python version
    // try {
    // majorVersion = cassandraVersion.split("-")[1].charAt(0);
    // } catch (Exception e) {
    // majorVersion = cassandraRoot.split("cassandra-")[1].charAt(0);
    // }
    // logger.debug("cassandra version: " + majorVersion);

    // if (majorVersion <= '3') {
    // logger.info("use python2 cqlsh script");
    // python = "python2";
    // cqlshPythonScript = cqlshPython2Script;
    // } else {
    // logger.info("use python3 cqlsh script");
    // python = "python3";
    // cqlshPythonScript = cqlshPython3Script;
    // }

    // startCqlshDaemon(cassandraRoot);
    // }

    private void startCqlshDaemon(String cassandraRoot)
            throws IOException, InterruptedException {
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
            bw.write(cqlshPythonScript.replace("%__reserved_port__",
                    Integer.toString(port)));
            bw.close();

            File logFile = Paths.get(cassandraRoot, "cqlsh_daemon.log")
                    .toFile();
            ProcessBuilder cqlshBuilder = new ProcessBuilder(python,
                    "bin/cqlsh_daemon.py");
            cqlshBuilder.directory(new File(cassandraRoot));
            cqlshBuilder.redirectErrorStream(true);
            cqlshBuilder.redirectOutput(logFile);

            // FIXME python script sometime crash but not terminated
            cqlsh = cqlshBuilder.start();

            Thread.sleep(500);
            if (cqlsh.isAlive()) {
                try {
                    socket = new Socket("localhost", port);
                    return;
                } catch (IOException e) {
                    logger.error(e);
                }
            } else {
                logger.error("failed to start cqlsh daemon, check " +
                        Paths.get(cassandraRoot, "cqlsh_daemon.log") +
                        " for information");
            }
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
        if (cnt == -1) {
            throw new IllegalStateException("cqlsh daemon crashed");
        }
        String cqlshMessage = new String(chars, 0, cnt);

        // System.out.println("receive size: " + cqlshMess.length() + " \n" +
        // cqlshMess);

        CqlshPacket cqlshPacket = new Gson().fromJson(cqlshMessage,
                CqlshPacket.class);

        logger.trace(
                "CqlshMessage:\n" +
                        new GsonBuilder().setPrettyPrinting().create()
                                .toJson(cqlshPacket));
        return cqlshPacket;
    }

    public static boolean testPortAvailable(int port) {
        Process p;
        try {
            p = Utilities.exec(new String[] { "bin/sh", "-c",
                    "netstat -tunlp | grep -P \":" +
                            port + "[\\s$]\"" },
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
        public String error;
        public double timeUsage;

        public CqlshPacket() {
            cmd = "";
            exitValue = 0;
            message = "";
            error = "";
            timeUsage = -1;
        }
    }

    /**
     * Destroy the current process.
     */
    public void destroy() {
        // Do need to destroy
        // docker-compose are in charge
        //
        // cqlsh.destroy();
    }
}
