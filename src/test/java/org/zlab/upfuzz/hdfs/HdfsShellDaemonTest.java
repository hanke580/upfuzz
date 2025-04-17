package org.zlab.upfuzz.hdfs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import junit.framework.TestCase;
import org.junit.jupiter.api.Test;
import org.zlab.upfuzz.hdfs.HDFSShellDaemon.HdfsPacket;

public class HdfsShellDaemonTest extends TestCase {
    protected void setUp() {
    }

    @Test
    public void testFromJson() {
        String jsonString = "{\"cmd\":\"CREATE KEYSPACE IF NOT EXISTS OXBJMXLSGDFXBKX WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 2 };\",\"exitValue\":0,\"timeUsage\":9.5367431640625e-07,\"message\":\"Test\"}";
        HdfsPacket hdfsPacket = new Gson().fromJson(jsonString,
                HdfsPacket.class);
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();
        System.out.println(gson.toJson(hdfsPacket, HdfsPacket.class));
    }

    // @Test
    public void testDaemon() {
        String[] cmds = new String[] { "-ls /", "-touchz /user_z", "-ls /",
                "-rm /user_z", "-ls /", "-rm /user_z" };

        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();
        Socket socket;
        try {
            socket = new Socket("127.0.0.1", 11116);

            BufferedReader br = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            DataOutputStream out = new DataOutputStream(
                    socket.getOutputStream());
            for (String cmd : cmds) {
                out.writeInt(cmd.getBytes().length);
                out.write(cmd.getBytes());

                BufferedWriter bw = new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream()));

                char[] chars = new char[65536];
                int len = br.read(chars);
                String hdfsMessage = new String(chars, 0, len);
                System.out.println("message: " + hdfsMessage);
                HdfsPacket hdfsPacket = gson.fromJson(hdfsMessage,
                        HdfsPacket.class);
            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
