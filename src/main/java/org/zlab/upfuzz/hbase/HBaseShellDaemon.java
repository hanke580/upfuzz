package org.zlab.upfuzz.hbase;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.docker.Docker;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.utils.Utilities;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;

public class HBaseShellDaemon {
    static Logger logger = LogManager.getLogger(HBaseShellDaemon.class);

    private Socket socket;

    public HBaseShellDaemon(String ipAddress, int port, String executorID,
            Docker docker) {
        int retry = 20;
        logger.info("[HKLOG] executor ID = " + executorID + "  "
                + "Connect to hbase shell daemon:" + ipAddress + "...");
        for (int i = 0; i < retry; ++i) {
            try {
                logger.debug("[HKLOG] executor ID = " + executorID + "  "
                        + "Connect to hbase shell:" + ipAddress + "..." + i);
                socket = new Socket();
                socket.connect(new InetSocketAddress(ipAddress, port),
                        3 * 1000);
                logger.info("[HKLOG] executor ID = " + executorID + "  "
                        + "hbase shell connected: " + ipAddress);
                return;
            } catch (Exception ignored) {
            }
            try {
                Thread.sleep(10 * 1000);
            } catch (InterruptedException ignored) {
            }

        }
        throw new RuntimeException("[HKLOG] executor ID = " + executorID
                + "  " + "cannot connect to hbase shell at " + ipAddress);
    }

    public HBasePacket execute(String cmd)
            throws IOException {
        BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(socket.getOutputStream()));

        bw.write(cmd);
        bw.flush();
        // System.out.println("executor write " + cmd);
        BufferedReader br = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));

        char[] chars = new char[51200];
        int cnt = br.read(chars);
        if (cnt == -1) {
            throw new IllegalStateException("cqlsh daemon crashed");
        }
        // logger.debug("ret len = " + cnt);
        String hbaseMessage = new String(chars, 0, cnt);

        Gson gson = new GsonBuilder()
                .setLenient()
                .create();

        // logger.info("hbase Message: " + hbaseMessage);
        HBasePacket hbasePacket = null;
        try {
            hbasePacket = gson.fromJson(hbaseMessage,
                    HBasePacket.class);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("ERROR: Cannot read from json\n WRONG_HBase MESSAGE: "
                    + hbaseMessage);
        }
        if (Config.getConf().debug) {
            logger.debug(
                    "HBaseMessage:\n" +
                            new GsonBuilder().setPrettyPrinting().create()
                                    .toJson(hbasePacket));
        }
        return hbasePacket;
    }

    public static class HBasePacket {
        public String cmd;
        public int exitValue;
        public double timeUsage;
        public String message;
        public String error;

        public HBasePacket() {
            cmd = "";
            exitValue = 0;
            timeUsage = -1;
            message = "";
            error = "";
        }
    }
}
