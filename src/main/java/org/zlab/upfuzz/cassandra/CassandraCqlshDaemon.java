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
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.docker.Docker;
import org.zlab.upfuzz.fuzzingengine.LogInfo;
import org.zlab.upfuzz.fuzzingengine.executor.Executor;
import org.zlab.upfuzz.utils.Utilities;

public class CassandraCqlshDaemon {
    static Logger logger = LogManager.getLogger(CassandraCqlshDaemon.class);
    private Socket socket;
    public static String cqlshPython2Script;
    public static String cqlshPython3Script;

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
        int retry = 20;
        logger.info("[HKLOG] executor ID = " + docker.executorID + "  "
                + "Connect to cqlsh:" + ipAddress + "...");
        for (int i = 0; i < retry; ++i) {
            try {
                logger.debug("[HKLOG] executor ID = " + docker.executorID + "  "
                        + "Connect to cqlsh:" + ipAddress + "..." + i);
                socket = new Socket();
                socket.connect(new InetSocketAddress(ipAddress, port),
                        3 * 1000);
                logger.info("[HKLOG] executor ID = " + docker.executorID + "  "
                        + "Cqlsh connected: " + ipAddress);
                return;
            } catch (Exception ignored) {
            }
            try {
                Thread.sleep(10 * 1000);
            } catch (InterruptedException ignored) {
            }

            try {
                Process grepProc = docker.runInContainer(new String[] {
                        "/bin/sh", "-c",
                        "ps -ef | grep org.apache.cassandra.service.CassandraDaemon | wc -l"
                });
                String result = new String(
                        grepProc.getInputStream().readAllBytes());
                int processNum = Integer.parseInt(result);
                logger.info("[HKLOG] processNum = " + processNum);
                if (Integer.parseInt(result) == 1) {
                    // Process has died
                    break;
                }

            } catch (Exception ignore) {
            }

            // read log to check whether it ends
//            LogInfo logInfo = docker.readLogInfo();
//            if (logInfo.getErrorMsg().size() > 0) {
//                for (String msg : logInfo.getErrorMsg()) {
//                    boolean isNoise = false;
//                    for (String noiseError : noiseErrors) {
//                        if (msg.contains(noiseError)) {
//                            isNoise = true;
//                            break;
//                        }
//                    }
//                    if (!isNoise) {
//                        break;
//                    }
//                    System.out.println(msg);
//                }
//                break;
//            }
        }
        throw new RuntimeException("[HKLOG] executor ID = " + docker.executorID
                + "  " + "cannot connect to cqlsh at " + ipAddress);
    }

    public CqlshPacket execute(String cmd) throws Exception {
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
        // logger.info("after decode: " + cqlshPacket.message);

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
