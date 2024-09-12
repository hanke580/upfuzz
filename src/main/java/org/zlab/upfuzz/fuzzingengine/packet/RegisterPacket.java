package org.zlab.upfuzz.fuzzingengine.packet;

import com.google.gson.Gson;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.fuzzingengine.Config;

public class RegisterPacket extends Packet {
    static Logger logger = LogManager.getLogger(RegisterPacket.class);

    public String systemId;
    public String clientId;
    public int group;

    public RegisterPacket() {
    }

    public RegisterPacket(Socket socket, int group) {
        this.systemId = Config.getConf().system;
        this.type = PacketType.RegisterPacket;
        this.clientId = socket.getLocalAddress().getHostName() +
                socket.getLocalSocketAddress().toString();
        this.group = group;
    }

    public static RegisterPacket read(DataInputStream in) {
        return (RegisterPacket) read(in, RegisterPacket.class);
    }
}
