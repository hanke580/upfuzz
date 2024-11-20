package org.zlab.upfuzz.ozone;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.docker.Docker;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.utils.Utilities;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class OzoneShellDaemon {

    static Logger logger = LogManager.getLogger(OzoneShellDaemon.class);

    private Socket socket;

    public OzoneShellDaemon(String ipAddress, int port, String executorID,
            Docker docker) {
        int retry = 12;
        logger.info("[HKLOG] executor ID = " + executorID + "  "
                + "Connect to ozone shell daemon:" + ipAddress + "...");
        for (int i = 0; i < retry; ++i) {
            try {
                logger.debug("[HKLOG] executor ID = " + executorID + "  "
                        + "Connect to ozone shell:" + ipAddress + "..." + i);
                socket = new Socket();
                socket.connect(new InetSocketAddress(ipAddress, port),
                        3 * 1000);
                logger.info("[HKLOG] executor ID = " + executorID + "  "
                        + "ozone shell connected: " + ipAddress);
                return;
            } catch (Exception ignored) {
            }
            try {
                Thread.sleep(10 * 1000);
            } catch (InterruptedException ignored) {
            }
        }
        throw new RuntimeException("[HKLOG] executor ID = " + executorID
                + "  " + "cannot connect to ozone shell at " + ipAddress);
    }

    public OzonePacket execute(String cmd)
            throws IOException {
        DataInputStream in = new DataInputStream(socket.getInputStream());
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        out.writeInt(cmd.getBytes().length);
        out.write(cmd.getBytes());

        int packetLength = in.readInt();
        if (Config.getConf().debug) {
            logger.info("ozone daemon ret len = " + packetLength);
        }
        byte[] bytes = new byte[packetLength];
        int len = 0;
        len = in.read(bytes, len, packetLength - len);
        while (len < packetLength) {
            int size = in.read(bytes, len, packetLength - len);
            len += size;
        }
        String ozoneMessage = new String(bytes);

        Gson gson = new GsonBuilder()
                .setLenient()
                .create();

        // logger.info("ozone Message: " + hdfsMessage);
        OzonePacket ozonePacket = null;
        try {
            ozonePacket = gson.fromJson(ozoneMessage,
                    OzonePacket.class);
            ozonePacket.message = Utilities.decodeString(ozonePacket.message)
                    .replace("\0", "");
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("ERROR: Cannot read from json\n WRONG_OZONE MESSAGE: "
                    + ozoneMessage);
        }
        if (Config.getConf().debug) {
            logger.debug(
                    "OzoneMessage:\n" +
                            new GsonBuilder().setPrettyPrinting().create()
                                    .toJson(ozonePacket));
        }
        return ozonePacket;
    }

    public static class OzonePacket {
        public String cmd;
        public int exitValue;
        public String message;
        public String error;
        public double timeUsage;

        public OzonePacket() {
            cmd = "";
            exitValue = 0;
            message = "";
            error = "";
            timeUsage = -1;
        }
    }
}
