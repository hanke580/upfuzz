package org.zlab.upfuzz.fuzzingengine.Packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

    public RegisterPacket() {
    }

    public RegisterPacket(Socket socket) {
        this.systemId = Config.getConf().system;
        this.type = PacketType.RegisterPacket;
        this.clientId = socket.getLocalAddress().getHostName()
                + socket.getLocalSocketAddress().toString();
    }

    public static RegisterPacket read(DataInputStream in) {
        byte[] bytes = new byte[65536];
        int len;
        try {
            len = in.read(bytes);
            RegisterPacket registerPacket = new Gson()
                    .fromJson(new String(bytes, 0, len), RegisterPacket.class);
            return registerPacket;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void write(DataOutputStream out) {
        try {
            out.writeInt(type.value);
            String jsonStr = new Gson().toJson(this);
            logger.debug("write register packet:" + jsonStr);
            out.write(jsonStr.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
