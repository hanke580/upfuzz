package org.zlab.upfuzz.hdfs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.cassandra.CassandraCqlshDaemon;
import org.zlab.upfuzz.docker.Docker;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;

public class HDFSShellDaemon {
    static Logger logger = LogManager.getLogger(CassandraCqlshDaemon.class);

    private Socket socket;

    public HDFSShellDaemon(String ipAddress, int port, String executorID,
            Docker docker) {
        int retry = 10;
        logger.info("[HKLOG] executor ID = " + executorID + "  "
                + "Connect to hdfs shell daemon:" + ipAddress + "...");
        for (int i = 0; i < retry; ++i) {
            try {
                logger.debug("[HKLOG] executor ID = " + executorID + "  "
                        + "Connect to hdfs shell:" + ipAddress + "..." + i);
                socket = new Socket();
                socket.connect(new InetSocketAddress(ipAddress, port),
                        3 * 1000);
                logger.info("[HKLOG] executor ID = " + executorID + "  "
                        + "hdfs shell connected: " + ipAddress);
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
                        "ps -ef | grep org.apache.hadoop.hdfs.server | wc -l"
                });
                String result = new String(
                        grepProc.getInputStream().readAllBytes()).strip();
                int processNum = Integer.parseInt(result);
                logger.info("[HKLOG] processNum = " + processNum);
                if (Integer.parseInt(result) <= 2) {
                    // Process has died
                    break;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        throw new RuntimeException("[HKLOG] executor ID = " + executorID
                + "  " + "cannot connect to hdfs shell at " + ipAddress);
    }

    public CassandraCqlshDaemon.CqlshPacket execute(String cmd)
            throws IOException {
        BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(socket.getOutputStream()));

        bw.write(cmd);
        bw.flush();
        // logger.info(String.format("Command: %s", cmd));
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
            throw new IllegalStateException("hdfs daemon crashed");
        }
        String cqlshMessage = new String(chars, 0, cnt);

        // logger.info("[HKLOG] length of cqlshMessage: " +
        // cqlshMessage.length());

        Gson gson = new GsonBuilder()
                .setLenient()
                .create();

        // logger.info("hdfs Message: " + cqlshMessage);
        CassandraCqlshDaemon.CqlshPacket cqlshPacket = null;
        try {
            cqlshPacket = gson.fromJson(cqlshMessage,
                    CassandraCqlshDaemon.CqlshPacket.class);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("ERROR: Cannot read from json\n WRONG_CQLSH MESSAGE: "
                    + cqlshMessage);
        }

        // logger.info(
        // "CqlshMessage:\n" +
        // new GsonBuilder().setPrettyPrinting().create()
        // .toJson(cqlshPacket));
        return cqlshPacket;
    }

}
