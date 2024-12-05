package org.zlab.upfuzz.hbase;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.docker.Docker;
import org.zlab.upfuzz.fuzzingengine.ClusterStuckException;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.utils.Utilities;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class HBaseShellDaemon {
    static Logger logger = LogManager.getLogger(HBaseShellDaemon.class);

    private Socket socket;

    public HBaseShellDaemon(String ipAddress, int port, String executorID,
            Docker docker) {
        logger.info("[HKLOG] executor ID = " + executorID + "  "
                + "Connect to hbase shell daemon:" + ipAddress + "...");
        for (int i = 0; i < Config.getConf().hbaseDaemonRetryTimes; ++i) {
            try {
                if (i % 5 == 0) {
                    logger.debug("[HKLOG] executor ID = " + executorID + "  "
                            + "Connect to hbase shell:" + ipAddress + "..."
                            + i);
                }
                socket = new Socket();
                socket.connect(new InetSocketAddress(ipAddress, port),
                        3 * 1000);
                logger.info("[HKLOG] executor ID = " + executorID + "  "
                        + "hbase shell connected: " + ipAddress);
                return;
            } catch (Exception ignored) {
            }
            try {
                Thread.sleep(5 * 1000);
            } catch (InterruptedException ignored) {
            }
        }
        throw new RuntimeException("[HKLOG] executor ID = " + executorID
                + "  " + "cannot connect to hbase shell at " + ipAddress);
    }

    public HBasePacket execute(String cmd)
            throws IOException, ClusterStuckException {
        // Set the socket read timeout to 2 minutes (120,000 milliseconds)
        socket.setSoTimeout(240_000); // 4 minutes in milliseconds

        try {
            Utilities.serializeSingleCommand(cmd, socket.getOutputStream());

            String hbaseMessage = Utilities.deserializeSingleCommandResult(
                    new DataInputStream(socket.getInputStream()));

            Gson gson = new GsonBuilder()
                    .setLenient()
                    .create();

            HBasePacket hbasePacket = null;
            try {
                hbasePacket = gson.fromJson(hbaseMessage, HBasePacket.class);
            } catch (Exception e) {
                e.printStackTrace();
                logger.error(
                        "ERROR: Cannot read from JSON. WRONG_HBase MESSAGE: "
                                + hbaseMessage);
            }

            if (Config.getConf().debug) {
                String prettyJson = new GsonBuilder().setPrettyPrinting()
                        .create().toJson(hbasePacket);
                logger.debug("HBaseMessage:\n" + prettyJson);
            }

            return hbasePacket;

        } catch (SocketTimeoutException e) {
            throw new ClusterStuckException(
                    "Command execution timed out.", e);
        }
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
