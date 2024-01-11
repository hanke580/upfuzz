package org.zlab.upfuzz.cassandra;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.sql.Time;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.docker.Docker;
import org.zlab.upfuzz.utils.Utilities;
import org.zlab.upfuzz.fuzzingengine.Config;

public class CassandraCqlshDaemon {
    static Logger logger = LogManager.getLogger(CassandraCqlshDaemon.class);
    private Socket socket;
    public static String cqlshPython2Script;
    public static String cqlshPython3Script;

    public static final int CASSANDRA_RETRY_TIMEOUT = 720; // seconds

    // Check the process num after WAIT_INTERVAL time to
    // reduce the FP since the process might not start yet
    public static final int WAIT_INTERVAL = 15;

    public static List<String> noiseErrors = new LinkedList<>();
    static {
        noiseErrors.add("Bootstrap Token collision");
    }

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

    public CassandraCqlshDaemon(String ipAddress, int port, Docker docker) {
        int SLEEP_INTERVAL = 1;
        int retry = CASSANDRA_RETRY_TIMEOUT / SLEEP_INTERVAL;
        logger.info("[HKLOG] executor ID = " + docker.executorID + "  "
                + "Connect to cqlsh:" + ipAddress + "..."
                + "\t this normally takes"
                + " 6 seconds for single node or 50s for 3-node cluster node");
        Long totalReadTimeFromProcess = 0L;
        Long totalProcExecTime = 0L;
        for (int i = 0; i < retry; ++i) {
            try {
                if (i % 5 == 0) {
                    logger.debug("[HKLOG] executor ID = " + docker.executorID
                            + "  "
                            + "Connect to cqlsh:" + ipAddress + "..." + i);
                    socket = new Socket();
                    if (Config.getConf().debug) {
                        logger.info(
                                "[CassandraCqlshDaemon] Created a new socket");
                    }
                    socket.connect(new InetSocketAddress(ipAddress, port),
                            3 * 1000);
                    logger.info(
                            "[HKLOG] executor ID = " + docker.executorID + "  "
                                    + "Cqlsh connected: " + ipAddress);
                    if (Config.getConf().debug) {
                        logger.info(
                                "[CassandraCqlshDaemon] Needed total proc exec time "
                                        + totalProcExecTime + " ms"
                                        + " and total read time "
                                        + totalReadTimeFromProcess + " ms");
                    }
                    totalReadTimeFromProcess = 0L;
                    totalProcExecTime = 0L;
                    return;
                }
            } catch (Exception ignored) {
            }
            try {
                Thread.sleep(SLEEP_INTERVAL * 1000);
            } catch (InterruptedException ignored) {
            }

            // After WAIT_INTERVAL, the process should have started
            if (i * SLEEP_INTERVAL >= WAIT_INTERVAL) {
                try {
                    if (Config.getConf().debug) {
                        logger.info(
                                "[CassandraCqlshDaemon] the process should start now");
                    }
                    Long curTime = System.currentTimeMillis();
                    Process grepProc = docker.runInContainer(new String[] {
                            "/bin/sh", "-c",
                            "ps -ef | grep org.apache.cassandra.service.CassandraDaemon | wc -l"
                    });
                    totalProcExecTime += System.currentTimeMillis() - curTime;
                    if (Config.getConf().debug) {
                        logger.info(
                                String.format(
                                        "[CassandraCqlshDaemon] have searched the daemon process in container for %d ms",
                                        System.currentTimeMillis() - curTime));
                    }
                    curTime = System.currentTimeMillis();
                    String result = new String(
                            grepProc.getInputStream().readAllBytes()).strip();
                    totalReadTimeFromProcess += System.currentTimeMillis()
                            - curTime;
                    if (Config.getConf().debug) {
                        logger.info(
                                String.format(
                                        "[CassandraCqlshDaemon] have read the bytes in %d ms",
                                        System.currentTimeMillis() - curTime));
                    }
                    // Process grepProc2 = docker.runInContainer(new String[] {
                    // "/bin/sh", "-c",
                    // "cat /var/log/supervisor/cassandra-stderr*"
                    // });
                    // String result2 = new String(
                    // grepProc2.getInputStream().readAllBytes()).strip();
                    // System.err.println("grep check result2 = " + result2);
                    // logger.debug("Timeout check: "
                    // + Config.getConf().cassandraEnableTimeoutCheck);
                    if (Config.getConf().cassandraEnableTimeoutCheck) {
                        int processNum = Integer.parseInt(result);
                        if (Integer.parseInt(result) <= 2) {
                            // Process has died
                            logger.debug("result = " + result);
                            logger.debug("[HKLOG] processNum = " + processNum
                                    + " smaller than 2, "
                                    + "system process died");
                            break;
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }
        throw new RuntimeException("[HKLOG] executor ID = " + docker.executorID
                + "  " + "cannot connect to cqlsh at " + ipAddress);
    }

    public CqlshPacket execute(String cmd) throws Exception {
        if (Config.getConf().debug) {
            logger.info("[CqlshPacket] Call to execute ");
        }
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

        // FIXME: Also need to modify cqlsh_daemon
        char[] chars = new char[51200];

        int cnt = br.read(chars);
        if (cnt == -1) {
            throw new IllegalStateException("cqlsh daemon crashed");
        }
        String cqlshMessage = new String(chars, 0, cnt);

        // logger.info("[HKLOG] length of cqlshMessage: " +
        // cqlshMessage.length());

        Gson gson = new GsonBuilder()
                .setLenient()
                .create();

        // logger.info("cqlshMessage: " + cqlshMessage);
        CqlshPacket cqlshPacket = null;
        cqlshPacket = gson.fromJson(cqlshMessage,
                CqlshPacket.class);

        // logger.info("before decode: " + cqlshPacket.message);
        cqlshPacket.message = Utilities.decodeString(cqlshPacket.message)
                .replace("\0", "");
        if (Config.getConf().debug) {
            logger.info("[CqlshDaemon] cqlsh message after decode: "
                    + cqlshPacket.message);
        }

        // logger.info("value size = " + cqlshPacket.message.length());

        // logger.info(
        // "CqlshMessage:\n" +
        // new GsonBuilder().setPrettyPrinting().create()
        // .toJson(cqlshPacket));
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
    }
}
