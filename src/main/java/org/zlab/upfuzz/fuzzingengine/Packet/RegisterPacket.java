package org.zlab.upfuzz.fuzzingengine.Packet;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import com.google.gson.Gson;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.fuzzingengine.Config;

public class RegisterPacket extends Packet {
    static Logger logger = LogManager.getLogger(RegisterPacket.class);

    public String systemId;
    public String clientId;

    public RegisterPacket(Socket socket) {
        this.systemId = Config.getConf().system;
        this.type = PacketType.RegisterPacket;
        this.clientId = socket.getLocalAddress().getHostName()
                + socket.getLocalSocketAddress().toString();
    }

    public void write(OutputStream out) {
        try {
            out.write(type.value);
            String jsonStr = new Gson().toJson(this);
            logger.debug("write register packet:" + jsonStr);
            out.write(jsonStr.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
