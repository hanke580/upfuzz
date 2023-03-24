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
        int retry = 50;
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
        DataInputStream in = new DataInputStream(socket.getInputStream());
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        out.writeInt(cmd.getBytes().length);
        out.write(cmd.getBytes());

        int packetLength = in.readInt();
        logger.info("ret len = " + packetLength);
        byte[] bytes = new byte[packetLength];
        int len = 0;
        len = in.read(bytes, len, packetLength - len);
        while (len < packetLength) {
            int size = in.read(bytes, len, packetLength - len);
            len += size;
        }
        String hbaseMessage = new String(bytes);

        Gson gson = new GsonBuilder()
                .setLenient()
                .create();

        // logger.info("hbase Message: " + hbaseMessage);
        HBasePacket hbasePacket = null;
        try {
            hbasePacket = gson.fromJson(hbaseMessage,
                    HBasePacket.class);
            hbasePacket.message = Utilities.decodeString(hbasePacket.message)
                    .replace("\0", "");
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
        public String message;
        public String error;
        public double timeUsage;

        public HBasePacket() {
            cmd = "";
            exitValue = 0;
            message = "";
            error = "";
            timeUsage = -1;
        }
    }
}
